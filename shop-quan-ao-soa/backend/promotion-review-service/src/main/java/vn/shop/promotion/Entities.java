package vn.shop.promotion;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="promotion_guard") class PromotionGuard { @Id public Long id; }
@Entity @Table(name="coupons") class Coupon {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(length=40,nullable=false,unique=true) public String code;
 @Column(length=20,nullable=false) public String type;
 @Column(name="discount_value",precision=19,scale=2,nullable=false) public BigDecimal value;
 @Column(precision=19,scale=2,nullable=false) public BigDecimal minimumTotal;
 @Column(precision=19,scale=2) public BigDecimal maximumDiscount;
 public int totalLimit;public int perCustomerLimit;
 public boolean active=true;
 public Instant startsAt;public Instant endsAt;
 public Instant createdAt=Instant.now();public Instant updatedAt=Instant.now();
}
@Entity @Table(name="coupon_usages") class CouponUsage {
 @Id @Column(length=36) public String orderId;
 public long couponId;public long userId;
 @Column(length=20,nullable=false) public String state="HELD";
 @Column(precision=19,scale=2,nullable=false) public BigDecimal discount;
 @Column(precision=19,scale=2,nullable=false) public BigDecimal originalSubtotal;
 public Instant expiresAt;public Instant createdAt=Instant.now();public Instant updatedAt=Instant.now();
}
@Entity @Table(name="promotions") class ProductPromotion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(length=120,nullable=false) public String name;
 @Column(length=20,nullable=false) public String targetType;
 public long targetId;
 @Column(length=20,nullable=false) public String type;
 @Column(name="discount_value",precision=19,scale=2,nullable=false) public BigDecimal value;
 @Column(precision=19,scale=2) public BigDecimal maximumDiscount;
 public boolean active=true;public Instant startsAt;public Instant endsAt;
 public Instant createdAt=Instant.now();public Instant updatedAt=Instant.now();
}
@Entity @Table(name="reviews",uniqueConstraints=@UniqueConstraint(columnNames={"userId","productId"})) class Review {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public long userId;public long productId;
 public int stars;
 @Column(length=1200,nullable=false) public String comment;
 @Column(length=20,nullable=false) public String state="PENDING";
 public Instant createdAt=Instant.now();public Instant updatedAt=Instant.now();
}
@Entity @Table(name="wishlists",uniqueConstraints=@UniqueConstraint(columnNames={"userId","productId"})) class Wishlist {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public long userId;public long productId;public Instant createdAt=Instant.now();
}


