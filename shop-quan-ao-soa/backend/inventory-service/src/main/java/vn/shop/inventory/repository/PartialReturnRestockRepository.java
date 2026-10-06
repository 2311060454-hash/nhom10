package vn.shop.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.shop.inventory.entity.PartialReturnRestock;

public interface PartialReturnRestockRepository extends JpaRepository<PartialReturnRestock,String> {
 boolean existsByOrderId(String orderId);
}
