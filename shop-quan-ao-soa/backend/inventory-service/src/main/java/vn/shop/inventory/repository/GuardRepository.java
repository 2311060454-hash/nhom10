package vn.shop.inventory.repository;
import vn.shop.inventory.entity.InventoryGuard;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface GuardRepository extends JpaRepository<InventoryGuard,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select g from InventoryGuard g where g.id=1") InventoryGuard lock();
}
