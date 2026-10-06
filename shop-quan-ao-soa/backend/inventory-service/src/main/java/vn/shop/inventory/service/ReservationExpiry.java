package vn.shop.inventory.service;
import vn.shop.inventory.repository.ReservationRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import org.slf4j.LoggerFactory;
@Component
public class ReservationExpiry {
    private final ReservationRepository reservations;private final InventoryService service;
    public ReservationExpiry(ReservationRepository r,InventoryService s){reservations=r;service=s;}
    @Scheduled(fixedDelayString="${app.expiry-delay-ms:30000}") public void expire(){for(String id:reservations.expired(Instant.now(),PageRequest.of(0,100)))try{service.expire(id);}catch(RuntimeException e){LoggerFactory.getLogger(getClass()).warn("Reservation expiry retry required: {}",e.getClass().getSimpleName());}}
}
