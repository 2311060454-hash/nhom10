package vn.shop.catalog.entity;
import jakarta.persistence.*;
@Entity @Table(name="brands")
public class Brand {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,unique=true,length=120) public String name;
    @Column(nullable=false) public boolean active=true;
}
