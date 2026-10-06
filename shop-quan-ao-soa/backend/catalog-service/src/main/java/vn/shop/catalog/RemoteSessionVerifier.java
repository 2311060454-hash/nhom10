package vn.shop.catalog;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import vn.shop.common.*;

@Component
public class RemoteSessionVerifier implements SessionVerifier {
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final String key;
    private final ObjectMapper mapper;
    public RemoteSessionVerifier(@Value("${security.internal-key}") String key,ObjectMapper mapper) { this.key=key; this.mapper=mapper; }
    @Override public boolean valid(Jwt jwt) {
        try {
            var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:8081/internal/sessions/"+jwt.getId()+"?userId="+Long.parseLong(jwt.getSubject())))
                .timeout(Duration.ofSeconds(5)).header("X-Internal-Key",key).GET().build();
            var response=client.send(req,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200) throw new ApiException(503,"Dịch vụ xác thực tạm thời không khả dụng");
            return mapper.readTree(response.body()).path("valid").asBoolean(false);
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new ApiException(503,"Yêu cầu bị gián đoạn"); }
        catch(java.io.IOException e) { throw new ApiException(503,"Dịch vụ xác thực tạm thời không khả dụng"); }
    }
}
