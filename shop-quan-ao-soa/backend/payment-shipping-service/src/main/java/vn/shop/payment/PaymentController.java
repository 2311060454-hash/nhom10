package vn.shop.payment;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import vn.shop.common.Caller;
import vn.shop.payment.PaymentDtos.*;
@RestController
public class PaymentController {
 private final PaymentService service;
 public PaymentController(PaymentService service){this.service=service;}
 @PostMapping("/internal/fulfillment/{orderId}/commands/{commandId}") public View command(@PathVariable String orderId,@PathVariable String commandId,@Valid @RequestBody Command input){return service.execute(orderId,commandId,input);}
 @GetMapping({"/api/payments/{orderId}","/api/shipments/{orderId}"}) public View get(@PathVariable String orderId){return service.get(orderId,Caller.id(),Caller.role("ADMIN")||Caller.role("STAFF"));}
}
