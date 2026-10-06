package vn.shop.order;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="return_status_history")
class ReturnEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(length=36,nullable=false) public String returnId;
 @Column(length=30,nullable=false) public String state;
 public long actorId;
 @Column(length=500) public String note;
 public Instant createdAt=Instant.now();
}
