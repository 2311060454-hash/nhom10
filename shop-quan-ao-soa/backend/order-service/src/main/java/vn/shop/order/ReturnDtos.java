package vn.shop.order;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.math.BigDecimal;

public final class ReturnDtos {
 private ReturnDtos(){}
 public record ReturnLine(@Positive long variantId,@Min(1) int quantity){}
 public record Request(@NotBlank @Size(min=10,max=500) String reason,List<@NotNull @Valid ReturnLine> items){
  public Request(String reason){this(reason,null);}
 }
 public record Decision(@NotBlank @Pattern(regexp="APPROVE|REJECT") String action,@Size(max=500) String note){}
 public record Receipt(@NotBlank @Size(max=500) String note){}
 public record Confirm(@NotBlank @Size(max=190) String reference){}
 public record Event(String state,long actorId,String note,Instant createdAt){}
 public record View(String id,String orderId,long userId,String state,String reason,String mode,BigDecimal refundAmount,List<ReturnLine> items,String decisionNote,String refundReference,String lastError,Instant createdAt,Instant updatedAt,List<Event> history){}
}
