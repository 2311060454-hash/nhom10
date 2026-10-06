package vn.shop.common;

import org.springframework.security.oauth2.jwt.Jwt;

@FunctionalInterface
public interface SessionVerifier { boolean valid(Jwt jwt); }
