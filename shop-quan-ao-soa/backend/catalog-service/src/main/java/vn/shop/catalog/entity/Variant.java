package vn.shop.catalog.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="product_variants")
public class Variant {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id",nullable=false) public Product product;
    @Column(nullable=false,unique=true,length=80) public String sku;
    @Column(nullable=false,length=20) public String size;
    @Column(nullable=false,length=50) public String color;
    @Column(nullable=false,precision=15,scale=2) public BigDecimal costPrice;
    @Column(nullable=false,precision=15,scale=2) public BigDecimal price;
    @Column(precision=15,scale=2) public BigDecimal salePrice;
    @Column(nullable=false) public boolean active;
}
