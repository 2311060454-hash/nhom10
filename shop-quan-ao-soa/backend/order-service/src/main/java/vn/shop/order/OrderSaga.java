package vn.shop.order;
import vn.shop.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

/** Mỗi bước ghi kho dùng order UUID; rollback local không làm mất khả năng đối soát kết quả remote. */
@Service
public class OrderSaga {
 private final GuardRepository guard;private final OrderRepository orders;private final OrderRemote remote;private final PaymentBridge payment;private final FulfillmentService fulfillment;private final PromotionBridge promotion;
 public OrderSaga(GuardRepository g,OrderRepository o,OrderRemote r,PaymentBridge p,FulfillmentService f,PromotionBridge promo){guard=g;orders=o;remote=r;payment=p;fulfillment=f;promotion=promo;}
 @Transactional public void reconcile(String id){
  guard.lock();ShopOrder o=orders.findById(id).orElseThrow(ApiException::missing);
  if(o.pendingCommandId!=null){o.updatedAt=Instant.now();fulfillment.process(o);return;}
  if(o.state.equals("AWAITING_PAYMENT")){o.updatedAt=Instant.now();if(o.createdAt.isBefore(Instant.now().minusSeconds(900)))OrderService.change(o,"FAIL_PENDING",0);return;}
  if(!Set.of("PROCESSING","CANCEL_PENDING","FAIL_PENDING").contains(o.state))return;
  o.updatedAt=Instant.now();
  if(o.state.equals("PROCESSING")){
   if(o.createdAt.isBefore(Instant.now().minusSeconds(600))){OrderService.change(o,"FAIL_PENDING",0);return;}
   try{
    if(o.couponCode!=null&&promotion.reserve(o).path("discount").decimalValue().compareTo(o.discount)!=0)throw new ApiException(409,"Giá trị mã giảm giá đã thay đổi");
    var lines=o.items.stream().map(i->Map.of("variantId",i.variantId,"quantity",i.quantity)).toList();
    var held=remote.mutate(id,"",Map.of("items",lines));
    if(!Set.of("HELD","COMMITTED").contains(held.path("state").asText())){OrderService.change(o,"FAIL_PENDING",0);return;}
    PaymentBridge.project(o,payment.initialize(o));
    remote.mutate(id,"/commit",Map.of());if(o.couponCode!=null)promotion.commit(o);OrderService.change(o,o.paymentMethod.equals("SIMULATED")?"AWAITING_PAYMENT":"PLACED",0);
   }catch(ApiException e){if(e.status()==409)OrderService.change(o,"FAIL_PENDING",0);else if(e.status()!=503)throw e;}
  }else{
   String target=o.state.equals("CANCEL_PENDING")?"CANCELLED":"FAILED";
   try{
    try{
     String state=remote.reservation(id).path("state").asText();
     switch(state){case "HELD"->remote.mutate(id,"/release",Map.of());case "COMMITTED"->remote.mutate(id,"/restock",Map.of());case "RELEASED","EXPIRED","RESTOCKED"->{}default->throw new ApiException(503,"Trạng thái kho chưa xác định");}
    }catch(ApiException e){if(e.status()!=404)throw e;}
    if(o.couponCode!=null)promotion.release(o);
    PaymentBridge.project(o,payment.cancel(o));
    OrderService.change(o,target,0);
   }catch(ApiException e){if(e.status()!=503&&e.status()!=409)throw e;}
  }
 }
}

