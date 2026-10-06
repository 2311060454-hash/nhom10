package vn.shop.catalog.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class StoreDtos {
 private StoreDtos(){}
 public record BannerInput(@NotBlank @Size(max=160) String title,@Size(max=320) String subtitle,
  @NotBlank @Pattern(regexp="/|/products/[1-9][0-9]*") String linkPath,@Min(0) @Max(1000) int sortOrder,
  @NotNull Instant startsAt,@NotNull Instant endsAt,boolean active){}
 public record BannerView(long id,String title,String subtitle,String linkPath,String imageUrl,int sortOrder,Instant startsAt,Instant endsAt,boolean active,Instant createdAt,Instant updatedAt){}
 public record ContentInput(@NotNull @Size(max=10000) String text){}
 public record ContentView(String key,String text,Instant updatedAt){}
}
