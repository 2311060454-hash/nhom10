package vn.shop.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, unique = true, length = 190) public String email;
    @Column(nullable = false, length = 120) public String fullName;
    @Column(nullable = false, length = 20) public String phone;
    @Column(nullable = false, length = 100) public String passwordHash;
    @Column(nullable = false) public boolean active = true;
    @Column(nullable = false) public boolean inventoryWrite;
    @Column(nullable = false) public boolean marketingConsent;
    public Instant marketingConsentAt;
    @Column(nullable = false) public Instant createdAt = Instant.now();
    @Column(nullable = false) public Instant updatedAt = Instant.now();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_name", nullable = false, length = 20)
    public Set<String> roles = new HashSet<>();
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
