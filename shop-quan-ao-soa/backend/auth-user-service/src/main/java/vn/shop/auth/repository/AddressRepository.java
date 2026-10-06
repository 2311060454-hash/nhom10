package vn.shop.auth.repository;

import vn.shop.auth.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByIdDesc(long userId);
    Optional<Address> findByIdAndUserId(long id, long userId);
}
