package vn.shop.promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;
interface GuardRepository extends JpaRepository<PromotionGuard,Long> { @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select g from PromotionGuard g where g.id=1") PromotionGuard lock(); }
interface CouponRepository extends JpaRepository<Coupon,Long> { Optional<Coupon> findByCode(String code); Page<Coupon> findAll(Pageable p); }
interface UsageRepository extends JpaRepository<CouponUsage,String> {
 long countByCouponIdAndStateIn(long couponId,Collection<String> states);
 long countByCouponIdAndUserIdAndStateIn(long couponId,long userId,Collection<String> states);
 List<CouponUsage> findTop100ByStateAndExpiresAtBefore(String state,Instant time);
}
interface PromotionRepository extends JpaRepository<ProductPromotion,Long> {
 @Query("select p from ProductPromotion p where p.active=true and p.startsAt<=:now and p.endsAt>:now and (p.startsAt>=:since or p.createdAt>=:since or p.updatedAt>=:since)")
 Page<ProductPromotion> marketing(Instant since,Instant now,Pageable page);
 @Query("select p from ProductPromotion p where p.active=true and p.startsAt<=:now and p.endsAt>:now and ((p.targetType='PRODUCT' and p.targetId=:productId) or (p.targetType='CATEGORY' and p.targetId=:categoryId))") List<ProductPromotion> active(long productId,long categoryId,Instant now);
}
interface ReviewRepository extends JpaRepository<Review,Long> {
 Optional<Review> findByUserIdAndProductId(long userId,long productId);
 Page<Review> findByProductIdAndStateOrderByCreatedAtDesc(long productId,String state,Pageable p);
 Page<Review> findByUserIdOrderByCreatedAtDesc(long userId,Pageable p);
 @Query("select r from Review r where (:productId is null or r.productId=:productId) and (:stars is null or r.stars=:stars) and (:state is null or r.state=:state)") Page<Review> manage(Long productId,Integer stars,String state,Pageable p);
}
interface WishlistRepository extends JpaRepository<Wishlist,Long> { Optional<Wishlist> findByUserIdAndProductId(long userId,long productId); List<Wishlist> findByUserIdOrderByCreatedAtDesc(long userId); }
