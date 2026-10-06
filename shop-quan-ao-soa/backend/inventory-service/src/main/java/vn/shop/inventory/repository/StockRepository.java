package vn.shop.inventory.repository;
import vn.shop.inventory.entity.Stock;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
public interface StockRepository extends JpaRepository<Stock,Long> {
 @Query("select s from Stock s where (:variantId is null or s.variantId=:variantId) and (:low=false or s.onHand-s.reserved<=s.minimumStock)") Page<Stock> search(Long variantId,boolean low,Pageable pageable);
}
