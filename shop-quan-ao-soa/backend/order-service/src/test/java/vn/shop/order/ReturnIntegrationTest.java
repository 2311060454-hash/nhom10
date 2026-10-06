package vn.shop.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import vn.shop.common.*;
import vn.shop.order.OrderDtos.*;
import vn.shop.order.ReturnDtos.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ReturnIntegrationTest {
 @Autowired ReturnService service;
 @Autowired OrderService orderService;
 @Autowired OrderRepository orders;
 @Autowired CartRepository carts;
 @Autowired GuardRepository guards;
 @Autowired ReturnRepository returns;
 @Autowired ReturnEventRepository events;
 @Autowired FulfillmentRepository fulfillment;
 @Autowired OrderReportingController reporting;
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper mapper;
 @Autowired JwtService jwt;
 @MockitoBean OrderRemote remote;
 @MockitoBean PaymentBridge payment;
 @MockitoBean PromotionBridge promotion;
 @MockitoBean RemoteSessionVerifier verifier;
 @BeforeEach void setup() throws Exception {
  events.deleteAll();returns.deleteAll();fulfillment.deleteAll();orders.deleteAll();carts.deleteAll();
  if(!guards.existsById(1L)){OrderGuard g=new OrderGuard();g.id=1L;guards.save(g);}
  when(verifier.valid(any())).thenReturn(true);
  when(promotion.price(anyLong(),anyLong(),any())).thenAnswer(c->c.getArgument(2));
  when(remote.quote(anyLong())).thenReturn(mapper.readTree("{\"productId\":1,\"categoryId\":7,\"sku\":\"TEST-S\",\"productName\":\"Áo\",\"size\":\"S\",\"color\":\"Đen\",\"price\":100000,\"active\":true}"));
  when(remote.mutate(anyString(),eq("/restock"),any())).thenReturn(mapper.createObjectNode().put("state","RESTOCKED"));
  when(payment.execute(any(),anyString(),anyLong(),any())).thenAnswer(c->{
   ShopOrder o=c.getArgument(0);FulfillmentInput in=c.getArgument(3);
   String state=in.action().equals("RETURN_CONFIRM")?"REFUNDED":o.paymentMethod.equals("COD")?"REFUND_PENDING":"SIMULATED_REFUNDED";
   return mapper.createObjectNode().put("paymentState",state).put("shippingState","RETURNED");
  });
 }
 String completed(String method) {
  var cart=orderService.setQuantity(1,1,1);
  var o=orderService.checkout(1,UUID.randomUUID().toString(),new Checkout(cart.revision(),"Khách","0901234567","Hà Nội","",method));
  ShopOrder row=orders.findById(o.id()).orElseThrow();
  row.paymentState=method.equals("COD")?"PAID":"SIMULATED_PAID";row.shippingState="DELIVERED";
  OrderService.change(row,"COMPLETED",2);orders.save(row);return o.id();
 }
 @Test void requestRequiresOwnerCompletedOrderAndStableKey(){
  String id=completed("COD");Request input=new Request("Áo không đúng mô tả");
  assertThatThrownBy(()->service.request(id,2,"return-test-key",input)).isInstanceOf(ApiException.class);
  var first=service.request(id,1,"return-test-key",input);
  assertThat(service.request(id,1,"return-test-key",input).id()).isEqualTo(first.id());
  assertThatThrownBy(()->service.request(id,1,"other-key-123",input)).isInstanceOf(ApiException.class);
  assertThat(returns.count()).isEqualTo(1);
  assertThat(first.history()).hasSize(1);
 }
 @Test void refusesLateOrUnfinishedOrders(){
  String id=completed("COD");
  var row=orders.findById(id).orElseThrow();
  row.history.stream().filter(h->h.state.equals("COMPLETED")).forEach(h->h.createdAt=Instant.now().minus(15,ChronoUnit.DAYS));
  orders.save(row);
  assertThatThrownBy(()->service.request(id,1,"late-return-key",new Request("Đã quá thời hạn"))).isInstanceOf(ApiException.class);
  OrderService.change(row,"RETURNED",2);orders.save(row);
  assertThatThrownBy(()->service.request(id,1,"wrong-state-key",new Request("Không được trả nữa"))).isInstanceOf(ApiException.class);
 }
 @Test void codFlowRestocksOnceAndWaitsForManualRefund(){
  String id=completed("COD");service.request(id,1,"return-test-key",new Request("Áo bị lỗi đường may"));
  assertThatThrownBy(()->service.receive(id,2,new Receipt("Đủ hàng"))).isInstanceOf(ApiException.class);
  service.decide(id,2,new Decision("APPROVE","Đủ điều kiện"));
  service.receive(id,2,new Receipt("Đã nhận đủ áo"));
  String requestId=returns.findByOrderId(id).orElseThrow().id;
  service.reconcile(requestId);service.reconcile(requestId);
  assertThat(service.get(id,1,false).state()).isEqualTo("REFUND_PENDING");
  assertThat(orderService.get(id,1,false).state()).isEqualTo("COMPLETED");
  assertThat(orderService.get(id,1,false).paymentState()).isEqualTo("PAID");
  verify(remote,times(1)).mutate(eq(id),eq("/restock"),any());
  service.confirm(id,3,new Confirm("BANK-REFUND-123"));
  assertThat(service.confirm(id,3,new Confirm("BANK-REFUND-123")).state()).isEqualTo("REFUND_CONFIRMING");
  service.reconcile(requestId);service.reconcile(requestId);
  assertThat(service.get(id,1,false).state()).isEqualTo("REFUNDED");
  assertThat(orderService.get(id,1,false).state()).isEqualTo("RETURNED");
  assertThat(orderService.get(id,1,false).paymentState()).isEqualTo("REFUNDED");
  assertThatThrownBy(()->service.confirm(id,3,new Confirm("DIFFERENT"))).isInstanceOf(ApiException.class);
 }
 @Test void simulationRefundIsExplicitlyLocal(){
  String id=completed("SIMULATED");service.request(id,1,"simulation-return-key",new Request("Size không vừa người mặc"));
  service.decide(id,2,new Decision("APPROVE",""));service.receive(id,2,new Receipt("Đã nhận hàng"));
  service.reconcile(returns.findByOrderId(id).orElseThrow().id);
  assertThat(service.get(id,1,false).state()).isEqualTo("REFUNDED");
  assertThat(orderService.get(id,1,false).paymentState()).isEqualTo("SIMULATED_REFUNDED");
  assertThatThrownBy(()->service.confirm(id,3,new Confirm("BANK"))).isInstanceOf(ApiException.class);
 }
 @Test void partialCodReturnUsesSnapshotAndKeepsOrderCompleted(){
  var cart=orderService.setQuantity(1,1,2);
  var placed=orderService.checkout(1,UUID.randomUUID().toString(),new Checkout(cart.revision(),"Khách","0901234567","Hà Nội","","COD"));
  ShopOrder row=orders.findById(placed.id()).orElseThrow();row.paymentState="PAID";row.shippingState="DELIVERED";OrderService.change(row,"COMPLETED",2);orders.save(row);
  String id=placed.id();var request=service.request(id,1,"partial-test-key",new Request("Trả một áo lỗi may",List.of(new ReturnLine(1,1))));
  assertThat(request.mode()).isEqualTo("PARTIAL");assertThat(request.refundAmount()).isEqualByComparingTo("100000");
  assertThatThrownBy(()->service.request(id,1,"partial-test-key",new Request("Trả một áo lỗi may",List.of(new ReturnLine(1,2))))).isInstanceOf(ApiException.class);
  when(payment.partial(any(),anyString(),anyLong(),anyString(),any(),any(),any())).thenReturn(mapper.createObjectNode().put("paymentState","PAID").put("shippingState","DELIVERED"));
  when(remote.mutate(eq(id),eq("/returns/"+request.id()),any())).thenReturn(mapper.createObjectNode().put("state","RESTOCKED"));
  service.decide(id,2,new Decision("APPROVE",""));service.receive(id,2,new Receipt("Đã nhận một áo"));service.reconcile(request.id());
  assertThat(service.get(id,1,false).state()).isEqualTo("REFUND_PENDING");assertThat(orders.findById(id).orElseThrow().returnedAmount).isZero();
  service.confirm(id,3,new Confirm("BANK-PARTIAL-1"));service.reconcile(request.id());service.reconcile(request.id());
  assertThat(service.get(id,1,false).state()).isEqualTo("REFUNDED");assertThat(orders.findById(id).orElseThrow().state).isEqualTo("COMPLETED");
  assertThat(orders.findById(id).orElseThrow().returnedAmount).isEqualByComparingTo("100000");verify(remote,times(1)).mutate(eq(id),eq("/returns/"+request.id()),any());
  var report=reporting.report(Instant.now().minusSeconds(3600),Instant.now().plusSeconds(3600),1L,0,20);
  var record=report.content().stream().filter(o->o.id().equals(id)).findFirst().orElseThrow();
  assertThat(record.returnEvents()).extracting("state").contains("REQUESTED","APPROVED","REFUNDED");
 }
 @Test void inventoryTimeoutKeepsRequestForRetry() throws Exception {
  String id=completed("COD");service.request(id,1,"timeout-return-key",new Request("Áo có lỗi cần đổi trả"));
  service.decide(id,2,new Decision("APPROVE",""));service.receive(id,2,new Receipt("Đã nhận"));
  when(remote.mutate(eq(id),eq("/restock"),any())).thenThrow(new ApiException(503,"Kho không phản hồi"));
  String requestId=returns.findByOrderId(id).orElseThrow().id;service.reconcile(requestId);
  assertThat(service.get(id,1,false).state()).isEqualTo("RECEIVED");
  when(remote.mutate(eq(id),eq("/restock"),any())).thenReturn(mapper.createObjectNode().put("state","RESTOCKED"));
  service.reconcile(requestId);assertThat(service.get(id,1,false).state()).isEqualTo("REFUND_PENDING");
 }
 @Test void httpRequiresBackendRolesAndHidesOtherOwners() throws Exception {
  String id=completed("COD");service.request(id,1,"owner-return-key",new Request("Sản phẩm không phù hợp"));
  String owner="Bearer "+jwt.issue(1,"owner",List.of("CUSTOMER"),false);
  String other="Bearer "+jwt.issue(4,"other",List.of("CUSTOMER"),false);
  String staff="Bearer "+jwt.issue(2,"staff",List.of("STAFF"),false);
  mvc.perform(get("/api/orders/"+id+"/return").header("Authorization",other)).andExpect(status().isNotFound());
  mvc.perform(post("/api/orders/"+id+"/return/decision").header("Authorization",owner).contentType("application/json").content("{\"action\":\"APPROVE\"}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/orders/"+id+"/return/confirm-refund").header("Authorization",staff).contentType("application/json").content("{\"reference\":\"BANK-1\"}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/orders/"+id+"/return").header("Authorization",owner)).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("REQUESTED"));
  mvc.perform(get("/api/orders/returns/manage").header("Authorization",owner)).andExpect(status().isForbidden());
  mvc.perform(get("/api/orders/returns/manage?state=REQUESTED").header("Authorization",staff)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
 }
}

