package vn.shop.order;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name="return_requests")
class ReturnRequest {
 @Id @Column(length=36) public String id;
 @Column(length=36,nullable=false,unique=true) public String orderId;
 public long userId;
 @Column(length=100,nullable=false) public String requestKey;
 @Column(length=500,nullable=false) public String reason;
 @Column(length=10,nullable=false) public String mode="FULL";
 @Column(precision=19,scale=2,nullable=false) public BigDecimal refundAmount=BigDecimal.ZERO;
 @OneToMany(mappedBy="request",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("variantId") public List<ReturnItem> items=new ArrayList<>();
 @Column(length=30,nullable=false) public String state="REQUESTED";
 @Column(length=500) public String decisionNote;
 @Column(length=190) public String refundReference;
 public Long actorId;
 public Instant createdAt=Instant.now();
 public Instant updatedAt=Instant.now();
 @Column(length=500) public String lastError;
}
