package vn.shop.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id @Column(length = 64) public String tokenHash;
    @Column(nullable = false) public Long userId;
    @Column(nullable = false) public Instant expiresAt;
    @Column(nullable = false) public boolean used;
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
