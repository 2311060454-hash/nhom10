package vn.shop.inventory.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="reservations")
public class Reservation {
 @Id @Column(length=36) public String orderId;
 @Column(nullable=false,length=64) public String requestHash;
 @Column(nullable=false,length=20) public String state;
 @Column(nullable=false) public Instant expiresAt;
 @Column(nullable=false) public Instant createdAt=Instant.now();
 @Column(nullable=false) public Instant updatedAt=Instant.now();
 @OneToMany(mappedBy="reservation",cascade=CascadeType.ALL) @OrderBy("variantId") public List<ReservationItem> items=new ArrayList<>();
 @PreUpdate void update(){updatedAt=Instant.now();}
}
