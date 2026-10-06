package vn.shop.catalog.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="store_content")
public class StoreContent {
 @Id @Column(length=40) public String contentKey;
 @Lob @Column(nullable=false,columnDefinition="TEXT") public String contentText;
 @Column(nullable=false) public Instant updatedAt=Instant.now();
}
