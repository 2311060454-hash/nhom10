package vn.shop.inventory.repository;
import vn.shop.inventory.entity.Reservation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
public interface ReservationRepository extends JpaRepository<Reservation,String> {
 @Query("select r.orderId from Reservation r where r.state='HELD' and r.expiresAt<:now") List<String> expired(Instant now,Pageable pageable);
}
