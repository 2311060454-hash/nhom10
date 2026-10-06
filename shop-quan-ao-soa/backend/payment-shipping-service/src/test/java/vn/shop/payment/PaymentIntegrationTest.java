package vn.shop.payment;
import vn.shop.payment.PaymentDtos.*;
import vn.shop.common.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class PaymentIntegrationTest {
 @Autowired PaymentService service;@Autowired GuardRepository guards;@Autowired PaymentRepository payments;@Autowired ShipmentRepository shipments;
 @Autowired CommandRepository commands;@Autowired EventRepository events;@Autowired RefundRepository refunds;@Autowired MockMvc mvc;@Autowired JwtService jwt;
 @MockitoBean RemoteSessionVerifier verifier;
 @BeforeEach void setup(){commands.deleteAll();events.deleteAll();refunds.deleteAll();shipments.deleteAll();payments.deleteAll();if(!guards.existsById(1L)){var g=new PaymentGuard();g.id=1L;guards.save(g);}when(verifier.valid(any())).thenReturn(true);}
 Command cmd(String method,String action){return new Command(1,method,new BigDecimal("130000"),new BigDecimal("30000"),"Khách","0901234567","Hà Nội",action,2,"Local","TRACK-001","Nhân viên","COD-RECEIPT-001",new BigDecimal("25000"),"Kiểm thử");}
 View run(String id,String method,String action){return service.execute(id,UUID.randomUUID().toString(),cmd(method,action));}
 String init(String method){String id=UUID.randomUUID().toString();run(id,method,"INIT");return id;}
 @Test void codRequiresDeliveryAndCollection(){String id=init("COD");assertThatThrownBy(()->run(id,"COD","COLLECT_COD")).isInstanceOf(ApiException.class);run(id,"COD","SHIP");var delivered=run(id,"COD","DELIVER");assertThat(delivered.paymentState()).isEqualTo("UNPAID");assertThat(run(id,"COD","COLLECT_COD").paymentState()).isEqualTo("PAID");assertThatThrownBy(()->run(id,"COD","CANCEL")).isInstanceOf(ApiException.class);}
 @Test void repeatedCollectionReturnsSnapshotWithoutExtraEvent(){String id=init("COD");run(id,"COD","SHIP");run(id,"COD","DELIVER");String key=UUID.randomUUID().toString();var a=service.execute(id,key,cmd("COD","COLLECT_COD"));var b=service.execute(id,key,cmd("COD","COLLECT_COD"));assertThat(b).isEqualTo(a);assertThat(events.count()).isEqualTo(4);}
 @Test void changedPayloadUnderSameKeyIsConflict(){String id=init("COD");String key=UUID.randomUUID().toString();service.execute(id,key,cmd("COD","SHIP"));assertThatThrownBy(()->service.execute(id,key,cmd("COD","DELIVER"))).isInstanceOf(ApiException.class);}
 @Test void simulationNeverClaimsRealPayment(){String id=init("SIMULATED");assertThat(run(id,"SIMULATED","SIM_SUCCESS").paymentState()).isEqualTo("SIMULATED_PAID");assertThat(payments.findById(id).orElseThrow().state).isEqualTo("SIMULATED_PAID");assertThatThrownBy(()->run(id,"SIMULATED","COLLECT_COD")).isInstanceOf(ApiException.class);}
 @Test void failedSimulationCannotShip(){String id=init("SIMULATED");run(id,"SIMULATED","SIM_FAILURE");assertThatThrownBy(()->run(id,"SIMULATED","SHIP")).isInstanceOf(ApiException.class);assertThat(run(id,"SIMULATED","CANCEL").paymentState()).isEqualTo("SIMULATED_FAILED");}
 @Test void simulatedRefundExactlyOnceUnderConcurrentCancellation()throws Exception{
  String id=init("SIMULATED");run(id,"SIMULATED","SIM_SUCCESS");var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);Callable<View> task=()->{gate.await();return run(id,"SIMULATED","CANCEL");};try{var a=pool.submit(task);var b=pool.submit(task);gate.countDown();assertThat(a.get(10,TimeUnit.SECONDS).paymentState()).isEqualTo("SIMULATED_REFUNDED");assertThat(b.get(10,TimeUnit.SECONDS).paymentState()).isEqualTo("SIMULATED_REFUNDED");assertThat(refunds.count()).isEqualTo(1);}finally{pool.shutdownNow();}
 }
 @Test void failedDeliveryMustReturnBeforeCancel(){String id=init("SIMULATED");run(id,"SIMULATED","SIM_SUCCESS");run(id,"SIMULATED","SHIP");run(id,"SIMULATED","DELIVERY_FAIL");assertThatThrownBy(()->run(id,"SIMULATED","CANCEL")).isInstanceOf(ApiException.class);run(id,"SIMULATED","RETRY_SHIP");run(id,"SIMULATED","DELIVERY_FAIL");assertThat(run(id,"SIMULATED","RETURN_RECEIVED").shippingState()).isEqualTo("RETURNED");assertThat(run(id,"SIMULATED","CANCEL").paymentState()).isEqualTo("SIMULATED_REFUNDED");assertThat(refunds.count()).isEqualTo(1);}
 @Test void trackingCannotBelongToTwoOrders(){String first=init("COD"),second=init("COD");run(first,"COD","SHIP");assertThatThrownBy(()->run(second,"COD","SHIP")).isInstanceOf(ApiException.class);assertThat(service.get(second,1,false).shippingState()).isEqualTo("NEW");}
 @Test void ownerAndInternalKeyEnforced()throws Exception{String id=init("COD");String token=jwt.issue(2,"test",List.of("CUSTOMER"),false);mvc.perform(get("/api/payments/"+id).header("Authorization","Bearer "+token)).andExpect(status().isNotFound());mvc.perform(post("/internal/fulfillment/"+id+"/commands/test-command-key").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());}
 @Test void customerDoesNotSeeCarrierCost(){String id=init("COD");run(id,"COD","SHIP");assertThat(service.get(id,1,false).carrierCost()).isNull();assertThat(service.get(id,2,true).carrierCost()).isEqualByComparingTo("25000");}
 @Test void completedCodReturnNeedsManualProofAndIsIdempotent(){
  String id=init("COD");run(id,"COD","SHIP");run(id,"COD","DELIVER");run(id,"COD","COLLECT_COD");
  String key=UUID.randomUUID().toString();var pending=service.execute(id,key,cmd("COD","RETURN_INIT"));
  assertThat(pending.paymentState()).isEqualTo("REFUND_PENDING");assertThat(pending.shippingState()).isEqualTo("RETURNED");
  assertThat(refunds.count()).isEqualTo(1);
  assertThat(service.execute(id,key,cmd("COD","RETURN_INIT"))).isEqualTo(pending);
  assertThatThrownBy(()->run(id,"COD","RETURN_INIT")).isInstanceOf(ApiException.class);
  var done=run(id,"COD","RETURN_CONFIRM");
  assertThat(done.paymentState()).isEqualTo("REFUNDED");
  assertThat(done.refunds().get(0).reference()).isEqualTo("COD-RECEIPT-001");
  assertThat(refunds.count()).isEqualTo(1);
  assertThatThrownBy(()->run(id,"COD","RETURN_CONFIRM")).isInstanceOf(ApiException.class);
 }
 @Test void completedSimulationReturnNeverClaimsBankRefund(){
  String id=init("SIMULATED");run(id,"SIMULATED","SIM_SUCCESS");run(id,"SIMULATED","SHIP");run(id,"SIMULATED","DELIVER");
  var result=run(id,"SIMULATED","RETURN_INIT");
  assertThat(result.paymentState()).isEqualTo("SIMULATED_REFUNDED");
  assertThat(result.refunds().get(0).state()).isEqualTo("SIMULATED_REFUNDED");
  assertThat(result.refunds().get(0).reference()).isNull();
  assertThatThrownBy(()->run(id,"SIMULATED","RETURN_CONFIRM")).isInstanceOf(ApiException.class);
 }
 @Test void partialCodRefundKeepsRemainingOrderPaid(){String id=init("COD");run(id,"COD","SHIP");run(id,"COD","DELIVER");run(id,"COD","COLLECT_COD");
  Command base=cmd("COD","PARTIAL_RETURN_INIT");Command part=new Command(base.userId(),base.method(),base.amount(),base.shippingFee(),base.recipient(),base.phone(),base.address(),base.action(),base.actorId(),base.carrier(),base.tracking(),base.assignee(),base.reference(),base.carrierCost(),base.note(),new BigDecimal("50000"));
  String key=UUID.randomUUID().toString();assertThat(service.execute(id,key,part).paymentState()).isEqualTo("PAID");assertThat(service.execute(id,key,part).shippingState()).isEqualTo("DELIVERED");
  assertThat(refunds.findByOrderId(id).get(0).amount).isEqualByComparingTo("50000");
  Command confirm=new Command(part.userId(),part.method(),part.amount(),part.shippingFee(),part.recipient(),part.phone(),part.address(),"PARTIAL_RETURN_CONFIRM",part.actorId(),part.carrier(),part.tracking(),part.assignee(),"BANK-PARTIAL-1",part.carrierCost(),part.note(),part.refundAmount());
  assertThat(service.execute(id,UUID.randomUUID().toString(),confirm).paymentState()).isEqualTo("PAID");assertThat(refunds.findByOrderId(id).get(0).state).isEqualTo("REFUNDED");
 }
}
