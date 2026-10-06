package vn.shop.catalog.entity;
import jakarta.persistence.*;
@Entity @Table(name="categories")
public class Category {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,unique=true,length=120) public String name;
    public Long parentId;
    @Column(nullable=false) public boolean active=true;
}
