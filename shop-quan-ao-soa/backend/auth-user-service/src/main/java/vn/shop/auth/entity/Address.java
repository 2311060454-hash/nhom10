package vn.shop.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "addresses")
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long userId;
    @Column(nullable = false, length = 120) public String recipient;
    @Column(nullable = false, length = 20) public String phone;
    @Column(nullable = false, length = 500) public String detail;
    @Column(nullable = false) public boolean defaultAddress;
    @Column(nullable = false) public Instant createdAt = Instant.now();
    @Column(nullable = false) public Instant updatedAt = Instant.now();
}
