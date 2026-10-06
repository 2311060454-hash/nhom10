package vn.shop.catalog.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="product_images")
public class ProductImage {
    @Id @Column(length=40) public String id;
    @Column(nullable=false) public Long productId;
    @Column(nullable=false,length=30) public String mediaType;
    @Column(nullable=false) public long byteSize;
    @Column(nullable=false) public Instant createdAt=Instant.now();
}
