package vn.shop.reporting;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class SupportDtos {
 private SupportDtos(){}
 public record Create(@NotBlank @Size(min=5,max=160) String subject,@NotBlank @Size(min=10,max=4000) String message){}
 public record Reply(@NotBlank @Size(min=1,max=4000) String message){}
 public record Change(@NotBlank @Pattern(regexp="IN_PROGRESS|RESOLVED|CLOSED") String state){}
 public record TicketView(long id,long userId,String subject,String state,Instant createdAt,Instant updatedAt){}
 public record MessageView(long id,long authorId,String authorRole,String message,Instant createdAt){}
 public record StatusView(String state,long actorId,Instant createdAt){}
 public record Detail(TicketView ticket,List<MessageView> messages,List<StatusView> history){}
}
