package vn.shop.gateway;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.shop.common.ApiException;

@RestController
public class GatewayController {
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final Map<String,Integer> ROUTES=Map.ofEntries(
        Map.entry("/api/auth",8081),Map.entry("/api/addresses",8081),Map.entry("/api/admin/users",8081),Map.entry("/api/admin/audit",8081),
        Map.entry("/api/catalog",8082),Map.entry("/api/inventory",8083),Map.entry("/api/cart",8084),Map.entry("/api/orders",8084),
        Map.entry("/api/payments",8085),Map.entry("/api/shipments",8085),Map.entry("/api/coupons",8086),Map.entry("/api/promotions",8086),Map.entry("/api/reviews",8086),
        Map.entry("/api/wishlists",8086),Map.entry("/api/reports",8087),Map.entry("/api/notifications",8087),Map.entry("/api/support",8087));
    @RequestMapping("/api/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) {
        String path=request.getRequestURI();
        int port=ROUTES.entrySet().stream().filter(e -> path.equals(e.getKey()) || path.startsWith(e.getKey()+"/"))
            .map(Map.Entry::getValue).findFirst().orElseThrow(ApiException::missing);
        if(path.contains("..") || path.contains("%") || path.contains(";")) throw new ApiException(400,"Đường dẫn không hợp lệ");
        try {
            byte[] body=request.getInputStream().readNBytes(10*1024*1024+1);
            if(body.length>10*1024*1024) throw new ApiException(413,"Yêu cầu vượt quá 10 MB");
            String query=request.getQueryString();
            var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path+(query==null?"":"?"+query))).timeout(Duration.ofSeconds(8));
            for(String header:new String[]{"Authorization","Content-Type","Idempotency-Key","Accept"}) {
                String value=request.getHeader(header); if(value!=null) builder.header(header,value);
            }
            var upstream=client.send(builder.method(request.getMethod(),HttpRequest.BodyPublishers.ofByteArray(body)).build(),HttpResponse.BodyHandlers.ofByteArray());
            var result=ResponseEntity.status(upstream.statusCode());
            upstream.headers().firstValue("Content-Type").ifPresent(v -> result.header("Content-Type",v));
            upstream.headers().firstValue("Content-Disposition").ifPresent(v -> result.header("Content-Disposition",v));
            result.header("Cache-Control","no-store");
            return result.body(upstream.body());
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new ApiException(503,"Yêu cầu bị gián đoạn"); }
        catch(java.io.IOException e) { throw new ApiException(503,"Dịch vụ tạm thời không phản hồi. Vui lòng thử lại sau"); }
    }
}
