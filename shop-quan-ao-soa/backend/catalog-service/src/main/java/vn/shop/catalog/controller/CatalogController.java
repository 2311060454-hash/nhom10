package vn.shop.catalog.controller;
import vn.shop.catalog.dto.CatalogDtos.*;
import vn.shop.catalog.service.*;
import vn.shop.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController @Validated
public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService s) { service=s; }
    @GetMapping({"/api/catalog/products","/api/catalog/manage/products"})
    public PageResult<ProductView> search(jakarta.servlet.http.HttpServletRequest req,
        @RequestParam(defaultValue="") @Size(max=100) String q,@RequestParam(required=false) @Positive Long categoryId,@RequestParam(required=false) @Positive Long brandId,
        @RequestParam(defaultValue="") @Pattern(regexp="|NAM|NU|UNISEX") String gender,@RequestParam(defaultValue="") @Size(max=20) String size,
        @RequestParam(defaultValue="") @Size(max=50) String color,@RequestParam(required=false) @DecimalMin("0") BigDecimal min,@RequestParam(required=false) @DecimalMin("0") BigDecimal max,
        @RequestParam(defaultValue="false") boolean sale,@RequestParam(required=false) Boolean featured,@RequestParam(defaultValue="newest") @Pattern(regexp="newest|priceAsc|priceDesc|bestSelling") String sort,
        @RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="12") @Min(1) @Max(100) int limit) {
        boolean manage=req.getRequestURI().contains("/manage/");if(manage)requireAdmin();
        return service.search(q,categoryId,brandId,gender,size,color,min,max,sale,featured,sort,page,limit,manage);
    }
    private void requireAdmin() { if(!Caller.role("ADMIN"))throw new ApiException(403,"Chỉ quản trị viên được truy cập"); }
    @GetMapping("/api/catalog/products/{id}") public ProductView detail(@PathVariable long id) { return service.detail(id,false); }
    @GetMapping("/api/catalog/manage/products/{id}") @PreAuthorize("hasRole('ADMIN')") public ProductView manage(@PathVariable long id) { return service.detail(id,true); }
    @PostMapping("/api/catalog/manage/products") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ProductView create(@Valid @RequestBody ProductInput in) {return service.save(null,in);}
    @PutMapping("/api/catalog/manage/products/{id}") @PreAuthorize("hasRole('ADMIN')") public ProductView update(@PathVariable long id,@Valid @RequestBody ProductInput in) {return service.save(id,in);}
    @DeleteMapping("/api/catalog/manage/products/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable long id) {service.deactivate(id);}
    @GetMapping("/api/catalog/categories") public List<CategoryView> categories() {return service.categories(false);}
    @GetMapping("/api/catalog/brands") public List<BrandView> brands() {return service.brands(false);}
    @GetMapping("/api/catalog/manage/categories") @PreAuthorize("hasRole('ADMIN')") public List<CategoryView> allCategories() {return service.categories(true);}
    @GetMapping("/api/catalog/manage/brands") @PreAuthorize("hasRole('ADMIN')") public List<BrandView> allBrands() {return service.brands(true);}
    @PostMapping("/api/catalog/manage/categories") @PreAuthorize("hasRole('ADMIN')") public CategoryView category(@Valid @RequestBody CategoryInput in) {return service.category(null,in);}
    @PutMapping("/api/catalog/manage/categories/{id}") @PreAuthorize("hasRole('ADMIN')") public CategoryView category(@PathVariable long id,@Valid @RequestBody CategoryInput in) {return service.category(id,in);}
    @PostMapping("/api/catalog/manage/brands") @PreAuthorize("hasRole('ADMIN')") public BrandView brand(@Valid @RequestBody BrandInput in) {return service.brand(null,in);}
    @PutMapping("/api/catalog/manage/brands/{id}") @PreAuthorize("hasRole('ADMIN')") public BrandView brand(@PathVariable long id,@Valid @RequestBody BrandInput in) {return service.brand(id,in);}
    @GetMapping("/internal/catalog/variants/{id}") public Quote quote(@PathVariable long id) {return service.quote(id);}
}
