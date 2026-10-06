package vn.shop.order;

import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.*;

interface ReturnRepository extends JpaRepository<ReturnRequest,String> {
 Optional<ReturnRequest> findByOrderId(String orderId);
 @Query("select r from ReturnRequest r where (:state is null or r.state=:state)")
 Page<ReturnRequest> search(String state,Pageable page);
 @Query("select r.id from ReturnRequest r where r.state in ('RECEIVED','REFUND_CONFIRMING') order by r.updatedAt")
 List<String> pending(Pageable page);
}
