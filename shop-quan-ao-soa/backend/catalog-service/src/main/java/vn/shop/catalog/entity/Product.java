package vn.shop.catalog.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="products")
public class Product {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,unique=true,length=50) public String code;
    @Column(nullable=false,length=200) public String name;
    @Column(nullable=false) public Long categoryId;
    @Column(nullable=false) public Long brandId;
    @Column(nullable=false,length=10000) public String description;
    @Column(nullable=false,length=120) public String material;
    @Column(nullable=false,length=120) public String style;
    @Column(nullable=false,length=10) public String gender;
    public boolean active;
    public boolean featured;
    @Version public long version;
    @Column(nullable=false) public Instant createdAt=Instant.now();
    @Column(nullable=false) public Instant updatedAt=Instant.now();
    @OneToMany(mappedBy="product",cascade=CascadeType.ALL) @OrderBy("id") public List<Variant> variants=new ArrayList<>();
    @PreUpdate void update() { updatedAt=Instant.now(); }
}
