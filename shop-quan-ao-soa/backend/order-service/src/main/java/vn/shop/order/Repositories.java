package vn.shop.order;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
interface GuardRepository extends JpaRepository<OrderGuard,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select g from OrderGuard g where g.id=1") OrderGuard lock();
}
interface CartRepository extends JpaRepository<Cart,Long> {}
interface OrderRepository extends JpaRepository<ShopOrder,String> {
 Optional<ShopOrder> findByUserIdAndRequestKey(long userId,String key);
 @Query("select o from ShopOrder o where (:owner is null or o.userId=:owner) and (:state is null or o.state=:state) and (:q is null or lower(o.id) like lower(concat('%',:q,'%')) or lower(o.recipient) like lower(concat('%',:q,'%')) or o.phone like concat('%',:q,'%')) and (:from is null or o.createdAt>=:from) and (:to is null or o.createdAt<:to)") Page<ShopOrder> search(Long owner,String state,String q,java.time.Instant from,java.time.Instant to,Pageable page);
 @Query("select o.id from ShopOrder o where o.pendingCommandId is not null or o.state in ('PROCESSING','CANCEL_PENDING','FAIL_PENDING','AWAITING_PAYMENT') order by o.updatedAt") List<String> pending(Pageable page);
 @Query("select count(i)>0 from ShopOrder o join o.items i where o.userId=:userId and o.state='COMPLETED' and i.productId=:productId") boolean purchased(long userId,long productId);
 @Query("select o from ShopOrder o where o.createdAt>=:from and o.createdAt<:to and (:userId is null or o.userId=:userId)") Page<ShopOrder> report(java.time.Instant from,java.time.Instant to,Long userId,Pageable page);
}
interface FulfillmentRepository extends JpaRepository<FulfillmentCommand,String> {
 Optional<FulfillmentCommand> findByOrderIdAndRequestKey(String orderId,String requestKey);
 List<FulfillmentCommand> findTop20ByOrderIdOrderByCreatedAtDesc(String orderId);
}
