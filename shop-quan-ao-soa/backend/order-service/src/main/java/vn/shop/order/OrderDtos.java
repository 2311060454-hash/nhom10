package vn.shop.order;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public final class OrderDtos {
 private OrderDtos(){}
 public record Quantity(@Min(1) @Max(99) int quantity){}
 public record Checkout(@Min(0) long cartRevision,@NotBlank @Size(max=120) String recipient,
  @NotBlank @Pattern(regexp="[0-9+ ()\\-]{9,20}") String phone,@NotBlank @Size(max=500) String address,@Size(max=500) String note,
  @Pattern(regexp="COD|SIMULATED|BANK_TRANSFER") String paymentMethod,@Pattern(regexp="[A-Za-z0-9_-]{4,40}") String couponCode){
   public Checkout(long revision,String recipient,String phone,String address,String note){this(revision,recipient,phone,address,note,null,null);}
   public Checkout(long revision,String recipient,String phone,String address,String note,String method){this(revision,recipient,phone,address,note,method,null);}
  }
 public record Transition(@NotBlank String state){}
 public record Line(long variantId,long productId,Long categoryId,String sku,String productName,String size,String color,int quantity,BigDecimal unitPrice,boolean active,String imageUrl){}
 public record CartView(long revision,List<Line> items,BigDecimal subtotal,BigDecimal shippingFee,BigDecimal total){}
 public record History(String state,long actorId,Instant createdAt){}
 public record OrderView(String id,long userId,String state,String paymentMethod,String paymentState,String shippingState,String pendingCommandId,String recipient,String phone,String address,String note,
  BigDecimal subtotal,BigDecimal discount,String couponCode,BigDecimal shippingFee,BigDecimal total,BigDecimal returnedAmount,Instant createdAt,List<Line> items,List<History> history){}
 public record FulfillmentInput(@NotBlank @Pattern(regexp="SIM_SUCCESS|SIM_FAILURE|BANK_CONFIRM|SHIP|DELIVER|DELIVERY_FAIL|RETRY_SHIP|RETURN_RECEIVED|COLLECT_COD") String action,
  @Size(max=120) String carrier,@Size(max=120) String tracking,@Size(max=120) String assignee,
  @Size(max=190) String reference,@DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal carrierCost,@Size(max=500) String note){}
 public record CommandView(String id,String action,String state,String failure,Instant createdAt){}
}
