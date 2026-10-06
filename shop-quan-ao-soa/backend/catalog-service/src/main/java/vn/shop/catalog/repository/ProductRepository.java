package vn.shop.catalog.repository;
import vn.shop.catalog.entity.Product;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product,Long>, JpaSpecificationExecutor<Product> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Product p where p.id=:id") Optional<Product> lock(long id);
 boolean existsByCategoryIdAndActiveTrue(long id);
 boolean existsByBrandIdAndActiveTrue(long id);
}
