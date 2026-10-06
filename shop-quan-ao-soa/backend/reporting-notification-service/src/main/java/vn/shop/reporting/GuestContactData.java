package vn.shop.reporting;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

@Entity @Table(name="guest_contacts",uniqueConstraints=@UniqueConstraint(columnNames={"email","request_key"}))
class GuestContact {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(name="request_key",length=100,nullable=false) String requestKey;
 @Column(name="request_hash",length=64,nullable=false) String requestHash;
 @Column(length=120,nullable=false) String fullName;
 @Column(length=190,nullable=false) String email;
 @Column(length=20) String phone;
 @Column(length=160,nullable=false) String subject;
 @Column(columnDefinition="TEXT",nullable=false) String message;
 @Column(length=20,nullable=false) String state="OPEN";
 @Column(length=1000) String staffNote;
 Long actorId;
 @Column(nullable=false) Instant createdAt=Instant.now();
 @Column(nullable=false) Instant updatedAt=Instant.now();
}
interface GuestContactRepository extends JpaRepository<GuestContact,Long> {
 Optional<GuestContact> findByEmailAndRequestKey(String email,String requestKey);
 long countByEmailAndCreatedAtAfter(String email,Instant after);
 @org.springframework.data.jpa.repository.Query("select c from GuestContact c where (:state is null or c.state=:state) and (:q is null or lower(c.subject) like lower(concat('%',:q,'%')) or lower(c.email) like lower(concat('%',:q,'%')))")
 Page<GuestContact> search(String state,String q,Pageable page);
}
