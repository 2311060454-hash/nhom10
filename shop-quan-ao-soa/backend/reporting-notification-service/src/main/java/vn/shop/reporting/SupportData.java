package vn.shop.reporting;

import java.time.Instant;
import java.util.*;
import jakarta.persistence.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

@Entity @Table(name="support_guard") class SupportGuard {@Id Long id;}
@Entity @Table(name="customer_support",uniqueConstraints=@UniqueConstraint(columnNames={"userId","requestKey"}))
class SupportTicket {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false) long userId;
 @Column(nullable=false,length=100) String requestKey;
 @Column(nullable=false,length=64) String requestHash;
 @Column(nullable=false,length=160) String subject;
 @Column(nullable=false,length=30) String state="OPEN";
 @Column(nullable=false) Instant createdAt=Instant.now();
 @Column(nullable=false) Instant updatedAt=Instant.now();
}
@Entity @Table(name="support_messages",uniqueConstraints=@UniqueConstraint(columnNames={"ticketId","requestKey"}))
class SupportMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false) long ticketId;
 @Column(nullable=false) long authorId;
 @Column(nullable=false,length=10) String authorRole;
 @Column(nullable=false,length=100) String requestKey;
 @Column(nullable=false,length=64) String requestHash;
 @Column(nullable=false,columnDefinition="TEXT") String messageText;
 @Column(nullable=false) Instant createdAt=Instant.now();
}
@Entity @Table(name="support_status_history")
class SupportStatusHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false) long ticketId;
 @Column(nullable=false,length=30) String state;
 @Column(nullable=false) long actorId;
 @Column(nullable=false) Instant createdAt=Instant.now();
}
interface SupportGuardRepository extends JpaRepository<SupportGuard,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @org.springframework.data.jpa.repository.Query("select g from SupportGuard g where g.id=1") SupportGuard lock();
}
interface SupportTicketRepository extends JpaRepository<SupportTicket,Long> {
 Optional<SupportTicket> findByUserIdAndRequestKey(long userId,String requestKey);
 @org.springframework.data.jpa.repository.Query("select t from SupportTicket t where (:owner is null or t.userId=:owner) and (:state is null or t.state=:state) and (:q is null or lower(t.subject) like lower(concat('%',:q,'%')))")
 Page<SupportTicket> search(Long owner,String state,String q,Pageable pageable);
}
interface SupportMessageRepository extends JpaRepository<SupportMessage,Long> {
 Optional<SupportMessage> findByTicketIdAndRequestKey(long ticketId,String requestKey);
 List<SupportMessage> findByTicketIdOrderByCreatedAtAscIdAsc(long ticketId);
}
interface SupportHistoryRepository extends JpaRepository<SupportStatusHistory,Long> {
 List<SupportStatusHistory> findByTicketIdOrderByCreatedAtAscIdAsc(long ticketId);
}
