package vn.shop.catalog.repository;
import vn.shop.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ImageRepository extends JpaRepository<ProductImage,String> {
 List<ProductImage> findByProductIdOrderByCreatedAt(long id);
 long countByProductId(long id);
}
