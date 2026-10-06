package vn.shop.inventory.controller;
import vn.shop.inventory.dto.InventoryDtos.*;
import vn.shop.inventory.service.InventoryService;
import vn.shop.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
@RestController @Validated
public class InventoryController {
    private final InventoryService service;private final InternalHttp http;
    public InventoryController(InventoryService s,InternalHttp h){service=s;http=h;}
    @GetMapping("/internal/inventory/report-summary") public java.util.Map<String,Long> reportSummary(){return java.util.Map.of("lowStockCount",service.list(null,true,0,1).totalElements());}
    @GetMapping("/api/inventory/availability/{id}") public Availability available(@PathVariable @Positive long id){return service.available(id);}
    @GetMapping("/api/inventory") @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public PageResult<StockView> list(@RequestParam(required=false) @Positive Long variantId,@RequestParam(defaultValue="false") boolean low,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.list(variantId,low,page,size);}
    @GetMapping(value="/api/inventory/export.csv",produces="text/csv;charset=UTF-8") @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public org.springframework.http.ResponseEntity<byte[]> export(@RequestParam(required=false) @Positive Long variantId,@RequestParam(defaultValue="false") boolean low){
        return org.springframework.http.ResponseEntity.ok().header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=bao-cao-ton-kho.csv").body(service.exportCsv(variantId,low));
    }
    @GetMapping("/api/inventory/history") @PreAuthorize("hasAnyRole('ADMIN','STAFF')") public PageResult<TransactionView> history(@RequestParam(required=false) @Positive Long variantId,@RequestParam(defaultValue="0") @Min(0) int page){return service.history(variantId,page);}
    @PostMapping("/api/inventory/adjustments") @PreAuthorize("hasRole('ADMIN') or (hasRole('STAFF') and hasAuthority('INVENTORY_WRITE'))")
    public StockView adjust(@RequestHeader("Idempotency-Key") @Pattern(regexp="[A-Za-z0-9_-]{8,100}") String key,@Valid @RequestBody Adjustment in){http.get(8082,"/internal/catalog/variants/"+in.variantId());return service.adjust(key,in,Caller.id());}
    @PostMapping("/internal/inventory/reservations/{orderId}") public ReservationView reserve(@PathVariable @Pattern(regexp="[a-fA-F0-9-]{36}") String orderId,@Valid @RequestBody Reserve in){return service.reserve(orderId,in);}
    @GetMapping("/internal/inventory/reservations/{orderId}") public ReservationView reservation(@PathVariable String orderId){return service.reservation(orderId);}
    @PostMapping("/internal/inventory/reservations/{orderId}/{action}") public ReservationView finish(@PathVariable String orderId,@PathVariable @Pattern(regexp="commit|release|restock") String action){return service.finish(orderId,action);}
    @PostMapping("/internal/inventory/reservations/{orderId}/returns/{returnId}") public ReservationView partialRestock(@PathVariable String orderId,@PathVariable String returnId,@Valid @RequestBody Reserve input){return service.partialRestock(orderId,returnId,input);}
}
