package vn.shop.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "auth_sessions")
public class AuthSession {
    @Id @Column(length = 36) public String id;
    @Column(nullable = false) public Long userId;
    @Column(nullable = false) public Instant expiresAt;
    @Column(nullable = false) public boolean revoked;
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
