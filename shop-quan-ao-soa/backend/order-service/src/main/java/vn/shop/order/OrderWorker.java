package vn.shop.order;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import org.slf4j.LoggerFactory;
@Component
public class OrderWorker {
 private final OrderRepository orders;private final OrderSaga saga;
 public OrderWorker(OrderRepository o,OrderSaga s){orders=o;saga=s;}
 @Scheduled(fixedDelayString="${app.saga-delay-ms:5000}",initialDelayString="${app.saga-delay-ms:5000}")
 public void tick(){for(String id:orders.pending(PageRequest.of(0,20)))try{saga.reconcile(id);}catch(Exception e){LoggerFactory.getLogger(getClass()).warn("Order reconciliation delayed: {}",e.getClass().getSimpleName());}}
}
