package vn.shop.order;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import org.slf4j.LoggerFactory;

@Component
public class ReturnWorker {
 private final ReturnRepository returns;private final ReturnService service;
 public ReturnWorker(ReturnRepository returns,ReturnService service){this.returns=returns;this.service=service;}
 @Scheduled(fixedDelayString="${app.saga-delay-ms:5000}",initialDelayString="${app.saga-delay-ms:5000}")
 public void tick(){for(String id:returns.pending(PageRequest.of(0,20)))try{service.reconcile(id);}catch(Exception e){LoggerFactory.getLogger(getClass()).warn("Return reconciliation delayed: {}",e.getClass().getSimpleName());}}
}
