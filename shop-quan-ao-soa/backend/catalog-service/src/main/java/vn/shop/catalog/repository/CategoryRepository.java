package vn.shop.catalog.repository;
import vn.shop.catalog.entity.Category;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.List;
public interface CategoryRepository extends JpaRepository<Category,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Category c order by c.id") List<Category> lockAll();
 boolean existsByParentIdAndActiveTrue(long id);
}
