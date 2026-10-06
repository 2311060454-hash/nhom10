package vn.shop.inventory.repository;
import vn.shop.inventory.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.Optional;
public interface TransactionRepository extends JpaRepository<InventoryTransaction,Long> {
 Optional<InventoryTransaction> findByOperationKey(String key);
 @Query("select t from InventoryTransaction t where (:variantId is null or t.variantId=:variantId)") Page<InventoryTransaction> history(Long variantId,Pageable pageable);
}
