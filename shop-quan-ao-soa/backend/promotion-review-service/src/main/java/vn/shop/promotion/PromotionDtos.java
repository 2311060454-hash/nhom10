package vn.shop.promotion;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public final class PromotionDtos {
 private PromotionDtos(){}
 public record CouponInput(@NotBlank @Pattern(regexp="[A-Z0-9_-]{4,40}") String code,
  @NotBlank @Pattern(regexp="PERCENT|FIXED") String type,@NotNull @DecimalMin("0.01") BigDecimal value,
  @NotNull @DecimalMin("0") BigDecimal minimumTotal,@DecimalMin("0") BigDecimal maximumDiscount,
  @Min(1) int totalLimit,@Min(1) int perCustomerLimit,@NotNull Instant startsAt,@NotNull Instant endsAt,boolean active){}
 public record CouponView(long id,String code,String type,BigDecimal value,BigDecimal minimumTotal,BigDecimal maximumDiscount,
  int totalLimit,int perCustomerLimit,Instant startsAt,Instant endsAt,boolean active,long heldOrUsed){}
 public record CouponQuote(@NotBlank String code,@NotNull @DecimalMin("0") BigDecimal subtotal,long userId){}
 public record QuoteView(String code,BigDecimal discount,BigDecimal remainingTotal){}
 public record CouponReserve(@NotBlank String code,@Min(1) long userId,@NotNull @DecimalMin("0") BigDecimal subtotal){}
 public record UsageView(String orderId,long userId,String code,String state,BigDecimal discount,Instant expiresAt){}
 public record PromotionInput(@NotBlank @Size(max=120) String name,@NotBlank @Pattern(regexp="PRODUCT|CATEGORY") String targetType,
  @Min(1) long targetId,@NotBlank @Pattern(regexp="PERCENT|FIXED") String type,@NotNull @DecimalMin("0.01") BigDecimal value,
  @DecimalMin("0") BigDecimal maximumDiscount,@NotNull Instant startsAt,@NotNull Instant endsAt,boolean active){}
 public record PromotionView(long id,String name,String targetType,long targetId,String type,BigDecimal value,
  BigDecimal maximumDiscount,Instant startsAt,Instant endsAt,boolean active,Instant createdAt,Instant updatedAt){}
 public record PriceQuote(@Min(1) long productId,@Min(1) long categoryId,@NotNull @DecimalMin("0") BigDecimal price){}
 public record PriceView(BigDecimal price,BigDecimal originalPrice,Long promotionId){}
 public record ReviewInput(@Min(1) long productId,@Min(1) @Max(5) int stars,@NotBlank @Size(max=1200) String comment){}
 public record ReviewView(long id,long userId,long productId,int stars,String comment,String state,Instant createdAt,Instant updatedAt){}
 public record Moderate(@NotBlank @Pattern(regexp="APPROVED|HIDDEN") String state){}
 public record WishlistView(long productId,Instant createdAt){}
}
