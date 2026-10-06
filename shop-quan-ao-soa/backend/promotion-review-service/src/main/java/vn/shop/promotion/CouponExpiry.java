package vn.shop.promotion;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.LoggerFactory;
@Component
public class CouponExpiry {
 private final CommercialService service;
 public CouponExpiry(CommercialService s){service=s;}
 @Scheduled(fixedDelay=30000,initialDelay=30000)
 public void run(){for(String id:service.expiredIds())try{service.expire(id);}catch(Exception e){LoggerFactory.getLogger(getClass()).warn("Coupon expiry delayed: {}",e.getClass().getSimpleName());}}
}
