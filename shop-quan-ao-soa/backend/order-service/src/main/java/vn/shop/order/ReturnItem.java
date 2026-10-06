package vn.shop.order;

import jakarta.persistence.*;

@Entity @Table(name="return_items",uniqueConstraints=@UniqueConstraint(columnNames={"return_id","variant_id"}))
class ReturnItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="return_id",nullable=false) public ReturnRequest request;
 @Column(nullable=false) public long variantId;
 @Column(nullable=false) public int quantity;
}
