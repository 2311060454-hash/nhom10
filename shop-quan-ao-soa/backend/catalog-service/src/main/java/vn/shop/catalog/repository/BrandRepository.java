package vn.shop.catalog.repository;
import vn.shop.catalog.entity.Brand;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface BrandRepository extends JpaRepository<Brand,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select b from Brand b where b.id=:id") Optional<Brand> lock(long id);
}
