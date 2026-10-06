package vn.shop.inventory.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="partial_return_restocks")
public class PartialReturnRestock {
 @Id @Column(length=36) public String returnId;
 @Column(length=36,nullable=false,unique=true) public String orderId;
 @Column(length=64,nullable=false) public String requestHash;
 @Column(nullable=false) public Instant createdAt=Instant.now();
}
