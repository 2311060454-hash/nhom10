package vn.shop.common;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    public JwtService(@Value("${security.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET phải có ít nhất 32 byte");
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var d = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        d.setJwtValidator(JwtValidators.createDefaultWithIssuer("shop-quan-ao"));
        decoder = d;
    }
    public String issue(long userId, String sessionId, List<String> roles, boolean inventoryWrite) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("shop-quan-ao").subject(Long.toString(userId))
            .issuedAt(now).expiresAt(now.plusSeconds(1800)).id(sessionId)
            .claim("roles", roles).claim("inventoryWrite", inventoryWrite).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
    public Jwt parse(String token) { return decoder.decode(token); }
}
