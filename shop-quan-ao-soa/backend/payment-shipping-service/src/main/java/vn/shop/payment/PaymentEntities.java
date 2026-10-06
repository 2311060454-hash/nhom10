package vn.shop.payment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="payment_guard") class PaymentGuard { @Id public Long id; }
@Entity @Table(name="payments") class Payment {
 @Id @Column(length=36) public String orderId;
 public long userId;
 @Column(length=20,nullable=false) public String method;
 @Column(length=30,nullable=false) public String state="UNPAID";
 @Column(precision=19,scale=2,nullable=false) public BigDecimal amount;
 @Column(length=190) public String reference;
 public Instant createdAt=Instant.now();public Instant updatedAt=Instant.now();
}
@Entity @Table(name="shipments",uniqueConstraints=@UniqueConstraint(columnNames={"carrier","tracking"})) class Shipment {
 @Id @Column(length=36) public String orderId;
 @Column(length=30,nullable=false) public String state="NEW";
 @Column(length=120,nullable=false) public String recipient;
 @Column(length=20,nullable=false) public String phone;
 @Column(length=500,nullable=false) public String address;
 @Column(length=120) public String carrier;
 @Column(length=120) public String tracking;
 @Column(length=120) public String assignee;
 @Column(precision=19,scale=2,nullable=false) public BigDecimal shippingFee;
 @Column(precision=19,scale=2) public BigDecimal carrierCost;
 public Instant createdAt=Instant.now();public Instant updatedAt=Instant.now();
}
@Entity @Table(name="fulfillment_events") class FulfillmentEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(length=36,nullable=false) public String orderId;
 @Column(length=30,nullable=false) public String action;
 public long actorId;
 @Column(length=500) public String note;
 public Instant createdAt=Instant.now();
}
@Entity @Table(name="payment_commands") class PaymentCommand {
 @Id @Column(length=100) public String id;
 @Column(length=36,nullable=false) public String orderId;
 @Column(length=64,nullable=false) public String requestHash;
 @Lob @Column(columnDefinition="MEDIUMTEXT",nullable=false) public String response;
 public Instant createdAt=Instant.now();
}
@Entity @Table(name="refunds") class Refund {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(length=36,nullable=false,unique=true) public String orderId;
 @Column(precision=19,scale=2,nullable=false) public BigDecimal amount;
 @Column(length=30,nullable=false) public String state="SIMULATED_REFUNDED";
 @Column(length=500,nullable=false) public String reason;
 @Column(length=190) public String reference;
 public Instant createdAt=Instant.now();
 public Instant updatedAt=Instant.now();
}
