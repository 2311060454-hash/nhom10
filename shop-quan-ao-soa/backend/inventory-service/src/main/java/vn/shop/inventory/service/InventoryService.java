package vn.shop.inventory.service;
import vn.shop.inventory.dto.InventoryDtos.*;
import vn.shop.inventory.entity.*;
import vn.shop.inventory.repository.*;
import vn.shop.common.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class InventoryService {
    private final StockRepository stocks;private final GuardRepository guard;private final TransactionRepository transactions;private final ReservationRepository reservations;private final PartialReturnRestockRepository partialReturns;
    public InventoryService(StockRepository s,GuardRepository g,TransactionRepository t,ReservationRepository r,PartialReturnRestockRepository p){stocks=s;guard=g;transactions=t;reservations=r;partialReturns=p;}
    private void lock(){if(guard.lock()==null)throw new ApiException(503,"Kho chưa được khởi tạo");}
    public static StockView view(Stock s){return new StockView(s.variantId,s.onHand,s.reserved,s.onHand-s.reserved,s.minimumStock);}
    @Transactional(readOnly=true) public PageResult<StockView> list(Long variantId,boolean low,int page,int size){var p=stocks.search(variantId,low,PageRequest.of(page,size,Sort.by("variantId")));return new PageResult<>(p.map(InventoryService::view).getContent(),p.getTotalElements(),p.getTotalPages(),page,size,p.isLast());}
    @Transactional(readOnly=true) public byte[] exportCsv(Long variantId,boolean low){
        var page=stocks.search(variantId,low,PageRequest.of(0,10001,Sort.by("variantId")));
        if(page.getTotalElements()>10000)throw new ApiException(413,"Báo cáo kho vượt 10.000 biến thể; hãy lọc theo ID biến thể");
        StringBuilder csv=new StringBuilder("\uFEFFMã biến thể,Tồn thực tế,Đang giữ,Khả dụng,Mức tối thiểu,Sắp hết hàng\r\n");
        for(Stock s:page.getContent())csv.append(s.variantId).append(',').append(s.onHand).append(',').append(s.reserved).append(',').append(s.onHand-s.reserved).append(',').append(s.minimumStock).append(',').append(s.onHand-s.reserved<=s.minimumStock?"Có":"Không").append("\r\n");
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    @Transactional(readOnly=true) public Availability available(long id){return new Availability(id,stocks.findById(id).map(s -> s.onHand-s.reserved).orElse(0));}
    @Transactional(readOnly=true) public PageResult<TransactionView> history(Long variantId,int page){var p=transactions.history(variantId,PageRequest.of(page,30,Sort.by("id").descending()));return new PageResult<>(p.map(t -> new TransactionView(t.id,t.variantId,t.operationKey,t.type,t.quantityDelta,t.reservedDelta,t.reason,t.actorId,t.createdAt)).getContent(),p.getTotalElements(),p.getTotalPages(),page,30,p.isLast());}
    @Transactional public StockView adjust(String key,Adjustment input,long actor){
        lock();String op="manual:"+key;String hash=hash(actor+"|"+input.variantId()+"|"+input.type()+"|"+input.quantity()+"|"+input.reason());
        var existing=transactions.findByOperationKey(op);if(existing.isPresent()){var t=existing.get();if(!t.requestHash.equals(hash))throw new ApiException(409,"Idempotency-Key đã dùng cho nội dung khác");return new StockView(t.variantId,t.resultOnHand,t.resultReserved,t.resultOnHand-t.resultReserved,t.resultMinimum);}
        Stock s=stocks.findById(input.variantId()).orElseGet(() -> {Stock n=new Stock();n.variantId=input.variantId();return n;});
        int previous=s.onHand;
        switch(input.type()){
            case "RECEIPT" -> {if(input.quantity()<1)throw new ApiException(400,"Số nhập phải lớn hơn 0");s.onHand=Math.addExact(s.onHand,input.quantity());if(s.onHand>1000000000)throw new ApiException(400,"Tồn kho vượt giới hạn");}
            case "ISSUE" -> {if(input.quantity()<1||input.quantity()>s.onHand-s.reserved)throw new ApiException(409,"Số xuất vượt tồn khả dụng hoặc không hợp lệ");s.onHand-=input.quantity();}
            case "COUNT" -> {if(input.quantity()<s.reserved)throw new ApiException(409,"Số kiểm kê không được nhỏ hơn số đang giữ");s.onHand=input.quantity();}
            case "MINIMUM" -> s.minimumStock=input.quantity();
            default -> throw new ApiException(400,"Loại điều chỉnh không hợp lệ");
        }
        stocks.save(s);log(s,op,hash,input.type(),s.onHand-previous,0,input.reason(),actor);return view(s);
    }
    @Transactional public ReservationView reserve(String orderId,Reserve input){
        lock();var sorted=input.items().stream().sorted(Comparator.comparing(Line::variantId)).toList();
        if(sorted.stream().map(Line::variantId).distinct().count()!=sorted.size())throw new ApiException(400,"Trùng biến thể trong yêu cầu giữ kho");
        String payload=sorted.stream().map(l -> l.variantId()+":"+l.quantity()).collect(java.util.stream.Collectors.joining(","));String hash=hash(payload);
        var found=reservations.findById(orderId);if(found.isPresent()){if(!found.get().requestHash.equals(hash))throw new ApiException(409,"Mã đơn đã được dùng cho giỏ khác");return reservationView(found.get());}
        Reservation r=new Reservation();r.orderId=orderId;r.requestHash=hash;r.state="HELD";r.expiresAt=Instant.now().plusSeconds(900);
        for(Line l:sorted){Stock s=stocks.findById(l.variantId()).orElseThrow(() -> new ApiException(409,"Biến thể chưa có tồn kho"));if(s.onHand-s.reserved<l.quantity())throw new ApiException(409,"Không đủ hàng khả dụng");s.reserved+=l.quantity();stocks.save(s);ReservationItem item=new ReservationItem();item.reservation=r;item.variantId=l.variantId();item.quantity=l.quantity();r.items.add(item);log(s,"reserve:"+orderId+":"+s.variantId,hash,"RESERVE",0,l.quantity(),"Giữ kho cho đơn "+orderId,0L);}
        reservations.save(r);return reservationView(r);
    }
    @Transactional public ReservationView finish(String orderId,String action){
        lock();Reservation r=reservations.findById(orderId).orElseThrow(ApiException::missing);
        if(action.equals("commit")){
            if(r.state.equals("COMMITTED"))return reservationView(r);
            if(!r.state.equals("HELD"))throw new ApiException(409,"Không thể xác nhận lượt giữ đã giải phóng");
            if(!r.expiresAt.isAfter(Instant.now()))throw new ApiException(409,"Lượt giữ đã hết hạn, không thể xác nhận");
            move(r,true,false);r.state="COMMITTED";
        }else if(action.equals("release")){
            if(r.state.equals("RELEASED")||r.state.equals("EXPIRED"))return reservationView(r);
            if(!r.state.equals("HELD"))throw new ApiException(409,"Hàng đã xuất, phải hoàn kho theo quy trình đơn");
            move(r,false,false);r.state="RELEASED";
        }else if(action.equals("restock")){
            if(r.state.equals("RESTOCKED"))return reservationView(r);
            if(!r.state.equals("COMMITTED"))throw new ApiException(409,"Chỉ hoàn kho cho lượt đã xuất");
            move(r,false,true);r.state="RESTOCKED";
        }else throw new ApiException(400,"Thao tác không hợp lệ");
        reservations.save(r);return reservationView(r);
    }
    @Transactional public ReservationView partialRestock(String orderId,String returnId,Reserve input){
        lock();UUID.fromString(returnId);Reservation r=reservations.findById(orderId).orElseThrow(ApiException::missing);
        var sorted=input.items().stream().sorted(Comparator.comparing(Line::variantId)).toList();
        if(sorted.isEmpty()||sorted.stream().map(Line::variantId).distinct().count()!=sorted.size())throw new ApiException(400,"Biến thể trả hàng không hợp lệ");
        String payload=sorted.stream().map(l->l.variantId()+":"+l.quantity()).collect(java.util.stream.Collectors.joining(","));String requestHash=hash(payload);
        var old=partialReturns.findById(returnId);
        if(old.isPresent()){if(!old.get().orderId.equals(orderId)||!old.get().requestHash.equals(requestHash))throw new ApiException(409,"Mã trả hàng đã dùng cho nội dung khác");return new ReservationView(orderId,"RESTOCKED",r.expiresAt,sorted);}
        if(!r.state.equals("COMMITTED")||partialReturns.existsByOrderId(orderId))throw new ApiException(409,"Đơn không đủ điều kiện hoàn kho một phần");
        int returned=0,total=0;for(var i:r.items)total+=i.quantity;
        for(Line line:sorted){var reserved=r.items.stream().filter(i->i.variantId.equals(line.variantId())).findFirst().orElseThrow(()->new ApiException(400,"Biến thể không thuộc đơn"));if(line.quantity()<1||line.quantity()>reserved.quantity)throw new ApiException(400,"Số lượng trả vượt số đã xuất");returned+=line.quantity();}
        if(returned>=total)throw new ApiException(400,"Trả toàn bộ phải dùng quy trình hoàn kho toàn đơn");
        for(Line line:sorted){Stock s=stocks.findById(line.variantId()).orElseThrow(ApiException::missing);s.onHand=Math.addExact(s.onHand,line.quantity());if(s.onHand>1000000000)throw new ApiException(409,"Tồn kho vượt giới hạn");stocks.save(s);log(s,"partial-return:"+returnId+":"+line.variantId(),requestHash,"RESTOCK",line.quantity(),0,"Hoàn một phần đơn "+orderId,0L);}
        PartialReturnRestock saved=new PartialReturnRestock();saved.returnId=returnId;saved.orderId=orderId;saved.requestHash=requestHash;partialReturns.save(saved);
        return new ReservationView(orderId,"RESTOCKED",r.expiresAt,sorted);
    }
    @Transactional public void expire(String id){lock();var found=reservations.findById(id);if(found.isEmpty())return;var r=found.get();if(r.state.equals("HELD")&&!r.expiresAt.isAfter(Instant.now())){move(r,false,false);r.state="EXPIRED";reservations.save(r);}}
    @Transactional(readOnly=true) public ReservationView reservation(String id){return reservationView(reservations.findById(id).orElseThrow(ApiException::missing));}
    private void move(Reservation r,boolean commit,boolean restock){for(var l:r.items){Stock s=stocks.findById(l.variantId).orElseThrow(ApiException::missing);int delta=restock?l.quantity:commit?-l.quantity:0;int rd=restock?0:-l.quantity;s.onHand+=delta;s.reserved+=rd;if(s.onHand<0||s.reserved<0||s.reserved>s.onHand||s.onHand>1000000000)throw new ApiException(409,"Thao tác vi phạm ràng buộc tồn kho");stocks.save(s);String type=restock?"RESTOCK":commit?"COMMIT":"RELEASE";log(s,type.toLowerCase(Locale.ROOT)+":"+r.orderId+":"+s.variantId,r.requestHash,type,delta,rd,"Xử lý kho cho đơn "+r.orderId,0L);}}
    private ReservationView reservationView(Reservation r){return new ReservationView(r.orderId,r.state,r.expiresAt,r.items.stream().map(i -> new Line(i.variantId,i.quantity)).toList());}
    private void log(Stock s,String key,String hash,String type,int delta,int reservedDelta,String reason,long actor){var t=new InventoryTransaction();t.variantId=s.variantId;t.operationKey=key;t.requestHash=hash;t.type=type;t.quantityDelta=delta;t.reservedDelta=reservedDelta;t.reason=reason;t.actorId=actor;t.resultOnHand=s.onHand;t.resultReserved=s.reserved;t.resultMinimum=s.minimumStock;transactions.save(t);}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
