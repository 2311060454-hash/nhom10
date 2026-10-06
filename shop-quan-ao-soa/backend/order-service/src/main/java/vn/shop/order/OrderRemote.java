package vn.shop.order;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.shop.common.*;
@Component
public class OrderRemote {
 private final InternalHttp reads; private final ObjectMapper mapper; private final String key;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
 public OrderRemote(InternalHttp reads,ObjectMapper mapper,@Value("${security.internal-key}") String key){this.reads=reads;this.mapper=mapper;this.key=key;}
 public JsonNode quote(long id){return reads.get(8082,"/internal/catalog/variants/"+id);}
 public JsonNode reservation(String id){return reads.get(8083,"/internal/inventory/reservations/"+id);}
 public JsonNode mutate(String id,String action,Object body){
  try {
   var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:8083/internal/inventory/reservations/"+id+action))
    .timeout(Duration.ofSeconds(2)).header("X-Internal-Key",key).header("Content-Type","application/json")
    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
   var res=client.send(req,HttpResponse.BodyHandlers.ofString());
   if(res.statusCode()==409)throw new ApiException(409,"Không đủ hàng hoặc lượt giữ kho đã hết hạn");
   if(res.statusCode()!=200)throw new ApiException(503,"Kho chưa phản hồi thành công");
   return mapper.readTree(res.body());
  }catch(java.io.IOException e){throw new ApiException(503,"Chưa xác định kết quả kho; hệ thống sẽ đối soát lại");}
   catch(InterruptedException e){Thread.currentThread().interrupt();throw new ApiException(503,"Yêu cầu bị gián đoạn");}
 }
}
