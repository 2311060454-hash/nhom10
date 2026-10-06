package vn.shop.reporting;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import jakarta.validation.constraints.Min;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.shop.common.*;
import vn.shop.reporting.ReportingDtos.*;

@RestController @Validated
public class ReportingController {
 private final ReportService reports;private final NotificationStore notifications;private final MarketingBridge marketing;
 public ReportingController(ReportService reports,NotificationStore notifications,MarketingBridge marketing){this.reports=reports;this.notifications=notifications;this.marketing=marketing;}
 @GetMapping("/api/reports/dashboard") @PreAuthorize("hasRole('ADMIN')") public Dashboard dashboard(){return reports.dashboard();}
 @GetMapping("/internal/reports/best-sellers") public List<ProductSale> bestSellers(){return reports.bestSellers();}
 @GetMapping("/api/reports/revenue") @PreAuthorize("hasRole('ADMIN')") public RevenueReport revenue(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(defaultValue="DAY") String group){return reports.revenue(from,to,group);}
 @GetMapping(value="/api/reports/revenue.csv",produces="text/csv;charset=UTF-8") @PreAuthorize("hasRole('ADMIN')")
 public ResponseEntity<byte[]> csv(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(defaultValue="DAY") String group){
  var bytes=("\uFEFF"+reports.revenueCsv(reports.revenue(from,to,group))).getBytes(StandardCharsets.UTF_8);
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=doanh-thu.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(bytes);
 }
 @GetMapping("/api/reports/exceptions") @PreAuthorize("hasRole('ADMIN')") public ExceptionReport exceptions(@RequestParam LocalDate from,@RequestParam LocalDate to){return reports.exceptions(from,to);}
 @GetMapping(value="/api/reports/exceptions.csv",produces="text/csv;charset=UTF-8") @PreAuthorize("hasRole('ADMIN')")
 public ResponseEntity<byte[]> exceptionsCsv(@RequestParam LocalDate from,@RequestParam LocalDate to){
  var bytes=("\uFEFF"+reports.exceptionsCsv(reports.exceptions(from,to))).getBytes(StandardCharsets.UTF_8);
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=don-huy-hoan.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(bytes);
 }
 @GetMapping("/api/reports/categories") @PreAuthorize("hasRole('ADMIN')") public CategoryRevenueReport categories(@RequestParam LocalDate from,@RequestParam LocalDate to){return reports.categoryRevenue(from,to);}
 @GetMapping(value="/api/reports/categories.csv",produces="text/csv;charset=UTF-8") @PreAuthorize("hasRole('ADMIN')")
 public ResponseEntity<byte[]> categoriesCsv(@RequestParam LocalDate from,@RequestParam LocalDate to){
  var bytes=("\uFEFF"+reports.categoryRevenueCsv(reports.categoryRevenue(from,to))).getBytes(StandardCharsets.UTF_8);
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=doanh-thu-danh-muc.csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(bytes);
 }
 private void sync(long user){notifications.importEvents(user,reports.orders(user));notifications.importPromotions(user,marketing.active(user));}
 @GetMapping("/api/notifications") public PageResult<NotificationView> mine(@RequestParam(defaultValue="0") @Min(0) int page){long user=Caller.id();sync(user);return notifications.list(user,page);}
 @GetMapping("/api/notifications/unread-count") public Map<String,Long> unread(){long user=Caller.id();sync(user);return Map.of("count",notifications.unread(user));}
 @PutMapping("/api/notifications/{id}/read") public NotificationView read(@PathVariable @Min(1) long id){return notifications.read(Caller.id(),id);}
}
