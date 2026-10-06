package vn.shop.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReturnEventRepository extends JpaRepository<ReturnEvent,Long> {
 List<ReturnEvent> findByReturnIdOrderByIdAsc(String returnId);
}
