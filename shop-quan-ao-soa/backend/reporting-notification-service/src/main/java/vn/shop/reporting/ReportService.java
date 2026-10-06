package vn.shop.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;
import org.springframework.stereotype.Service;
import vn.shop.common.*;
import vn.shop.reporting.ReportingDtos.*;

@Service
public class ReportService {
 static final ZoneId SHOP_ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
 private static final Set<String> PAID_COD=Set.of("PAID");
 private final InternalHttp http;
 public ReportService(InternalHttp http){this.http=http;}
 public List<JsonNode> orders(Long userId){
  var result=new ArrayList<JsonNode>();String from="1970-01-01T00:00:00Z",to=Instant.now().plusSeconds(86400).toString();
  for(int page=0;page<100;page++){
   String path="/internal/orders/report?from="+enc(from)+"&to="+enc(to)+"&size=100&page="+page+(userId==null?"":"&userId="+userId);
   JsonNode data=http.get(8084,path);for(JsonNode row:data.path("content"))result.add(row);
   if(data.path("last").asBoolean())return result;
  }
  throw new ApiException(503,"Báo cáo vượt 10.000 đơn; cần thu hẹp hoặc mở rộng xử lý phân trang");
 }
 private static String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
 private static BigDecimal amount(JsonNode order){return order.path("total").decimalValue().subtract(order.path("returnedAmount").decimalValue());}
 private static Instant instant(JsonNode node){return Instant.parse(node.asText());}
 private static Instant completedAt(JsonNode order){for(JsonNode e:order.path("events"))if(e.path("state").asText().equals("COMPLETED"))return instant(e.path("createdAt"));return null;}
 static boolean realized(JsonNode order){return order.path("state").asText().equals("COMPLETED")&&order.path("paymentMethod").asText().equals("COD")&&PAID_COD.contains(order.path("paymentState").asText());}
 static boolean simulated(JsonNode order){return order.path("state").asText().equals("COMPLETED")&&order.path("paymentMethod").asText().equals("SIMULATED")&&order.path("paymentState").asText().equals("SIMULATED_PAID");}
 private static boolean between(LocalDate day,LocalDate from,LocalDate to){return !day.isBefore(from)&&!day.isAfter(to);}
 private static String period(LocalDate day,String group){return switch(group){case "DAY"->day.toString();case "MONTH"->day.toString().substring(0,7);case "QUARTER"->day.getYear()+"-Q"+((day.getMonthValue()-1)/3+1);case "YEAR"->String.valueOf(day.getYear());default->throw new ApiException(400,"Kiểu nhóm báo cáo không hợp lệ");};}
 private record Totals(BigDecimal real,BigDecimal sim,long count){Totals add(BigDecimal r,BigDecimal s){return new Totals(real.add(r),sim.add(s),count+1);}}
 public RevenueReport revenue(LocalDate from,LocalDate to,String group){
  if(from==null||to==null||from.isAfter(to)||to.isAfter(LocalDate.now(SHOP_ZONE).plusDays(1)))throw new ApiException(400,"Khoảng ngày không hợp lệ");
  if(!Set.of("DAY","MONTH","QUARTER","YEAR").contains(group))throw new ApiException(400,"Kiểu nhóm báo cáo không hợp lệ");
  var buckets=new TreeMap<String,Totals>();BigDecimal real=BigDecimal.ZERO,sim=BigDecimal.ZERO;long count=0;
  for(JsonNode o:orders(null)){
   boolean cash=realized(o),demo=simulated(o);if(!cash&&!demo)continue;
   Instant done=completedAt(o);if(done==null)continue;LocalDate day=done.atZone(SHOP_ZONE).toLocalDate();if(!between(day,from,to))continue;
   BigDecimal r=cash?amount(o):BigDecimal.ZERO,s=demo?amount(o):BigDecimal.ZERO;real=real.add(r);sim=sim.add(s);count++;
   buckets.merge(period(day,group),new Totals(r,s,1),(a,b)->new Totals(a.real.add(b.real),a.sim.add(b.sim),a.count+b.count));
  }
  return new RevenueReport(from.toString(),to.toString(),group,real,sim,count,buckets.entrySet().stream().map(e->new Bucket(e.getKey(),e.getValue().real,e.getValue().sim,e.getValue().count)).toList());
 }
 public Dashboard dashboard(){
  var all=orders(null);LocalDate today=LocalDate.now(SHOP_ZONE),week=today.with(WeekFields.ISO.dayOfWeek(),1),month=today.withDayOfMonth(1),year=today.withDayOfYear(1);
  BigDecimal real=BigDecimal.ZERO,sim=BigDecimal.ZERO,d=BigDecimal.ZERO,w=BigDecimal.ZERO,m=BigDecimal.ZERO,y=BigDecimal.ZERO;
  long pending=0,shipping=0,completed=0,cancelled=0;Map<Long,ProductSale> sellers=new HashMap<>();
  for(JsonNode o:all){
   String state=o.path("state").asText();if(Set.of("PROCESSING","PLACED","AWAITING_PAYMENT").contains(state))pending++;if(state.equals("SHIPPED"))shipping++;if(state.equals("COMPLETED"))completed++;if(state.equals("CANCELLED"))cancelled++;
   boolean cash=realized(o),demo=simulated(o);if(!cash&&!demo)continue;
   Instant done=completedAt(o);if(done==null)continue;
   if(demo){sim=sim.add(amount(o));continue;}
   BigDecimal paid=amount(o);real=real.add(paid);LocalDate date=done.atZone(SHOP_ZONE).toLocalDate();
   if(date.equals(today))d=d.add(paid);if(!date.isBefore(week))w=w.add(paid);if(!date.isBefore(month))m=m.add(paid);if(!date.isBefore(year))y=y.add(paid);
   for(JsonNode item:o.path("items")){long id=item.path("productId").asLong();int qty=item.path("quantity").asInt()-item.path("returnedQuantity").asInt();BigDecimal line=item.path("unitPrice").decimalValue().multiply(BigDecimal.valueOf(qty));
    sellers.merge(id,new ProductSale(id,item.path("productName").asText(),qty,line),(a,b)->new ProductSale(id,a.productName(),a.quantity()+b.quantity(),a.revenue().add(b.revenue())));
   }
  }
  long products=http.get(8082,"/internal/catalog/report-summary").path("productCount").asLong();
  long customers=http.get(8081,"/internal/users/report-summary").path("customerCount").asLong();
  long low=http.get(8083,"/internal/inventory/report-summary").path("lowStockCount").asLong();
  List<ProductSale> top=sellers.values().stream().sorted(Comparator.comparingLong(ProductSale::quantity).reversed()).limit(10).toList();
  List<OrderShort> recent=all.stream().sorted(Comparator.comparing((JsonNode o)->o.path("createdAt").asText()).reversed()).limit(10)
   .map(o->new OrderShort(o.path("id").asText(),o.path("state").asText(),amount(o),instant(o.path("createdAt")))).toList();
  return new Dashboard(real,sim,d,w,m,y,all.size(),pending,shipping,completed,cancelled,products,customers,low,top,recent);
 }
 public List<ProductSale> bestSellers(){
  Map<Long,ProductSale> sales=new HashMap<>();
  for(JsonNode order:orders(null)){
   if(!realized(order)||completedAt(order)==null)continue;
   for(JsonNode item:order.path("items")){
    int quantity=item.path("quantity").asInt()-item.path("returnedQuantity").asInt();
    if(quantity<=0)continue;
    long id=item.path("productId").asLong();
    BigDecimal revenue=item.path("unitPrice").decimalValue().multiply(BigDecimal.valueOf(quantity));
    sales.merge(id,new ProductSale(id,item.path("productName").asText(),quantity,revenue),
     (a,b)->new ProductSale(id,a.productName(),a.quantity()+b.quantity(),a.revenue().add(b.revenue())));
   }
  }
  return sales.values().stream().sorted(Comparator.comparingLong(ProductSale::quantity).reversed().thenComparingLong(ProductSale::productId)).toList();
 }
 public String revenueCsv(RevenueReport report){StringBuilder b=new StringBuilder("ky,doanh_thu_cod_vnd,gia_tri_mo_phong_vnd,don_hoan_tat\r\n");
  for(Bucket row:report.buckets())b.append(row.period()).append(',').append(row.realizedRevenue().toPlainString()).append(',').append(row.simulatedTurnover().toPlainString()).append(',').append(row.completedOrders()).append("\r\n");return b.toString();}
 public ExceptionReport exceptions(LocalDate from,LocalDate to){
  if(from==null||to==null||from.isAfter(to)||to.isAfter(LocalDate.now(SHOP_ZONE).plusDays(1)))throw new ApiException(400,"Khoảng ngày không hợp lệ");
  List<ExceptionRow> rows=new ArrayList<>();long cancelled=0,full=0,partial=0;BigDecimal cod=BigDecimal.ZERO,demo=BigDecimal.ZERO;
  for(JsonNode order:orders(null)){
   String id=order.path("id").asText(),method=order.path("paymentMethod").asText();BigDecimal total=order.path("total").decimalValue();
   if(order.path("state").asText().equals("CANCELLED"))for(JsonNode event:order.path("events"))if(event.path("state").asText().equals("CANCELLED")){
    Instant at=instant(event.path("createdAt"));if(between(at.atZone(SHOP_ZONE).toLocalDate(),from,to)){rows.add(new ExceptionRow(id,"CANCELLED",method,total,BigDecimal.ZERO,at));cancelled++;}break;
   }
   for(JsonNode event:order.path("returnEvents"))if(event.path("state").asText().equals("REFUNDED")){
    Instant at=instant(event.path("createdAt"));if(!between(at.atZone(SHOP_ZONE).toLocalDate(),from,to))continue;
    String kind=event.path("mode").asText().equals("PARTIAL")?"PARTIAL_RETURN":"FULL_RETURN";
    BigDecimal refund=event.path("refundAmount").decimalValue();rows.add(new ExceptionRow(id,kind,method,total,refund,at));
    if(kind.equals("PARTIAL_RETURN"))partial++;else full++;
    if(method.equals("COD"))cod=cod.add(refund);else if(method.equals("SIMULATED"))demo=demo.add(refund);
   }
  }
  rows.sort(Comparator.comparing(ExceptionRow::occurredAt).reversed().thenComparing(ExceptionRow::orderId));
  return new ExceptionReport(from.toString(),to.toString(),cancelled,full,partial,cod,demo,rows);
 }
 public String exceptionsCsv(ExceptionReport report){StringBuilder csv=new StringBuilder("ma_don,loai,phuong_thuc,gia_tri_don_vnd,tien_hoan_vnd,thoi_diem_utc\r\n");
  for(ExceptionRow row:report.rows())csv.append(row.orderId()).append(',').append(row.kind()).append(',').append(row.paymentMethod()).append(',').append(row.orderTotal().toPlainString()).append(',').append(row.refundAmount().toPlainString()).append(',').append(row.occurredAt()).append("\r\n");return csv.toString();}
 private static class CategorySum {long quantity,orders;BigDecimal revenue=BigDecimal.ZERO;}
 public CategoryRevenueReport categoryRevenue(LocalDate from,LocalDate to){
  if(from==null||to==null||from.isAfter(to)||to.isAfter(LocalDate.now(SHOP_ZONE).plusDays(1)))throw new ApiException(400,"Khoảng ngày không hợp lệ");
  JsonNode catalog=http.get(8082,"/internal/catalog/report-categories");Map<Long,Long> productCategories=new HashMap<>();Map<Long,String> names=new HashMap<>();
  for(JsonNode row:catalog.path("products"))productCategories.put(row.path("productId").asLong(),row.path("categoryId").asLong());
  for(JsonNode row:catalog.path("categories"))names.put(row.path("id").asLong(),row.path("name").asText());
  Map<Long,CategorySum> totals=new TreeMap<>();BigDecimal merchandise=BigDecimal.ZERO,shipping=BigDecimal.ZERO;
  for(JsonNode order:orders(null)){
   if(!realized(order))continue;Instant done=completedAt(order);if(done==null||!between(done.atZone(SHOP_ZONE).toLocalDate(),from,to))continue;
   Map<Long,BigDecimal> weights=new TreeMap<>();Map<Long,Long> quantities=new HashMap<>();
   for(JsonNode item:order.path("items")){
    long category=item.path("categoryId").isNumber()&&item.path("categoryId").asLong()>0?item.path("categoryId").asLong():productCategories.getOrDefault(item.path("productId").asLong(),0L);
    int quantity=item.path("quantity").asInt()-item.path("returnedQuantity").asInt();if(quantity<=0)continue;
    BigDecimal gross=item.path("unitPrice").decimalValue().multiply(BigDecimal.valueOf(quantity));
    weights.merge(category,gross,BigDecimal::add);quantities.merge(category,(long)quantity,Long::sum);
   }
   BigDecimal freight=order.path("shippingFee").decimalValue(),net=amount(order).subtract(freight);
   if(net.signum()<0)throw new ApiException(503,"Đơn có doanh thu hàng hóa âm");
   BigDecimal weightTotal=weights.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add);
   if(weightTotal.signum()==0){if(net.signum()>0)throw new ApiException(503,"Đơn không có sản phẩm còn lại để phân bổ doanh thu");shipping=shipping.add(freight);continue;}
   BigDecimal allocated=BigDecimal.ZERO;int index=0;
   for(var entry:weights.entrySet()){
    index++;BigDecimal share=index==weights.size()?net.subtract(allocated):net.multiply(entry.getValue()).divide(weightTotal,2,RoundingMode.HALF_UP);
    allocated=allocated.add(share);CategorySum sum=totals.computeIfAbsent(entry.getKey(),ignored->new CategorySum());
    sum.revenue=sum.revenue.add(share);sum.quantity+=quantities.get(entry.getKey());sum.orders++;
   }
   merchandise=merchandise.add(net);shipping=shipping.add(freight);
  }
  List<CategoryRevenueRow> rows=totals.entrySet().stream().map(e->new CategoryRevenueRow(e.getKey(),names.getOrDefault(e.getKey(),"Chưa phân loại"),e.getValue().quantity,e.getValue().orders,e.getValue().revenue)).toList();
  return new CategoryRevenueReport(from.toString(),to.toString(),merchandise,shipping,merchandise.add(shipping),rows);
 }
 private static String csvCell(String value){String clean=value==null?"":value.replace("\r"," ").replace("\n"," ");if(clean.matches("^\\s*[=+@-].*"))clean="'"+clean;return "\""+clean.replace("\"","\"\"")+"\"";}
 public String categoryRevenueCsv(CategoryRevenueReport report){StringBuilder csv=new StringBuilder("ma_danh_muc,ten_danh_muc,so_san_pham,so_don,doanh_thu_hang_hoa_vnd\r\n");
  for(CategoryRevenueRow row:report.categories())csv.append(row.categoryId()).append(',').append(csvCell(row.categoryName())).append(',').append(row.quantity()).append(',').append(row.orderCount()).append(',').append(row.revenue().toPlainString()).append("\r\n");return csv.toString();}
}
