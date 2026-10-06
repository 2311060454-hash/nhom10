package vn.shop.inventory;
import vn.shop.inventory.service.InventoryService;
import vn.shop.inventory.dto.InventoryDtos.*;
import vn.shop.inventory.entity.InventoryGuard;
import vn.shop.inventory.repository.*;
import vn.shop.common.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class InventoryIntegrationTest {
 @Autowired InventoryService service;@Autowired StockRepository stocks;@Autowired TransactionRepository transactions;@Autowired ReservationRepository reservations;@Autowired PartialReturnRestockRepository partialReturns;@Autowired GuardRepository guards;@Autowired MockMvc mvc;@Autowired JwtService jwt;
 @MockitoBean RemoteSessionVerifier verifier;
 @BeforeEach void setup(){partialReturns.deleteAll();reservations.deleteAll();transactions.deleteAll();stocks.deleteAll();if(!guards.existsById(1L)){InventoryGuard g=new InventoryGuard();g.id=1L;guards.save(g);}when(verifier.valid(any())).thenReturn(true);}
 void stock(int qty){service.adjust("initial-test-stock",new Adjustment(1L,"RECEIPT",qty,"Test"),1);}
 String id(){return UUID.randomUUID().toString();}
 Reserve one(){return new Reserve(List.of(new Line(1L,1)));}
 @Test void concurrentCustomersCannotOversellLastUnit()throws Exception{
  stock(1);var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
  Callable<Integer> call=() -> {gate.await();try{service.reserve(id(),one());return 200;}catch(ApiException e){return e.status();}};
  try{var a=pool.submit(call);var b=pool.submit(call);gate.countDown();assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);}finally{pool.shutdownNow();}
  assertThat(service.available(1).available()).isZero();assertThat(stocks.findById(1L).orElseThrow().reserved).isEqualTo(1);
 }
 @Test void reserveAndReleaseAreIdempotent(){stock(5);String order=id();service.reserve(order,one());service.reserve(order,one());assertThat(service.available(1).available()).isEqualTo(4);service.finish(order,"release");service.finish(order,"release");assertThat(service.available(1).available()).isEqualTo(5);assertThat(transactions.count()).isEqualTo(3);}
 @Test void commitAndRestockExactlyOnce(){stock(5);String order=id();service.reserve(order,one());service.finish(order,"commit");service.finish(order,"commit");assertThat(stocks.findById(1L).orElseThrow().onHand).isEqualTo(4);service.finish(order,"restock");service.finish(order,"restock");assertThat(stocks.findById(1L).orElseThrow().onHand).isEqualTo(5);assertThatThrownBy(() -> service.finish(order,"commit")).isInstanceOf(ApiException.class);}
 @Test void partialReturnRestocksOnlySelectedUnitsOnce(){stock(5);String order=id(),returnId=id();service.reserve(order,new Reserve(List.of(new Line(1L,2))));service.finish(order,"commit");
  var selected=one();assertThat(service.partialRestock(order,returnId,selected).state()).isEqualTo("RESTOCKED");service.partialRestock(order,returnId,selected);
  assertThat(stocks.findById(1L).orElseThrow().onHand).isEqualTo(4);
  assertThatThrownBy(()->service.partialRestock(order,returnId,new Reserve(List.of(new Line(1L,2))))).isInstanceOf(ApiException.class);
  assertThatThrownBy(()->service.partialRestock(order,id(),selected)).isInstanceOf(ApiException.class);
 }
 @Test void insufficientBatchRollsBackAllChanges(){stock(2);assertThatThrownBy(() -> service.reserve(id(),new Reserve(List.of(new Line(1L,1),new Line(2L,1))))).isInstanceOf(ApiException.class);assertThat(service.available(1).available()).isEqualTo(2);assertThat(reservations.count()).isZero();assertThat(transactions.count()).isEqualTo(1);}
 @Test void idempotencyKeyCannotBeReusedWithDifferentPayload(){stock(2);String order=id();service.reserve(order,one());assertThatThrownBy(() -> service.reserve(order,new Reserve(List.of(new Line(1L,2))))).isInstanceOf(ApiException.class);assertThatThrownBy(() -> service.adjust("initial-test-stock",new Adjustment(1L,"RECEIPT",10,"Test"),1)).isInstanceOf(ApiException.class);}
 @Test void cannotIssueOrCountBelowReserved(){stock(5);service.reserve(id(),new Reserve(List.of(new Line(1L,4))));assertThatThrownBy(() -> service.adjust("bad-issue-key",new Adjustment(1L,"ISSUE",2,"Test"),1)).isInstanceOf(ApiException.class);assertThatThrownBy(() -> service.adjust("bad-count-key",new Adjustment(1L,"COUNT",2,"Test"),1)).isInstanceOf(ApiException.class);assertThat(service.available(1).available()).isEqualTo(1);}
 @Test void expiredHoldIsReleasedOnce(){stock(5);String order=id();service.reserve(order,one());var r=reservations.findById(order).orElseThrow();r.expiresAt=Instant.now().minusSeconds(1);reservations.save(r);assertThatThrownBy(() -> service.finish(order,"commit")).isInstanceOf(ApiException.class);service.expire(order);service.expire(order);assertThat(service.available(1).available()).isEqualTo(5);assertThat(service.reservation(order).state()).isEqualTo("EXPIRED");}
 @Test void publicOnlySeesAvailabilityAndStaffNeedsPermission()throws Exception{
  stock(2);mvc.perform(get("/api/inventory/availability/1")).andExpect(status().isOk()).andExpect(jsonPath("available").value(2)).andExpect(jsonPath("onHand").doesNotExist());
  String token=jwt.issue(2,"test",List.of("STAFF"),false);
  mvc.perform(post("/api/inventory/adjustments").header("Authorization","Bearer "+token).header("Idempotency-Key","test-denied-key").contentType("application/json").content("{\"variantId\":1,\"type\":\"RECEIPT\",\"quantity\":1,\"reason\":\"Test\"}")).andExpect(status().isForbidden());
  mvc.perform(post("/internal/inventory/reservations/"+id()).contentType("application/json").content("{\"items\":[{\"variantId\":1,\"quantity\":1}]}")).andExpect(status().isUnauthorized());
  String admin=jwt.issue(1,"test-admin",List.of("ADMIN"),true);
  mvc.perform(post("/api/inventory/adjustments").header("Authorization","Bearer "+admin).contentType("application/json").content("{\"variantId\":1,\"type\":\"RECEIPT\",\"quantity\":1,\"reason\":\"Test\"}")).andExpect(status().isBadRequest());
 }
 @Test void csvExportUsesActualStockAndBackendRoles()throws Exception{
  stock(5);service.adjust("min-export-key",new Adjustment(1L,"MINIMUM",5,"Mức tối thiểu"),1);
  service.adjust("second-export-key",new Adjustment(2L,"RECEIPT",9,"Nhập để xuất báo cáo"),1);
  String staff=jwt.issue(5,"staff",List.of("STAFF"),false),customer=jwt.issue(2,"customer",List.of("CUSTOMER"),false);
  mvc.perform(get("/api/inventory/export.csv")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/inventory/export.csv").header("Authorization","Bearer "+customer)).andExpect(status().isForbidden());
  var result=mvc.perform(get("/api/inventory/export.csv?low=true").header("Authorization","Bearer "+staff)).andExpect(status().isOk()).andReturn();
  String csv=new String(result.getResponse().getContentAsByteArray(),java.nio.charset.StandardCharsets.UTF_8);
  assertThat(csv).startsWith("\uFEFFMã biến thể,").contains("1,5,0,5,5,Có").doesNotContain("2,9,0,9,5,Không");
  assertThat(new String(service.exportCsv(2L,false),java.nio.charset.StandardCharsets.UTF_8)).contains("2,9,0,9,5,Không").doesNotContain("1,5,0,5,5,Có");
 }
}
