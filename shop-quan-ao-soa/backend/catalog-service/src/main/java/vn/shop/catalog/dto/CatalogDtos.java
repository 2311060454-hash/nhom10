package vn.shop.catalog.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public final class CatalogDtos {
    private CatalogDtos() {}
    public record CategoryInput(@NotBlank @Size(max=120) String name,@Positive Long parentId,boolean active) {}
    public record BrandInput(@NotBlank @Size(max=120) String name,boolean active) {}
    public record CategoryView(long id,String name,Long parentId,boolean active) {}
    public record BrandView(long id,String name,boolean active) {}
    public record VariantInput(@Positive Long id,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,80}") String sku,
        @NotBlank @Size(max=20) String size,@NotBlank @Size(max=50) String color,
        @NotNull @DecimalMin("0") @Digits(integer=13,fraction=2) BigDecimal costPrice,
        @NotNull @DecimalMin("1") @Digits(integer=13,fraction=2) BigDecimal price,
        @DecimalMin("1") @Digits(integer=13,fraction=2) BigDecimal salePrice,boolean active) {}
    public record ProductInput(@NotBlank @Size(max=200) String name,@NotNull @Positive Long categoryId,@NotNull @Positive Long brandId,
        @NotBlank @Size(max=10000) String description,@NotBlank @Size(max=120) String material,@NotBlank @Size(max=120) String style,
        @NotBlank @Pattern(regexp="NAM|NU|UNISEX") String gender,boolean active,boolean featured,@PositiveOrZero Long version,
        @NotEmpty @Size(max=100) List<@Valid VariantInput> variants) {}
    public record VariantView(long id,String sku,String size,String color,BigDecimal price,BigDecimal salePrice,boolean active,BigDecimal costPrice) {}
    public record ImageView(String id,String url) {}
    public record ProductView(long id,String code,String name,long categoryId,String categoryName,long brandId,String brandName,
        String description,String material,String style,String gender,boolean active,boolean featured,long version,Instant createdAt,
        List<VariantView> variants,List<ImageView> images) {}
    public record Quote(long variantId,long productId,long categoryId,String sku,String productName,String size,String color,BigDecimal price,boolean active,String imageUrl) {}
}

