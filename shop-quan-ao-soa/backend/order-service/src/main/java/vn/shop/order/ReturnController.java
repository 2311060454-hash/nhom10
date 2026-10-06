package vn.shop.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.shop.common.Caller;
import vn.shop.common.PageResult;
import vn.shop.order.ReturnDtos.*;

@RestController @org.springframework.validation.annotation.Validated
public class ReturnController {
 private final ReturnService service;
 public ReturnController(ReturnService service){this.service=service;}
 @GetMapping("/api/orders/returns/manage") @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
 public PageResult<View> list(@RequestParam(required=false) String state,@RequestParam(defaultValue="0") @Min(0) int page){return service.list(state,page);}
 @GetMapping("/api/orders/{orderId}/return")
 public View get(@PathVariable String orderId){return service.get(orderId,Caller.id(),manager());}
 @PostMapping("/api/orders/{orderId}/return") @PreAuthorize("hasRole('CUSTOMER')") @ResponseStatus(HttpStatus.CREATED)
 public View request(@PathVariable String orderId,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody Request input){return service.request(orderId,Caller.id(),key,input);}
 @PostMapping("/api/orders/{orderId}/return/decision") @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
 public View decide(@PathVariable String orderId,@Valid @RequestBody Decision input){return service.decide(orderId,Caller.id(),input);}
 @PostMapping("/api/orders/{orderId}/return/receive") @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
 public View receive(@PathVariable String orderId,@Valid @RequestBody Receipt input){return service.receive(orderId,Caller.id(),input);}
 @PostMapping("/api/orders/{orderId}/return/confirm-refund") @PreAuthorize("hasRole('ADMIN')")
 public View confirm(@PathVariable String orderId,@Valid @RequestBody Confirm input){return service.confirm(orderId,Caller.id(),input);}
 private boolean manager(){return Caller.role("ADMIN")||Caller.role("STAFF");}
}
