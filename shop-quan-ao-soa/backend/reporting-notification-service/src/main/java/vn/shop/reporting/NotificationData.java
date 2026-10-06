package vn.shop.reporting;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import vn.shop.common.PageResult;

@Entity @Table(name="notification_guard") class NotificationGuard {@Id Long id;}
@Entity @Table(name="notifications",uniqueConstraints=@UniqueConstraint(columnNames="eventKey"))
class Notification {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false) long userId;
 @Column(nullable=false,length=180) String eventKey;
 @Column(length=36) String orderId;
 @Column(length=200) String linkPath;
 @Column(nullable=false,length=30) String state;
 @Column(nullable=false,length=160) String title;
 @Column(nullable=false,length=500) String body;
 @Column(nullable=false) Instant createdAt;
 Instant readAt;
}
interface NotificationGuardRepository extends JpaRepository<NotificationGuard,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select g from NotificationGuard g where g.id=1") NotificationGuard lock();
}
interface NotificationRepository extends JpaRepository<Notification,Long> {
 boolean existsByEventKey(String eventKey);
 Page<Notification> findByUserIdOrderByCreatedAtDescIdDesc(long userId,Pageable pageable);
 long countByUserIdAndReadAtIsNull(long userId);
}
