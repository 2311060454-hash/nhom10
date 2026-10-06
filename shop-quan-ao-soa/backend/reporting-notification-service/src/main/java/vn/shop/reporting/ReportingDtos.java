package vn.shop.reporting;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ReportingDtos {
 private ReportingDtos(){}
 public record OrderShort(String id,String state,BigDecimal total,Instant createdAt){}
 public record ProductSale(long productId,String productName,long quantity,BigDecimal revenue){}
 public record Dashboard(BigDecimal realizedRevenue,BigDecimal simulatedTurnover,BigDecimal todayRevenue,BigDecimal weekRevenue,
  BigDecimal monthRevenue,BigDecimal yearRevenue,long totalOrders,long pendingOrders,long shippingOrders,long completedOrders,
  long cancelledOrders,long productCount,long customerCount,long lowStockCount,List<ProductSale> bestSellers,List<OrderShort> recentOrders){}
 public record Bucket(String period,BigDecimal realizedRevenue,BigDecimal simulatedTurnover,long completedOrders){}
 public record RevenueReport(String from,String to,String group,BigDecimal realizedRevenue,BigDecimal simulatedTurnover,long completedOrders,List<Bucket> buckets){}
 public record ExceptionRow(String orderId,String kind,String paymentMethod,BigDecimal orderTotal,BigDecimal refundAmount,Instant occurredAt){}
 public record ExceptionReport(String from,String to,long cancelledOrders,long fullReturns,long partialReturns,BigDecimal codRefunds,BigDecimal simulatedRefunds,List<ExceptionRow> rows){}
 public record CategoryRevenueRow(long categoryId,String categoryName,long quantity,long orderCount,BigDecimal revenue){}
 public record CategoryRevenueReport(String from,String to,BigDecimal merchandiseRevenue,BigDecimal shippingRevenue,BigDecimal totalRevenue,List<CategoryRevenueRow> categories){}
 public record NotificationView(long id,String orderId,String linkPath,String state,String title,String body,Instant createdAt,Instant readAt){}
}
