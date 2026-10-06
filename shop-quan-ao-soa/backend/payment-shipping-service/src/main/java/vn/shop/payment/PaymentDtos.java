package vn.shop.payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.constraints.*;
public final class PaymentDtos {
 private PaymentDtos(){}
 public record Command(@Min(1) long userId,@Pattern(regexp="COD|SIMULATED") @NotNull String method,
  @NotNull @DecimalMin("0") BigDecimal amount,@NotNull @DecimalMin("0") BigDecimal shippingFee,
  @NotBlank @Size(max=120) String recipient,@NotBlank @Size(max=20) String phone,@NotBlank @Size(max=500) String address,
  @NotBlank String action,@Min(0) long actorId,@Size(max=120) String carrier,@Size(max=120) String tracking,
  @Size(max=120) String assignee,@Size(max=190) String reference,@DecimalMin("0") BigDecimal carrierCost,@Size(max=500) String note,
  @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal refundAmount){
   public Command(long userId,String method,BigDecimal amount,BigDecimal shippingFee,String recipient,String phone,String address,String action,long actorId,String carrier,String tracking,String assignee,String reference,BigDecimal carrierCost,String note){this(userId,method,amount,shippingFee,recipient,phone,address,action,actorId,carrier,tracking,assignee,reference,carrierCost,note,null);}
  }
 public record Event(String action,long actorId,String note,Instant createdAt){}
 public record RefundView(BigDecimal amount,String state,String reason,String reference,Instant createdAt,Instant updatedAt){}
 public record View(String orderId,long userId,String method,String paymentState,BigDecimal amount,String reference,
  String shippingState,String recipient,String phone,String address,String carrier,String tracking,String assignee,
  BigDecimal shippingFee,BigDecimal carrierCost,Instant updatedAt,List<Event> history,List<RefundView> refunds){}
}
