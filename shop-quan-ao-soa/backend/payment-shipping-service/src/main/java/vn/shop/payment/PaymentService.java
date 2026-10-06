package vn.shop.payment;
import vn.shop.common.*;
import vn.shop.payment.PaymentDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class PaymentService {
 private final GuardRepository guard;private final PaymentRepository payments;private final ShipmentRepository shipments;
 private final CommandRepository commands;private final EventRepository events;private final RefundRepository refunds;private final ObjectMapper mapper;
 public PaymentService(GuardRepository g,PaymentRepository p,ShipmentRepository s,CommandRepository c,EventRepository e,RefundRepository r,ObjectMapper m){guard=g;payments=p;shipments=s;commands=c;events=e;refunds=r;mapper=m;}
 @Transactional public View execute(String orderId,String commandId,Command in){
  UUID.fromString(orderId);if(!commandId.matches("[A-Za-z0-9_-]{8,100}"))throw new ApiException(400,"Mã yêu cầu không hợp lệ");
  if(guard.lock()==null)throw new IllegalStateException("Missing payment guard");
  String hash=hash(in);var old=commands.findById(commandId);
  if(old.isPresent()){var c=old.get();if(!c.orderId.equals(orderId)||!c.requestHash.equals(hash))throw new ApiException(409,"Mã xử lý đã dùng với dữ liệu khác");return decode(c.response);}
  Payment p=payments.findById(orderId).orElse(null);Shipment s;
  if(p==null){
   if(!Set.of("INIT","CANCEL").contains(in.action()))throw new ApiException(409,"Chưa khởi tạo thanh toán");
   p=new Payment();p.orderId=orderId;p.userId=in.userId();p.method=in.method();p.amount=in.amount();p=payments.save(p);
   s=new Shipment();s.orderId=orderId;s.recipient=in.recipient();s.phone=in.phone();s.address=in.address();s.shippingFee=in.shippingFee();s=shipments.save(s);
  }else{s=shipments.findById(orderId).orElseThrow(ApiException::missing);if(p.userId!=in.userId()||!p.method.equals(in.method())||p.amount.compareTo(in.amount())!=0||s.shippingFee.compareTo(in.shippingFee())!=0)throw new ApiException(409,"Thông tin thanh toán không khớp đơn");}
  switch(in.action()){
   case "INIT" -> {}
   case "SIM_SUCCESS","SIM_FAILURE" -> {
    require(p.method.equals("SIMULATED")&&p.state.equals("UNPAID")&&s.state.equals("NEW"),"Giao dịch mô phỏng không còn chờ xử lý");
    p.state=in.action().equals("SIM_SUCCESS")?"SIMULATED_PAID":"SIMULATED_FAILED";p.reference="LOCAL-SIM-"+commandId;
   }
   case "SHIP" -> {
    require(s.state.equals("NEW")&&(p.method.equals("COD")&&p.state.equals("UNPAID")||p.state.equals("SIMULATED_PAID")),"Đơn chưa đủ điều kiện giao");
    require(text(in.carrier())&&text(in.tracking())&&text(in.assignee())&&in.carrierCost()!=null,"Nhập đơn vị, mã vận đơn, người phụ trách và chi phí giao");
    require(!shipments.existsByCarrierAndTrackingAndOrderIdNot(in.carrier().trim(),in.tracking().trim(),orderId),"Mã vận đơn đã dùng cho đơn khác");
    s.carrier=in.carrier().trim();s.tracking=in.tracking().trim();s.assignee=in.assignee().trim();s.carrierCost=in.carrierCost();s.state="SHIPPING";
   }
   case "DELIVER" -> {require(s.state.equals("SHIPPING"),"Đơn chưa đang vận chuyển");s.state="DELIVERED";}
   case "DELIVERY_FAIL" -> {require(s.state.equals("SHIPPING")&&text(in.note()),"Cần đơn đang giao và lý do giao thất bại");s.state="DELIVERY_FAILED";}
   case "RETRY_SHIP" -> {require(s.state.equals("DELIVERY_FAILED"),"Chỉ giao lại đơn đã giao thất bại");s.state="SHIPPING";}
   case "RETURN_RECEIVED" -> {require(s.state.equals("DELIVERY_FAILED")&&text(in.note()),"Chỉ nhận hàng hoàn sau giao thất bại, cần lý do");s.state="RETURNED";cancelPayment(p,"Nhận lại hàng giao thất bại");}
   case "COLLECT_COD" -> {
    require(p.method.equals("COD")&&p.state.equals("UNPAID")&&s.state.equals("DELIVERED")&&text(in.reference()),"Chỉ ghi thu COD sau giao thành công và có mã chứng từ");
    p.state="PAID";p.reference=in.reference().trim();
   }
   case "RETURN_INIT" -> {
    require(s.state.equals("DELIVERED")&&Set.of("PAID","SIMULATED_PAID").contains(p.state)&&text(in.note()),"Chỉ hoàn đơn đã giao và thanh toán, cần lý do trả hàng");
    require(refunds.findByOrderId(orderId).isEmpty(),"Đơn đã có giao dịch hoàn tiền");
    Refund r=new Refund();r.orderId=orderId;r.amount=p.amount;r.reason=in.note().trim();
    if(p.method.equals("SIMULATED")){r.state="SIMULATED_REFUNDED";p.state="SIMULATED_REFUNDED";}
    else{r.state="PENDING_MANUAL";p.state="REFUND_PENDING";}
    refunds.save(r);s.state="RETURNED";
   }
   case "RETURN_CONFIRM" -> {
    require(p.method.equals("COD")&&p.state.equals("REFUND_PENDING")&&s.state.equals("RETURNED")&&text(in.reference()),"Chỉ xác nhận hoàn COD đã thực hiện và có mã chứng từ");
    Refund r=refunds.findByOrderId(orderId).stream().findFirst().orElseThrow(ApiException::missing);
    require(r.state.equals("PENDING_MANUAL"),"Giao dịch hoàn tiền đã xử lý");
    r.state="REFUNDED";r.reference=in.reference().trim();r.updatedAt=Instant.now();p.state="REFUNDED";
   }
   case "PARTIAL_RETURN_INIT" -> {
    require(s.state.equals("DELIVERED")&&Set.of("PAID","SIMULATED_PAID").contains(p.state)&&text(in.note()),"Chỉ hoàn một phần đơn đã giao và thanh toán");
    require(in.refundAmount()!=null&&in.refundAmount().signum()>0&&in.refundAmount().compareTo(p.amount)<0,"Số tiền hoàn một phần không hợp lệ");
    require(refunds.findByOrderId(orderId).isEmpty(),"Đơn đã có giao dịch hoàn tiền");
    Refund r=new Refund();r.orderId=orderId;r.amount=in.refundAmount();r.reason=in.note().trim();r.state=p.method.equals("SIMULATED")?"SIMULATED_REFUNDED":"PENDING_MANUAL";refunds.save(r);
   }
   case "PARTIAL_RETURN_CONFIRM" -> {
    require(p.method.equals("COD")&&p.state.equals("PAID")&&s.state.equals("DELIVERED")&&text(in.reference()),"Chỉ xác nhận hoàn COD một phần có chứng từ");
    Refund r=refunds.findByOrderId(orderId).stream().findFirst().orElseThrow(ApiException::missing);
    require(r.state.equals("PENDING_MANUAL")&&in.refundAmount()!=null&&r.amount.compareTo(in.refundAmount())==0,"Giao dịch hoàn một phần không khớp");
    r.state="REFUNDED";r.reference=in.reference().trim();r.updatedAt=Instant.now();
   }
   case "CANCEL" -> {
    require(Set.of("NEW","CANCELLED","RETURNED").contains(s.state),"Không hủy hàng đang vận chuyển hoặc đã giao");
    cancelPayment(p,"Hủy đơn hoặc đặt hàng thất bại");if(!s.state.equals("RETURNED"))s.state="CANCELLED";
   }
   default -> throw new ApiException(400,"Thao tác không hỗ trợ");
  }
  p.updatedAt=Instant.now();s.updatedAt=Instant.now();FulfillmentEvent event=new FulfillmentEvent();event.orderId=orderId;event.action=in.action();event.actorId=in.actorId();event.note=in.note();events.saveAndFlush(event);
  View result=view(p,s);PaymentCommand c=new PaymentCommand();c.id=commandId;c.orderId=orderId;c.requestHash=hash;c.response=encode(result);commands.save(c);return result;
 }
 private void cancelPayment(Payment p,String reason){
  require(!p.state.equals("PAID"),"Tiền COD đã thu cần quy trình hoàn tiền riêng");
  if(p.state.equals("SIMULATED_PAID")){Refund r=new Refund();r.orderId=p.orderId;r.amount=p.amount;r.reason=reason;refunds.save(r);p.state="SIMULATED_REFUNDED";}
  else if(!p.state.equals("SIMULATED_REFUNDED")&&!p.state.equals("SIMULATED_FAILED"))p.state="CANCELLED";
 }
 @Transactional(readOnly=true) public View get(String id,long caller,boolean manager){Payment p=payments.findById(id).orElseThrow(ApiException::missing);if(!manager&&p.userId!=caller)throw ApiException.missing();return view(p,shipments.findById(id).orElseThrow(ApiException::missing),manager);}
 private View view(Payment p,Shipment s){return view(p,s,true);}
 private View view(Payment p,Shipment s,boolean manager){return new View(p.orderId,p.userId,p.method,p.state,p.amount,p.reference,s.state,s.recipient,s.phone,s.address,s.carrier,s.tracking,manager?s.assignee:null,s.shippingFee,manager?s.carrierCost:null,p.updatedAt,events.findByOrderIdOrderByIdAsc(p.orderId).stream().map(e->new Event(e.action,e.actorId,e.note,e.createdAt)).toList(),refunds.findByOrderId(p.orderId).stream().map(r->new RefundView(r.amount,r.state,r.reason,manager?r.reference:null,r.createdAt,r.updatedAt)).toList());}
 private static boolean text(String s){return s!=null&&!s.isBlank();}
 private static void require(boolean condition,String message){if(!condition)throw new ApiException(409,message);}
 private String encode(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
 private View decode(String value){try{return mapper.readValue(value,View.class);}catch(Exception e){throw new IllegalStateException(e);}}
 private String hash(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encode(value).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}


