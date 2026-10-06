package vn.shop.auth.repository;

import vn.shop.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ResetRepository extends JpaRepository<PasswordResetToken, String> {
    @Query("select t.userId from PasswordResetToken t where t.tokenHash=:hash")
    Optional<Long> owner(@Param("hash") String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from PasswordResetToken t where t.tokenHash=:hash")
    Optional<PasswordResetToken> lock(@Param("hash") String hash);
    @Modifying @Query("update PasswordResetToken t set t.used=true where t.userId=:id")
    void invalidate(@Param("id") long id);
}
