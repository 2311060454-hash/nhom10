package vn.shop.catalog.repository;
import vn.shop.catalog.entity.Variant;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VariantRepository extends JpaRepository<Variant,Long> {}
