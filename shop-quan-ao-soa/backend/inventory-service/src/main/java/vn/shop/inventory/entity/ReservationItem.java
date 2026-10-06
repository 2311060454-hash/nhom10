package vn.shop.inventory.entity;
import jakarta.persistence.*;
@Entity @Table(name="reservation_items")
public class ReservationItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id",nullable=false) public Reservation reservation;
 @Column(nullable=false) public Long variantId;
 @Column(nullable=false) public int quantity;
}
