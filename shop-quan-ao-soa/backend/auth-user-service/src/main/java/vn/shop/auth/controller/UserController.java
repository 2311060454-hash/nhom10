package vn.shop.auth.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import vn.shop.auth.dto.AuthDtos.*;
import vn.shop.auth.repository.*;
import vn.shop.auth.service.*;
import vn.shop.common.PageResult;

@RestController @Validated
public class UserController {
    private final AddressService addresses;
    private final AuthService auth;
    private final UserRepository users;
    private final AuditRepository audits;
    public UserController(AddressService addresses,AuthService auth,UserRepository users,AuditRepository audits) { this.addresses=addresses; this.auth=auth; this.users=users; this.audits=audits; }
    @GetMapping("/internal/users/report-summary") public java.util.Map<String,Long> reportSummary() { return java.util.Map.of("customerCount",users.search("","CUSTOMER",PageRequest.of(0,1)).getTotalElements()); }
    @GetMapping("/internal/users/{id}/marketing-preference") public MarketingPreference marketing(@PathVariable @Min(1) long id){return auth.marketing(id);}
    @GetMapping("/api/addresses") public List<AddressView> addresses() { return addresses.list(); }
    @PostMapping("/api/addresses") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public AddressView create(@Valid @RequestBody AddressInput input) { return addresses.save(null,input); }
    @PutMapping("/api/addresses/{id}") public AddressView update(@PathVariable long id,@Valid @RequestBody AddressInput input) { return addresses.save(id,input); }
    @DeleteMapping("/api/addresses/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { addresses.delete(id); }
    @GetMapping("/api/admin/users") @PreAuthorize("hasRole('ADMIN')")
    public PageResult<UserView> users(@RequestParam(defaultValue="") @Size(max=100) String q,@RequestParam(defaultValue="") @Pattern(regexp="|ADMIN|STAFF|CUSTOMER") String role,
            @RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        var result=users.search(q,role,PageRequest.of(page,size,Sort.by("id").descending())).map(UserView::of);
        return page(result);
    }
    @PostMapping("/api/admin/users/staff") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public UserView staff(@Valid @RequestBody Staff input) { return auth.staff(input); }
    @PutMapping("/api/admin/users/{id}/access") @PreAuthorize("hasRole('ADMIN')")
    public UserView access(@PathVariable long id,@Valid @RequestBody Access input) { return auth.access(id,input); }
    public record AuditView(long id,Long actorId,String action,Long targetId,java.time.Instant createdAt) {}
    @GetMapping("/api/admin/audit") @PreAuthorize("hasRole('ADMIN')")
    public PageResult<AuditView> audit(@RequestParam(defaultValue="0") @Min(0) int page) {
        return page(audits.findAll(PageRequest.of(page,30,Sort.by("id").descending())).map(a -> new AuditView(a.id,a.actorId,a.action,a.targetId,a.createdAt)));
    }
    private static <T> PageResult<T> page(Page<T> p) { return new PageResult<>(p.getContent(),p.getTotalElements(),p.getTotalPages(),p.getNumber(),p.getSize(),p.isLast()); }
}
