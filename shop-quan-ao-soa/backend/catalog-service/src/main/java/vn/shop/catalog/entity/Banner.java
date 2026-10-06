package vn.shop.catalog.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="banners")
public class Banner {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=160) public String title;
 @Column(length=320) public String subtitle;
 @Column(nullable=false,length=120) public String linkPath="/";
 @Column(length=40,unique=true) public String imageId;
 @Column(nullable=false) public int sortOrder;
 @Column(nullable=false) public boolean active;
 @Column(nullable=false) public Instant startsAt;
 @Column(nullable=false) public Instant endsAt;
 @Column(nullable=false) public Instant createdAt=Instant.now();
 @Column(nullable=false) public Instant updatedAt=Instant.now();
}
