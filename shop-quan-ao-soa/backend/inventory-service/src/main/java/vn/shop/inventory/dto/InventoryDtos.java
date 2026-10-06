package vn.shop.inventory.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
public final class InventoryDtos {
    private InventoryDtos() {}
    public record StockView(long variantId,int onHand,int reserved,int available,int minimumStock) {}
    public record Availability(long variantId,int available) {}
    public record Adjustment(@NotNull @Positive Long variantId,@NotBlank @Pattern(regexp="RECEIPT|ISSUE|COUNT|MINIMUM") String type,
        @Min(0) @Max(1000000000) int quantity,@NotBlank @Size(max=500) String reason) {}
    public record Line(@NotNull @Positive Long variantId,@Min(1) @Max(1000000) int quantity) {}
    public record Reserve(@NotEmpty @Size(max=100) List<@Valid Line> items) {}
    public record ReservationView(String orderId,String state,Instant expiresAt,List<Line> items) {}
    public record TransactionView(long id,long variantId,String operationKey,String type,int quantityDelta,int reservedDelta,String reason,long actorId,Instant createdAt) {}
}
