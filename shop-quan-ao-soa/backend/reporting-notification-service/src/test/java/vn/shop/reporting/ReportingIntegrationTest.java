package vn.shop.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.shop.common.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ReportingIntegrationTest {
 @Autowired ReportService reports;@Autowired NotificationStore notifications;@Autowired NotificationRepository notificationRows;
 @Autowired NotificationGuardRepository guards;@Autowired ObjectMapper mapper;@Autowired MockMvc mvc;@Autowired JwtService jwt;
 @MockitoBean InternalHttp http;@MockitoBean RemoteSessionVerifier verifier;
 private JsonNode order(String id,long user,String state,String method,String payment,int total,Instant created,Instant completed)throws Exception{
  String events=completed==null?"[]":"[{\"state\":\"COMPLETED\",\"createdAt\":\""+completed+"\"}]";
  return mapper.readTree("{\"id\":\""+id+"\",\"userId\":"+user+",\"state\":\""+state+"\",\"paymentMethod\":\""+method+"\",\"paymentState\":\""+payment+"\",\"total\":"+total+",\"createdAt\":\""+created+"\",\"items\":[{\"productId\":7,\"productName\":\"Áo\",\"quantity\":2,\"unitPrice\":40000}],\"events\":"+events+"}");
 }
 private void mockOrders(JsonNode... rows)throws Exception{var obj=mapper.createObjectNode();var content=mapper.createArrayNode();for(JsonNode row:rows)content.add(row);obj.set("content",content);obj.put("last",true);when(http.get(eq(8084),startsWith("/internal/orders/report?"))).thenReturn(obj);}
 @BeforeEach void setup()throws Exception{notificationRows.deleteAll();if(!guards.existsById(1L)){NotificationGuard g=new NotificationGuard();g.id=1L;guards.save(g);}when(verifier.valid(any())).thenReturn(true);mockOrders();when(http.get(8082,"/internal/catalog/report-summary")).thenReturn(mapper.readTree("{\"productCount\":8}"));when(http.get(8081,"/internal/users/report-summary")).thenReturn(mapper.readTree("{\"customerCount\":3}"));when(http.get(8083,"/internal/inventory/report-summary")).thenReturn(mapper.readTree("{\"lowStockCount\":2}"));when(http.get(eq(8081),startsWith("/internal/users/"))).thenReturn(mapper.readTree("{\"enabled\":false}"));}
 @Test void dashboardSeparatesRealizedCashFromSimulationAndCancelled()throws Exception{
  Instant now=Instant.now(),old=now.minusSeconds(86400);mockOrders(order("a",1,"COMPLETED","COD","PAID",100000,old,now),order("b",2,"COMPLETED","SIMULATED","SIMULATED_PAID",120000,old,now),order("c",3,"CANCELLED","COD","PAID",50000,old,now));
  var d=reports.dashboard();assertThat(d.realizedRevenue()).isEqualByComparingTo("100000");assertThat(d.simulatedTurnover()).isEqualByComparingTo("120000");assertThat(d.todayRevenue()).isEqualByComparingTo("100000");assertThat(d.completedOrders()).isEqualTo(2);assertThat(d.cancelledOrders()).isEqualTo(1);assertThat(d.productCount()).isEqualTo(8);assertThat(d.bestSellers()).hasSize(1);
 }
 @Test void revenueUsesCompletionDateAndGroupsMonths()throws Exception{Instant done=Instant.now(),created=done.minusSeconds(86400*40L);mockOrders(order("a",1,"COMPLETED","COD","PAID",100000,created,done));LocalDate today=LocalDate.now(ReportService.SHOP_ZONE);var r=reports.revenue(today,today,"DAY");assertThat(r.realizedRevenue()).isEqualByComparingTo("100000");assertThat(r.buckets()).hasSize(1);assertThat(r.buckets().get(0).period()).isEqualTo(today.toString());}
 @Test void partialRefundReducesNetRevenueAndBestSellerQuantity()throws Exception{Instant now=Instant.now();var row=(com.fasterxml.jackson.databind.node.ObjectNode)order("partial",1,"COMPLETED","COD","PAID",100000,now,now);
  row.put("returnedAmount",40000);((com.fasterxml.jackson.databind.node.ObjectNode)row.withArray("items").get(0)).put("returnedQuantity",1);mockOrders(row);
  assertThat(reports.dashboard().realizedRevenue()).isEqualByComparingTo("60000");assertThat(reports.dashboard().bestSellers().get(0).quantity()).isEqualTo(1);
  LocalDate today=LocalDate.now(ReportService.SHOP_ZONE);assertThat(reports.revenue(today,today,"DAY").realizedRevenue()).isEqualByComparingTo("60000");
 }
 @Test void bestSellersUsePaidCodNetQuantityOnly()throws Exception{
  Instant now=Instant.now();var cash=(com.fasterxml.jackson.databind.node.ObjectNode)order("cash",1,"COMPLETED","COD","PAID",100000,now,now);
  ((com.fasterxml.jackson.databind.node.ObjectNode)cash.withArray("items").get(0)).put("returnedQuantity",1);
  var simulation=order("simulation",2,"COMPLETED","SIMULATED","SIMULATED_PAID",100000,now,now);
  var cancelled=order("cancelled",3,"CANCELLED","COD","PAID",100000,now,now);
  mockOrders(cash,simulation,cancelled);
  assertThat(reports.bestSellers()).hasSize(1);assertThat(reports.bestSellers().get(0).quantity()).isEqualTo(1);
 }
 @Test void exceptionReportIncludesCancellationFullAndPartialRefundsSeparately()throws Exception{
  Instant now=Instant.now();LocalDate today=LocalDate.now(ReportService.SHOP_ZONE);
  var cancelled=(com.fasterxml.jackson.databind.node.ObjectNode)order("cancelled",1,"CANCELLED","COD","CANCELLED",90000,now,null);
  cancelled.set("events",mapper.readTree("[{\"state\":\"CANCELLED\",\"createdAt\":\""+now+"\"}]"));
  var full=(com.fasterxml.jackson.databind.node.ObjectNode)order("full",2,"RETURNED","COD","REFUNDED",130000,now,now);
  full.set("returnEvents",mapper.readTree("[{\"state\":\"REFUNDED\",\"mode\":\"FULL\",\"refundAmount\":130000,\"createdAt\":\""+now+"\"}]"));
  var partial=(com.fasterxml.jackson.databind.node.ObjectNode)order("partial",3,"COMPLETED","SIMULATED","SIMULATED_PAID",200000,now,now);
  partial.set("returnEvents",mapper.readTree("[{\"state\":\"REFUNDED\",\"mode\":\"PARTIAL\",\"refundAmount\":40000,\"createdAt\":\""+now+"\"}]"));
  mockOrders(cancelled,full,partial);var report=reports.exceptions(today,today);
  assertThat(report.cancelledOrders()).isEqualTo(1);assertThat(report.fullReturns()).isEqualTo(1);assertThat(report.partialReturns()).isEqualTo(1);
  assertThat(report.codRefunds()).isEqualByComparingTo("130000");assertThat(report.simulatedRefunds()).isEqualByComparingTo("40000");
  assertThat(report.rows()).extracting(ReportingDtos.ExceptionRow::kind).containsExactlyInAnyOrder("CANCELLED","FULL_RETURN","PARTIAL_RETURN");
  assertThat(reports.exceptionsCsv(report)).contains("full,FULL_RETURN,COD,130000,130000");
  assertThatThrownBy(()->reports.exceptions(today,today.minusDays(1))).isInstanceOf(ApiException.class);
  String customer=jwt.issue(7,"customer",List.of("CUSTOMER"),false),admin=jwt.issue(1,"admin",List.of("ADMIN"),false);
  mvc.perform(get("/api/reports/exceptions?from="+today+"&to="+today).header("Authorization","Bearer "+customer)).andExpect(status().isForbidden());
  mvc.perform(get("/api/reports/exceptions?from="+today+"&to="+today).header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.partialReturns").value(1));
 }
 @Test void categoryRevenueAllocatesDiscountAndPartialRefundWithoutShippingOrSimulation()throws Exception{
  Instant now=Instant.now();LocalDate today=LocalDate.now(ReportService.SHOP_ZONE);
  var cash=(com.fasterxml.jackson.databind.node.ObjectNode)order("category-cash",1,"COMPLETED","COD","PAID",210000,now,now);
  cash.put("shippingFee",30000);cash.put("returnedAmount",45000);
  cash.set("items",mapper.readTree("[{\"productId\":7,\"categoryId\":11,\"quantity\":2,\"returnedQuantity\":1,\"unitPrice\":50000},{\"productId\":8,\"categoryId\":12,\"quantity\":1,\"returnedQuantity\":0,\"unitPrice\":100000}]"));
  var demo=(com.fasterxml.jackson.databind.node.ObjectNode)order("category-demo",2,"COMPLETED","SIMULATED","SIMULATED_PAID",100000,now,now);
  demo.put("shippingFee",30000);mockOrders(cash,demo);
  when(http.get(8082,"/internal/catalog/report-categories")).thenReturn(mapper.readTree("{\"categories\":[{\"id\":11,\"name\":\"Áo\"},{\"id\":12,\"name\":\"=Phụ kiện\"}],\"products\":[{\"productId\":7,\"categoryId\":12},{\"productId\":8,\"categoryId\":12}]}"));
  var report=reports.categoryRevenue(today,today);
  assertThat(report.merchandiseRevenue()).isEqualByComparingTo("135000");assertThat(report.shippingRevenue()).isEqualByComparingTo("30000");assertThat(report.totalRevenue()).isEqualByComparingTo("165000");
  assertThat(report.categories()).hasSize(2);assertThat(report.categories().get(0).revenue()).isEqualByComparingTo("45000");assertThat(report.categories().get(1).revenue()).isEqualByComparingTo("90000");
  assertThat(report.categories().get(0).categoryId()).isEqualTo(11L);
  assertThat(reports.categoryRevenueCsv(report)).contains("\"'=Phụ kiện\"");
  assertThatThrownBy(()->reports.categoryRevenue(today,today.minusDays(1))).isInstanceOf(ApiException.class);
  String customer=jwt.issue(7,"customer",List.of("CUSTOMER"),false),admin=jwt.issue(1,"admin",List.of("ADMIN"),false);
  mvc.perform(get("/api/reports/categories?from="+today+"&to="+today).header("Authorization","Bearer "+customer)).andExpect(status().isForbidden());
  mvc.perform(get("/api/reports/categories?from="+today+"&to="+today).header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.totalRevenue").value(165000));
 }
 @Test void notificationImportIsIdempotentAndOwnerCannotReadAnother()throws Exception{Instant at=Instant.now();JsonNode row=order("a",7,"COMPLETED","COD","PAID",100000,at,at);notifications.importEvents(7,List.of(row));notifications.importEvents(7,List.of(row));assertThat(notifications.list(7,0).content()).hasSize(1);long id=notifications.list(7,0).content().get(0).id();assertThatThrownBy(()->notifications.read(8,id)).isInstanceOf(ApiException.class);notifications.read(7,id);assertThat(notifications.unread(7)).isZero();}
 @Test void returnedOrderIsExcludedFromRevenueAndNotifiesOwner()throws Exception{
  Instant now=Instant.now();var row=(com.fasterxml.jackson.databind.node.ObjectNode)order("returned",7,"RETURNED","COD","REFUNDED",130000,now.minusSeconds(86400),now.minusSeconds(60));
  ((com.fasterxml.jackson.databind.node.ArrayNode)row.get("events")).add(mapper.createObjectNode().put("state","RETURNED").put("createdAt",now.toString()));
  mockOrders(row);assertThat(reports.dashboard().realizedRevenue()).isEqualByComparingTo("0");
  notifications.importEvents(7,List.of(row));
  assertThat(notifications.list(7,0).content()).extracting("state").contains("RETURNED");
 }
 @Test void partialReturnEventsNotifyOwnerOnceWithoutDuplicatingFullReturn()throws Exception{
  Instant now=Instant.now();var partial=(com.fasterxml.jackson.databind.node.ObjectNode)order("partial-notice",7,"COMPLETED","COD","PAID",230000,now.minusSeconds(3600),now.minusSeconds(1800));
  partial.set("returnEvents",mapper.readTree("[{\"id\":11,\"returnId\":\"return-1\",\"mode\":\"PARTIAL\",\"state\":\"APPROVED\",\"refundAmount\":100000,\"createdAt\":\""+now.minusSeconds(60)+"\"},{\"id\":12,\"returnId\":\"return-1\",\"mode\":\"PARTIAL\",\"state\":\"REFUNDED\",\"refundAmount\":100000,\"createdAt\":\""+now+"\"}]") );
  notifications.importEvents(7,List.of(partial));notifications.importEvents(7,List.of(partial));
  var rows=notifications.list(7,0).content();assertThat(rows).extracting("state").contains("RETURN_APPROVED","RETURN_REFUNDED");
  assertThat(rows.stream().filter(n->n.state().equals("RETURN_REFUNDED")).toList()).hasSize(1);
  assertThat(rows.stream().filter(n->n.state().equals("RETURN_REFUNDED")).findFirst().orElseThrow().body()).contains("100");
  assertThatThrownBy(()->notifications.read(8,rows.get(0).id())).isInstanceOf(ApiException.class);
  var full=(com.fasterxml.jackson.databind.node.ObjectNode)order("full-notice",7,"RETURNED","COD","REFUNDED",130000,now,now);
  full.withArray("events").add(mapper.createObjectNode().put("state","RETURNED").put("createdAt",now.toString()));
  full.set("returnEvents",mapper.readTree("[{\"id\":13,\"returnId\":\"return-2\",\"mode\":\"FULL\",\"state\":\"REFUNDED\",\"refundAmount\":130000,\"createdAt\":\""+now+"\"}]") );
  notifications.importEvents(7,List.of(full));assertThat(notifications.list(7,0).content().stream().filter(n->n.orderId().equals("full-notice")).toList()).extracting("state").contains("RETURNED").doesNotContain("RETURN_REFUNDED");
 }
 @Test void marketingRequiresConsentAndCreatesOneOwnerNotification()throws Exception{
  String token=jwt.issue(7,"marketing-customer",List.of("CUSTOMER"),false);
  mvc.perform(get("/api/notifications").header("Authorization","Bearer "+token)).andExpect(status().isOk());
  assertThat(notificationRows.count()).isZero();
  Instant now=Instant.now();
  when(http.get(8081,"/internal/users/7/marketing-preference")).thenReturn(mapper.readTree("{\"enabled\":true,\"since\":\""+now.minusSeconds(120)+"\"}"));
  var feed=mapper.createObjectNode();feed.put("last",true);feed.set("content",mapper.readTree("[{\"id\":19,\"name\":\"Ưu đãi áo mới\",\"targetType\":\"PRODUCT\",\"targetId\":4,\"active\":true,\"startsAt\":\""+now.minusSeconds(60)+"\",\"endsAt\":\""+now.plusSeconds(3600)+"\",\"createdAt\":\""+now.minusSeconds(60)+"\",\"updatedAt\":\""+now.minusSeconds(60)+"\"}]"));
  when(http.get(eq(8086),startsWith("/internal/promotions/marketing?"))).thenReturn(feed);
  mvc.perform(get("/api/notifications").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("content[0].state").value("PROMOTION")).andExpect(jsonPath("content[0].linkPath").value("/products/4"));
  mvc.perform(get("/api/notifications").header("Authorization","Bearer "+token)).andExpect(status().isOk());
  assertThat(notificationRows.count()).isEqualTo(1);assertThatThrownBy(()->notifications.read(8,notifications.list(7,0).content().get(0).id())).isInstanceOf(ApiException.class);
  when(http.get(8081,"/internal/users/7/marketing-preference")).thenReturn(mapper.readTree("{\"enabled\":false}"));
  mvc.perform(get("/api/notifications").header("Authorization","Bearer "+token)).andExpect(status().isOk());assertThat(notificationRows.count()).isEqualTo(1);
 }
 @Test void reportsRequireAdminAndNotificationsRequireLogin()throws Exception{String customer=jwt.issue(1,"customer",List.of("CUSTOMER"),false);mvc.perform(get("/api/reports/dashboard").header("Authorization","Bearer "+customer)).andExpect(status().isForbidden());mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());mvc.perform(get("/api/notifications").header("Authorization","Bearer "+customer)).andExpect(status().isOk());}
 @Test void downstreamFailureIsNotReportedAsZeroRevenue(){when(http.get(eq(8084),startsWith("/internal/orders/report?"))).thenThrow(new ApiException(503,"Order không phản hồi"));assertThatThrownBy(()->reports.dashboard()).isInstanceOf(ApiException.class).satisfies(e->assertThat(((ApiException)e).status()).isEqualTo(503));}
}
