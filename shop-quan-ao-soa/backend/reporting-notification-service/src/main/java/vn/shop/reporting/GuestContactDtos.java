package vn.shop.reporting;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class GuestContactDtos {
 private GuestContactDtos(){}
 public record Create(@NotBlank @Size(min=2,max=120) String fullName,@NotBlank @Email @Size(max=190) String email,
  @Pattern(regexp="|[0-9+ ()-]{9,20}") String phone,@NotBlank @Size(min=5,max=160) String subject,
  @NotBlank @Size(min=10,max=4000) String message){}
 public record Update(@NotBlank @Pattern(regexp="IN_PROGRESS|RESOLVED") String state,@Size(max=1000) String note){}
 public record Receipt(long id,Instant createdAt){}
 public record View(long id,String fullName,String email,String phone,String subject,String message,String state,String staffNote,Long actorId,Instant createdAt,Instant updatedAt){}
}
