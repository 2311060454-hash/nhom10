package vn.shop.order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.shop.common.*;
import vn.shop.order.OrderDtos.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
@RestController @Validated
public class OrderController {
 private final OrderService service;private final FulfillmentService fulfillment;private final OrderRepository orders;
 public OrderController(OrderService s,FulfillmentService f,OrderRepository o){service=s;fulfillment=f;orders=o;}
 @GetMapping("/api/cart") public CartView cart(){return service.cartView(Caller.id());}
 @PutMapping("/api/cart/items/{id}") public CartView put(@PathVariable @Min(1) long id,@Valid @RequestBody Quantity q){return service.setQuantity(Caller.id(),id,q.quantity());}
 @DeleteMapping("/api/cart/items/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void delete(@PathVariable long id){service.remove(Caller.id(),id);}
 @DeleteMapping("/api/cart") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void clear(){service.remove(Caller.id(),null);}
 @PostMapping("/api/orders") @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
 public OrderView checkout(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody Checkout c){return service.checkout(Caller.id(),key,c);}
 @GetMapping("/api/orders") public PageResult<OrderView> mine(@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(required=false) String state){return service.list(Caller.id(),false,state,page);}
 @GetMapping("/api/orders/manage") @PreAuthorize("hasAnyRole('ADMIN','STAFF')") public PageResult<OrderView> manage(@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(required=false) String state,@RequestParam(required=false) @Size(max=120) String q,@RequestParam(required=false) Long customerId,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return service.list(Caller.id(),true,state,q,customerId,from,to,page);}
 @GetMapping("/api/orders/{id}") public OrderView get(@PathVariable String id){return service.get(id,Caller.id(),manager());}
 @PostMapping("/api/orders/{id}/state") public OrderView transition(@PathVariable String id,@Valid @RequestBody Transition t){return service.transition(id,Caller.id(),manager(),t.state());}
 @PostMapping("/api/orders/{id}/fulfillment") @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
 public CommandView fulfill(@PathVariable String id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody FulfillmentInput input){return fulfillment.submit(id,Caller.id(),manager(),key,input);}
 @GetMapping("/api/orders/{id}/fulfillment") public java.util.List<CommandView> commands(@PathVariable String id){return fulfillment.list(id,Caller.id(),manager());}
 @GetMapping("/internal/orders/purchases") public java.util.Map<String,Boolean> purchased(@RequestParam long userId,@RequestParam long productId){return java.util.Map.of("eligible",orders.purchased(userId,productId));}
 private boolean manager(){return Caller.role("ADMIN")||Caller.role("STAFF");}
}
