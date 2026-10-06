package vn.shop.order;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.math.*;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import vn.shop.common.ApiException;
import vn.shop.common.PageResult;
import vn.shop.order.OrderDtos.FulfillmentInput;
import vn.shop.order.ReturnDtos.*;

@Service
public class ReturnService {
 private final GuardRepository guard;private final OrderRepository orders;private final ReturnRepository returns;
 private final ReturnEventRepository events;private final PaymentBridge payment;private final OrderRemote inventory;
 public ReturnService(GuardRepository guard,OrderRepository orders,ReturnRepository returns,ReturnEventRepository events,PaymentBridge payment,OrderRemote inventory){
  this.guard=guard;this.orders=orders;this.returns=returns;this.events=events;this.payment=payment;this.inventory=inventory;
 }
 @Transactional(readOnly=true) public View get(String orderId,long actor,boolean manager){
  owned(orderId,actor,manager);return view(returns.findByOrderId(orderId).orElseThrow(ApiException::missing));
 }
 @Transactional(readOnly=true) public PageResult<View> list(String state,int page){
  if(state!=null&&!Set.of("REQUESTED","APPROVED","REJECTED","RECEIVED","REFUND_PENDING","REFUND_CONFIRMING","REFUNDED").contains(state))
   throw new ApiException(400,"Trạng thái trả hàng không hợp lệ");
  var data=returns.search(state,PageRequest.of(page,20,Sort.by("updatedAt").descending().and(Sort.by("id").descending())));
  return new PageResult<>(data.map(this::view).getContent(),data.getTotalElements(),data.getTotalPages(),page,20,data.isLast());
 }
 @Transactional public View request(String orderId,long actor,String key,Request input){
  if(key==null||!key.matches("[A-Za-z0-9_-]{8,100}"))throw new ApiException(400,"Idempotency-Key không hợp lệ");
  guard.lock();ShopOrder order=owned(orderId,actor,false);
  String reason=input.reason().trim();
  List<ReturnLine> selected=input.items()==null?List.of():input.items();
  String selection=selected.stream().sorted(Comparator.comparingLong(ReturnLine::variantId)).map(l->l.variantId()+":"+l.quantity()).collect(java.util.stream.Collectors.joining(","));
  var previous=returns.findByOrderId(orderId);
  if(previous.isPresent()){
   ReturnRequest r=previous.get();
   String old=r.items.stream().map(l->l.variantId+":"+l.quantity).collect(java.util.stream.Collectors.joining(","));
   if(!r.requestKey.equals(key)||!r.reason.equals(reason)||!old.equals(selection))throw new ApiException(409,"Đơn đã có yêu cầu trả hàng khác");
   return view(r);
  }
  if(!order.state.equals("COMPLETED")||order.pendingCommandId!=null||!Set.of("PAID","SIMULATED_PAID").contains(order.paymentState))
   throw new ApiException(409,"Chỉ yêu cầu trả đơn đã hoàn tất và thanh toán");
  Instant completed=order.history.stream().filter(h->h.state.equals("COMPLETED")).map(h->h.createdAt).max(Instant::compareTo).orElse(order.createdAt);
  if(completed.isBefore(Instant.now().minus(14,ChronoUnit.DAYS)))throw new ApiException(409,"Đã quá 14 ngày kể từ khi hoàn tất đơn");
  ReturnRequest r=new ReturnRequest();r.id=UUID.randomUUID().toString();r.orderId=orderId;r.userId=actor;r.requestKey=key;r.reason=reason;
  if(selected.isEmpty())r.refundAmount=order.total;
  else{
   if(selected.size()>100||selected.stream().map(ReturnLine::variantId).distinct().count()!=selected.size())throw new ApiException(400,"Danh sách biến thể trả không hợp lệ");
   BigDecimal returnedSubtotal=BigDecimal.ZERO;int returnedCount=0,totalCount=0;
   for(OrderItem item:order.items)totalCount+=item.quantity;
   for(ReturnLine line:selected){
    OrderItem bought=order.items.stream().filter(i->i.variantId==line.variantId()).findFirst().orElseThrow(()->new ApiException(400,"Biến thể không thuộc đơn"));
    if(line.quantity()<1||line.quantity()>bought.quantity)throw new ApiException(400,"Số lượng trả vượt số đã mua");
    returnedCount+=line.quantity();returnedSubtotal=returnedSubtotal.add(bought.unitPrice.multiply(BigDecimal.valueOf(line.quantity())));
    ReturnItem ri=new ReturnItem();ri.request=r;ri.variantId=line.variantId();ri.quantity=line.quantity();r.items.add(ri);
   }
   if(returnedCount>=totalCount)throw new ApiException(400,"Trả toàn bộ đơn hãy chọn chế độ trả toàn bộ");
   BigDecimal allocatedDiscount=order.discount.multiply(returnedSubtotal).divide(order.subtotal,2,RoundingMode.HALF_UP);
   r.refundAmount=returnedSubtotal.subtract(allocatedDiscount);r.mode="PARTIAL";
   if(r.refundAmount.signum()<=0||r.refundAmount.compareTo(order.total)>=0)throw new ApiException(400,"Số tiền hoàn không hợp lệ");
  }
  returns.saveAndFlush(r);event(r,"REQUESTED",actor,reason);return view(r);
 }
 @Transactional public View decide(String orderId,long actor,Decision input){
  guard.lock();ReturnRequest r=existing(orderId);
  String next=input.action().equals("APPROVE")?"APPROVED":"REJECTED";
  String note=input.note()==null?"":input.note().trim();
  if(next.equals("REJECTED")&&note.isBlank())throw new ApiException(400,"Cần lý do từ chối");
  if(r.state.equals(next)){
   if(!Objects.equals(r.decisionNote,note))throw new ApiException(409,"Quyết định đã lưu với ghi chú khác");
   return view(r);
  }
  if(!r.state.equals("REQUESTED"))throw new ApiException(409,"Yêu cầu không còn chờ duyệt");
  r.decisionNote=note;r.actorId=actor;change(r,next,actor,note);return view(r);
 }
 @Transactional public View receive(String orderId,long actor,Receipt input){
  guard.lock();ReturnRequest r=existing(orderId);
  if(Set.of("RECEIVED","REFUND_PENDING","REFUND_CONFIRMING","REFUNDED").contains(r.state))return view(r);
  if(!r.state.equals("APPROVED"))throw new ApiException(409,"Cần duyệt yêu cầu trước khi nhận hàng");
  r.actorId=actor;change(r,"RECEIVED",actor,input.note().trim());return view(r);
 }
 @Transactional public View confirm(String orderId,long actor,Confirm input){
  guard.lock();ReturnRequest r=existing(orderId);ShopOrder order=orders.findById(orderId).orElseThrow(ApiException::missing);
  if(!order.paymentMethod.equals("COD"))throw new ApiException(409,"Đơn mô phỏng không cần chứng từ hoàn COD");
  String reference=input.reference().trim();
  if(Set.of("REFUND_CONFIRMING","REFUNDED").contains(r.state)){
   if(!reference.equals(r.refundReference))throw new ApiException(409,"Chứng từ hoàn tiền đã lưu khác");
   return view(r);
  }
  if(!r.state.equals("REFUND_PENDING"))throw new ApiException(409,"Chưa nhận hàng hoặc chưa tạo yêu cầu hoàn COD");
  r.refundReference=reference;r.actorId=actor;change(r,"REFUND_CONFIRMING",actor,"ADMIN xác nhận đã hoàn COD: "+reference);return view(r);
 }
 @Transactional public void reconcile(String id){
  guard.lock();ReturnRequest r=returns.findById(id).orElseThrow(ApiException::missing);
  if(!Set.of("RECEIVED","REFUND_CONFIRMING").contains(r.state))return;
  ShopOrder order=orders.findById(r.orderId).orElseThrow(ApiException::missing);
  try{
   if(r.state.equals("RECEIVED")&&r.mode.equals("PARTIAL")){
    payment.partial(order,r.id+"-partial-init",r.actorId==null?0:r.actorId,"PARTIAL_RETURN_INIT",r.refundAmount,r.reason,null);
    var stock=inventory.mutate(order.id,"/returns/"+r.id,Map.of("items",r.items.stream().map(i->Map.of("variantId",i.variantId,"quantity",i.quantity)).toList()));
    if(!stock.path("state").asText().equals("RESTOCKED"))throw new ApiException(503,"Kho chưa xác nhận hoàn tồn một phần");
    if(order.paymentMethod.equals("SIMULATED")){order.returnedAmount=r.refundAmount;change(r,"REFUNDED",r.actorId==null?0:r.actorId,"Đã hoàn giao dịch mô phỏng một phần");}
    else change(r,"REFUND_PENDING",r.actorId==null?0:r.actorId,"Đã hoàn kho; chờ chứng từ hoàn COD một phần");
   }else if(r.state.equals("REFUND_CONFIRMING")&&r.mode.equals("PARTIAL")){
    payment.partial(order,r.id+"-partial-confirm",r.actorId==null?0:r.actorId,"PARTIAL_RETURN_CONFIRM",r.refundAmount,r.reason,r.refundReference);
    order.returnedAmount=r.refundAmount;change(r,"REFUNDED",r.actorId==null?0:r.actorId,"Đã ghi nhận hoàn COD một phần");
   }else if(r.state.equals("RECEIVED")){
    var paymentResult=payment.execute(order,r.id+"-init",r.actorId==null?0:r.actorId,
     new FulfillmentInput("RETURN_INIT",null,null,null,null,null,r.reason));
    if(order.paymentMethod.equals("SIMULATED"))PaymentBridge.project(order,paymentResult);
    else order.shippingState=paymentResult.path("shippingState").asText();
    var stock=inventory.mutate(order.id,"/restock",Map.of());
    if(!stock.path("state").asText().equals("RESTOCKED"))throw new ApiException(503,"Kho chưa xác nhận hoàn tồn");
    if(order.paymentMethod.equals("SIMULATED")){
     OrderService.change(order,"RETURNED",r.actorId==null?0:r.actorId);change(r,"REFUNDED",r.actorId==null?0:r.actorId,"Giao dịch mô phỏng đã hoàn; không chuyển tiền thật");
    }else change(r,"REFUND_PENDING",r.actorId==null?0:r.actorId,"Đã hoàn kho; chờ ADMIN ghi chứng từ hoàn COD thực tế");
   }else{
    var paymentResult=payment.execute(order,r.id+"-confirm",r.actorId==null?0:r.actorId,
     new FulfillmentInput("RETURN_CONFIRM",null,null,null,r.refundReference,null,"Xác nhận hoàn COD"));
    PaymentBridge.project(order,paymentResult);
    OrderService.change(order,"RETURNED",r.actorId==null?0:r.actorId);
    change(r,"REFUNDED",r.actorId==null?0:r.actorId,"Đã ghi nhận hoàn COD");
   }
   r.lastError=null;
  }catch(ApiException e){r.lastError=e.getMessage();r.updatedAt=Instant.now();}
 }
 private ShopOrder owned(String id,long actor,boolean manager){
  ShopOrder o=orders.findById(id).orElseThrow(ApiException::missing);
  if(!manager&&o.userId!=actor)throw ApiException.missing();
  return o;
 }
 private ReturnRequest existing(String orderId){return returns.findByOrderId(orderId).orElseThrow(ApiException::missing);}
 private void change(ReturnRequest r,String state,long actor,String note){r.state=state;r.updatedAt=Instant.now();event(r,state,actor,note);}
 private void event(ReturnRequest r,String state,long actor,String note){
  ReturnEvent e=new ReturnEvent();e.returnId=r.id;e.state=state;e.actorId=actor;e.note=note;events.saveAndFlush(e);
 }
 private View view(ReturnRequest r){return new View(r.id,r.orderId,r.userId,r.state,r.reason,r.mode,r.refundAmount,r.items.stream().map(i->new ReturnLine(i.variantId,i.quantity)).toList(),r.decisionNote,r.refundReference,r.lastError,r.createdAt,r.updatedAt,
  events.findByReturnIdOrderByIdAsc(r.id).stream().map(e->new Event(e.state,e.actorId,e.note,e.createdAt)).toList());}
}
