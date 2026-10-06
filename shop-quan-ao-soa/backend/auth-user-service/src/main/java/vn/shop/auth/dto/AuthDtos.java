package vn.shop.auth.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Set;
import vn.shop.auth.entity.User;
import vn.shop.auth.entity.Address;

public final class AuthDtos {
    private AuthDtos() {}
    public record Register(@NotBlank @Size(max=120) String fullName, @NotBlank @Email @Size(max=190) String email,
        @NotBlank @Pattern(regexp="[0-9+ ()-]{9,20}") String phone, @NotBlank @Size(min=10,max=64) String password) {}
    public record Login(@NotBlank @Email @Size(max=190) String email, @NotBlank @Size(max=64) String password) {}
    public record Profile(@NotBlank @Size(max=120) String fullName, @NotBlank @Pattern(regexp="[0-9+ ()-]{9,20}") String phone, boolean marketingConsent) {}
    public record ChangePassword(@NotBlank @Size(max=64) String currentPassword, @NotBlank @Size(min=10,max=64) String newPassword) {}
    public record Forgot(@NotBlank @Email @Size(max=190) String email) {}
    public record Reset(@NotBlank @Size(max=100) String token, @NotBlank @Size(min=10,max=64) String password) {}
    public record UserView(long id, String fullName, String email, String phone, Set<String> roles, boolean active, boolean inventoryWrite, boolean marketingConsent, Instant createdAt) {
        public static UserView of(User u) { return new UserView(u.id,u.fullName,u.email,u.phone,Set.copyOf(u.roles),u.active,u.inventoryWrite,u.marketingConsent,u.createdAt); }
    }
    public record MarketingPreference(boolean enabled, Instant since) {}
    public record LoginResult(String accessToken, String tokenType, long expiresIn, UserView user) {}
    public record AddressInput(@NotBlank @Size(max=120) String recipient, @NotBlank @Pattern(regexp="[0-9+ ()-]{9,20}") String phone, @NotBlank @Size(max=500) String detail, boolean defaultAddress) {}
    public record AddressView(long id, String recipient, String phone, String detail, boolean defaultAddress) {
        public static AddressView of(Address a) { return new AddressView(a.id,a.recipient,a.phone,a.detail,a.defaultAddress); }
    }
    public record Access(@NotBlank @Pattern(regexp="ADMIN|STAFF|CUSTOMER") String role, boolean active, boolean inventoryWrite) {}
    public record Staff(@NotBlank @Size(max=120) String fullName, @NotBlank @Email @Size(max=190) String email,
        @NotBlank @Pattern(regexp="[0-9+ ()-]{9,20}") String phone, @NotBlank @Size(min=10,max=64) String password, boolean inventoryWrite) {}
}
