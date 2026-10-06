package vn.shop.payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.List;
interface GuardRepository extends JpaRepository<PaymentGuard,Long> { @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select g from PaymentGuard g where g.id=1") PaymentGuard lock(); }
interface PaymentRepository extends JpaRepository<Payment,String> {}
interface ShipmentRepository extends JpaRepository<Shipment,String> { boolean existsByCarrierAndTrackingAndOrderIdNot(String carrier,String tracking,String orderId); }
interface CommandRepository extends JpaRepository<PaymentCommand,String> {}
interface EventRepository extends JpaRepository<FulfillmentEvent,Long> { List<FulfillmentEvent> findByOrderIdOrderByIdAsc(String orderId); }
interface RefundRepository extends JpaRepository<Refund,Long> { List<Refund> findByOrderId(String orderId); }
