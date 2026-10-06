package vn.shop.inventory.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="inventory_transactions")
public class InventoryTransaction {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public Long variantId;
 @Column(nullable=false,unique=true,length=150) public String operationKey;
 @Column(nullable=false,length=64) public String requestHash;
 @Column(nullable=false,length=20) public String type;
 @Column(nullable=false) public int quantityDelta;
 @Column(nullable=false) public int reservedDelta;
 @Column(nullable=false,length=500) public String reason;
 @Column(nullable=false) public Long actorId;
 @Column(nullable=false) public int resultOnHand;
 @Column(nullable=false) public int resultReserved;
 @Column(nullable=false) public int resultMinimum;
 @Column(nullable=false) public Instant createdAt=Instant.now();
}
