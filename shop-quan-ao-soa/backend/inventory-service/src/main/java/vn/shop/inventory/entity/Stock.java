package vn.shop.inventory.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="inventory")
public class Stock {
 @Id public Long variantId;
 @Column(nullable=false) public int onHand;
 @Column(nullable=false) public int reserved;
 @Column(nullable=false) public int minimumStock=5;
 @Version public long version;
 @Column(nullable=false) public Instant createdAt=Instant.now();
 @Column(nullable=false) public Instant updatedAt=Instant.now();
 @PreUpdate void update(){updatedAt=Instant.now();}
}
