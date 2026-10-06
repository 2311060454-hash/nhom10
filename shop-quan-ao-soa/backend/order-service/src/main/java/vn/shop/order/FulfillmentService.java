package vn.shop.order;
import vn.shop.common.*;
import vn.shop.order.OrderDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Service
public class FulfillmentService {
 private final GuardRepository guard;private final OrderRepository orders;private final FulfillmentRepository commands;private final PaymentBridge bridge;private final ObjectMapper mapper;
 public FulfillmentService(GuardRepository g,OrderRepository o,FulfillmentRepository c,PaymentBridge b,ObjectMapper m){guard=g;orders=o;commands=c;bridge=b;mapper=m;}
 @Transactional public CommandView submit(String orderId,long actor,boolean manager,String key,FulfillmentInput in){
  if(!key.matches("[A-Za-z0-9_-]{8,100}"))throw new ApiException(400,"Idempotency-Key không hợp lệ");
  guard.lock();var o=owned(orderId,actor,manager);String payload=encode(in);String hash=hash(actor+":"+payload);
  var existing=commands.findByOrderIdAndRequestKey(orderId,key);if(existing.isPresent()){var c=existing.get();if(!c.requestHash.equals(hash))throw new ApiException(409,"Mã yêu cầu đã dùng với dữ liệu khác");return view(c);}
  if(o.pendingCommandId!=null)throw new ApiException(409,"Có thao tác đang đối soát. Chờ kết quả trước khi tiếp tục");
  if(in.action().startsWith("SIM_")){
   if(o.userId!=actor)throw new ApiException(403,"Chỉ chủ đơn được chọn kết quả mô phỏng");
   require(o.paymentMethod.equals("SIMULATED")&&o.state.equals("AWAITING_PAYMENT"),"Đơn không còn chờ thanh toán mô phỏng");
   require(o.createdAt.isAfter(Instant.now().minusSeconds(900)),"Hết thời hạn thanh toán mô phỏng");
  }else{
   if(!manager)throw new ApiException(403,"Chỉ ADMIN/STAFF được xử lý vận chuyển và COD");
   String expected=switch(in.action()){case "SHIP"->"PACKING";case "DELIVER","DELIVERY_FAIL"->"SHIPPED";case "RETRY_SHIP","RETURN_RECEIVED"->"DELIVERY_FAILED";case "COLLECT_COD"->"DELIVERED";default->throw new ApiException(400,"Thao tác không hỗ trợ");};
   require(o.state.equals(expected),"Trạng thái đơn không cho phép thao tác này");
   if(in.action().equals("COLLECT_COD"))require(o.paymentMethod.equals("COD"),"Đây không phải đơn COD");
  }
  var c=new FulfillmentCommand();c.id=UUID.randomUUID().toString();c.orderId=orderId;c.requestKey=key;c.requestHash=hash;c.action=in.action();c.actorId=actor;c.payload=payload;commands.save(c);o.pendingCommandId=c.id;o.updatedAt=Instant.now();return view(c);
 }
 @Transactional(readOnly=true) public List<CommandView> list(String orderId,long actor,boolean manager){owned(orderId,actor,manager);return commands.findTop20ByOrderIdOrderByCreatedAtDesc(orderId).stream().map(this::view).toList();}
 /** Gọi trong transaction OrderSaga, sau khi đã lấy order_guard. */
 void process(ShopOrder o){
  var c=commands.findById(o.pendingCommandId).orElseThrow(ApiException::missing);c.updatedAt=Instant.now();
  try{
   bridge.initialize(o);var result=bridge.execute(o,c.id,c.actorId,decode(c.payload));PaymentBridge.project(o,result);
   String next=switch(c.action){
    case "SIM_SUCCESS"->"PLACED";case "SIM_FAILURE"->"FAIL_PENDING";case "SHIP","RETRY_SHIP"->"SHIPPED";
    case "DELIVER"->o.paymentState.equals("SIMULATED_PAID")?"COMPLETED":"DELIVERED";
    case "COLLECT_COD"->"COMPLETED";case "DELIVERY_FAIL"->"DELIVERY_FAILED";case "RETURN_RECEIVED"->"CANCEL_PENDING";
    default->throw new IllegalStateException("Unexpected persisted action");};
   OrderService.change(o,next,c.actorId);c.state="DONE";o.pendingCommandId=null;
  }catch(ApiException e){if(e.status()==409){c.state="REJECTED";c.failure=e.getMessage();o.pendingCommandId=null;}else if(e.status()!=503)throw e;}
 }
 private ShopOrder owned(String id,long actor,boolean manager){var o=orders.findById(id).orElseThrow(ApiException::missing);if(!manager&&o.userId!=actor)throw ApiException.missing();return o;}
 private CommandView view(FulfillmentCommand c){return new CommandView(c.id,c.action,c.state,c.failure,c.createdAt);}
 private void require(boolean condition,String message){if(!condition)throw new ApiException(409,message);}
 private String encode(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
 private FulfillmentInput decode(String value){try{return mapper.readValue(value,FulfillmentInput.class);}catch(Exception e){throw new IllegalStateException(e);}}
 private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
