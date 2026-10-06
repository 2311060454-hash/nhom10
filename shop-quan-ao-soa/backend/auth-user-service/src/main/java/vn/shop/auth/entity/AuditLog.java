package vn.shop.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long actorId;
    @Column(nullable = false, length = 80) public String action;
    public Long targetId;
    @Column(nullable = false) public Instant createdAt = Instant.now();
}
