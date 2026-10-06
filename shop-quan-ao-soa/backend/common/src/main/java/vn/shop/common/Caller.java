package vn.shop.common;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class Caller {
    private Caller() {}
    public static long id() { return Long.parseLong(jwt().getSubject()); }
    public static Jwt jwt() { return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
    public static boolean role(String role) { return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role)); }
}
