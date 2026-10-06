package vn.shop.auth.repository;

import vn.shop.auth.entity.AuthSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<AuthSession, String> {
    @Modifying @Query("update AuthSession s set s.revoked=true where s.userId=:userId")
    void revokeAll(@Param("userId") long userId);
}
