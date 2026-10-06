package vn.shop.reporting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.shop.common.*;
import vn.shop.reporting.GuestContactDtos.*;

@RestController @Validated
public class GuestContactController {
 private final GuestContactService service;
 public GuestContactController(GuestContactService service){this.service=service;}
 @PostMapping("/api/support/guest") @ResponseStatus(HttpStatus.CREATED)
 public Receipt create(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody Create input){return service.create(key,input);}
 @GetMapping("/api/support/guest/manage") @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
 public PageResult<View> list(@RequestParam(required=false) String state,@RequestParam(required=false) @Size(max=100) String q,@RequestParam(defaultValue="0") @Min(0) int page){return service.list(state,q,page);}
 @GetMapping("/api/support/guest/manage/{id}") @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
 public View get(@PathVariable @Min(1) long id){return service.get(id);}
 @PutMapping("/api/support/guest/manage/{id}") @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
 public View update(@PathVariable @Min(1) long id,@Valid @RequestBody Update input){return service.update(id,Caller.id(),input);}
}
