package vn.shop.order;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="order_guard") class OrderGuard { @Id public Long id; }
@Entity @Table(name="carts") class Cart {
 @Id public Long userId;
 public long revision;
 public Instant updatedAt=Instant.now();
 @OneToMany(cascade=CascadeType.ALL,orphanRemoval=true) @JoinColumn(name="cart_id",nullable=false) @OrderBy("variantId") public List<CartItem> items=new ArrayList<>();
}
@Entity @Table(name="cart_items",uniqueConstraints=@UniqueConstraint(columnNames={"cart_id","variantId"})) class CartItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public long variantId; public int quantity;
}
@Entity @Table(name="orders",uniqueConstraints=@UniqueConstraint(columnNames={"userId","requestKey"})) class ShopOrder {
 @Id @Column(length=36) public String id;
 public long userId;
 @Column(length=100,nullable=false) public String requestKey;
 @Column(length=64,nullable=false) public String requestHash;
 @Column(length=30,nullable=false) public String state="PROCESSING";
 @Column(length=20,nullable=false) public String paymentMethod="COD";
 @Column(length=30,nullable=false) public String paymentState="UNPAID";
 @Column(length=30,nullable=false) public String shippingState="NEW";
 @Column(length=36) public String pendingCommandId;
 @Column(length=120,nullable=false) public String recipient;
 @Column(length=20,nullable=false) public String phone;
 @Column(length=500,nullable=false) public String address;
 @Column(length=500) public String note;
 @Column(precision=19,scale=2,nullable=false) public BigDecimal subtotal;
 @Column(precision=19,scale=2,nullable=false) public BigDecimal shippingFee;
 @Column(precision=19,scale=2,nullable=false) public BigDecimal total;
 @Column(precision=19,scale=2,nullable=false) public BigDecimal returnedAmount=BigDecimal.ZERO;
 @Column(length=40) public String couponCode;
 @Column(precision=19,scale=2,nullable=false) public BigDecimal discount=BigDecimal.ZERO;
 public Instant createdAt=Instant.now(); public Instant updatedAt=Instant.now();
 @OneToMany(cascade=CascadeType.ALL,orphanRemoval=true) @JoinColumn(name="order_id",nullable=false) @OrderBy("id") public List<OrderItem> items=new ArrayList<>();
 @OneToMany(cascade=CascadeType.ALL,orphanRemoval=true) @JoinColumn(name="order_id",nullable=false) @OrderBy("id") public List<OrderHistory> history=new ArrayList<>();
}
@Entity @Table(name="fulfillment_commands",uniqueConstraints=@UniqueConstraint(columnNames={"orderId","requestKey"})) class FulfillmentCommand {
 @Id @Column(length=36) public String id;
 @Column(length=36,nullable=false) public String orderId;
 @Column(length=100,nullable=false) public String requestKey;
 @Column(length=64,nullable=false) public String requestHash;
 @Column(length=30,nullable=false) public String action;
 @Column(length=20,nullable=false) public String state="PENDING";
 public long actorId;
 @Lob @Column(columnDefinition="TEXT",nullable=false) public String payload;
 @Column(length=500) public String failure;
 public Instant createdAt=Instant.now();public Instant updatedAt=Instant.now();
}
@Entity @Table(name="order_items") class OrderItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public long variantId; public long productId;
 public Long categoryId;
 @Column(length=100,nullable=false) public String sku;
 @Column(length=200,nullable=false) public String productName;
 @Column(length=50,nullable=false) public String size;
 @Column(length=50,nullable=false) public String color;
 public int quantity;
 @Column(precision=19,scale=2,nullable=false) public BigDecimal unitPrice;
}
@Entity @Table(name="order_status_history") class OrderHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(length=30,nullable=false) public String state;
 public long actorId; public Instant createdAt=Instant.now();
}
