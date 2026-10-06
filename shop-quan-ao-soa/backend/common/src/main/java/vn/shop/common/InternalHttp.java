package vn.shop.common;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Chỉ dùng GET nội bộ. Không retry thao tác ghi chưa có idempotency. */
@Service
public class InternalHttp {
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper mapper;
    private final String key;
    public InternalHttp(ObjectMapper mapper,@Value("${security.internal-key}") String key) { this.mapper=mapper; this.key=key; }
    public JsonNode get(int port,String path) {
        for(int attempt=0;attempt<2;attempt++) {
            try {
                var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(2)).header("X-Internal-Key",key).GET().build();
                var res=client.send(req,HttpResponse.BodyHandlers.ofString());
                if(res.statusCode()==404) throw ApiException.missing();
                if(res.statusCode()!=200) throw new ApiException(503,"Dịch vụ phụ thuộc tạm thời không khả dụng");
                return mapper.readTree(res.body());
            } catch(java.io.IOException e) {
                if(attempt==1) throw new ApiException(503,"Dịch vụ phụ thuộc không phản hồi");
                try { Thread.sleep(100); } catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new ApiException(503,"Yêu cầu bị gián đoạn"); }
            } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new ApiException(503,"Yêu cầu bị gián đoạn"); }
        }
        throw new ApiException(503,"Dịch vụ phụ thuộc không phản hồi");
    }
}
