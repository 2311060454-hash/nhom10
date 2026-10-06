package vn.shop.reporting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.shop.common.*;
import vn.shop.reporting.SupportDtos.*;

@RestController @Validated
public class SupportController {
 private final SupportService service;
 public SupportController(SupportService service){this.service=service;}
 private boolean manager(){return Caller.role("ADMIN")||Caller.role("STAFF");}
 @PostMapping("/api/support") @PreAuthorize("hasRole('CUSTOMER')") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
 public Detail create(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody Create input){return service.create(Caller.id(),key,input);}
 @GetMapping("/api/support") public PageResult<TicketView> mine(@RequestParam(defaultValue="0") @Min(0) int page){return service.list(Caller.id(),null,null,page);}
 @GetMapping("/api/support/manage") @PreAuthorize("hasAnyRole('ADMIN','STAFF')") public PageResult<TicketView> manage(@RequestParam(required=false) String state,@RequestParam(required=false) @Size(max=100) String q,@RequestParam(defaultValue="0") @Min(0) int page){return service.list(null,state,q,page);}
 @GetMapping("/api/support/{id}") public Detail detail(@PathVariable @Min(1) long id){return service.detail(id,Caller.id(),manager());}
 @PostMapping("/api/support/{id}/messages") public Detail reply(@PathVariable @Min(1) long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody Reply input){return service.reply(id,Caller.id(),Caller.role("ADMIN")?"ADMIN":manager()?"STAFF":"CUSTOMER",key,input);}
 @PutMapping("/api/support/{id}/state") public Detail change(@PathVariable @Min(1) long id,@Valid @RequestBody Change input){return service.change(id,Caller.id(),manager(),input.state());}
}
