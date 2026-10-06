package vn.shop.auth.repository;

import vn.shop.auth.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from User u where u.email=:email")
    Optional<User> lockByEmail(@Param("email") String email);
    boolean existsByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from User u where u.id=:id")
    Optional<User> lock(@Param("id") long id);
    @Query("select distinct u from User u join u.roles r where (:role='' or r=:role) and (lower(u.fullName) like lower(concat('%',:q,'%')) or u.email like concat('%',:q,'%') or u.phone like concat('%',:q,'%'))")
    Page<User> search(@Param("q") String q, @Param("role") String role, Pageable pageable);
}
