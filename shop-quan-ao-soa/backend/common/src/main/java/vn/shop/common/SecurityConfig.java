package vn.shop.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecurityFilterChain security(HttpSecurity http, JwtService jwt, SessionVerifier verifier, ObjectMapper mapper,
            @Value("${security.internal-key}") String internalKey, @Value("${security.cors-origin:http://localhost:5173}") String origin) throws Exception {
        if (internalKey.length() < 32) throw new IllegalArgumentException("INTERNAL_API_KEY phải có ít nhất 32 ký tự");
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(java.util.Arrays.stream(origin.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", cors);
        return http.csrf(c -> c.disable()).cors(c -> c.configurationSource(source))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/support/guest").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/catalog/**", "/api/reviews/products/**", "/api/promotions/active", "/api/promotions/price", "/api/inventory/availability/**").permitAll()
                .requestMatchers("/internal/**").hasRole("INTERNAL").anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> write(res, mapper, 401, "Vui lòng đăng nhập"))
                .accessDeniedHandler((req, res, ex) -> write(res, mapper, 403, "Bạn không có quyền truy cập")))
            .addFilterBefore(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
                    try {
                        if (req.getRequestURI().startsWith("/internal/")) {
                            String supplied = req.getHeader("X-Internal-Key");
                            if (supplied != null && MessageDigest.isEqual(internalKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
                                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("service", null, List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))));
                            }
                        } else {
                            String auth = req.getHeader("Authorization");
                            if (auth != null && auth.startsWith("Bearer ")) {
                                var token = jwt.parse(auth.substring(7));
                                if (!verifier.valid(token)) { write(res, mapper, 401, "Phiên đăng nhập đã hết hiệu lực"); return; }
                                var authorities = new ArrayList<SimpleGrantedAuthority>();
                                token.getClaimAsStringList("roles").forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
                                if (Boolean.TRUE.equals(token.getClaimAsBoolean("inventoryWrite"))) authorities.add(new SimpleGrantedAuthority("INVENTORY_WRITE"));
                                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(token, null, authorities));
                            }
                        }
                    } catch (JwtException | IllegalArgumentException e) { write(res, mapper, 401, "Token không hợp lệ hoặc hết hạn"); return; }
                    catch (ApiException e) { write(res, mapper, e.status(), e.getMessage()); return; }
                    chain.doFilter(req, res);
                }
            }, UsernamePasswordAuthenticationFilter.class).build();
    }
    private static void write(HttpServletResponse res, ObjectMapper mapper, int status, String message) throws IOException {
        res.setStatus(status); res.setContentType("application/json;charset=UTF-8"); mapper.writeValue(res.getOutputStream(), Errors.body(status, message));
    }
}
