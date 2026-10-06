package vn.shop.order;
import vn.shop.common.*;
import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class PromotionBridge {
 private final InternalHttp reads;private final ObjectMapper mapper;private final String key;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
 public PromotionBridge(InternalHttp reads,ObjectMapper mapper,@Value("${security.internal-key}") String key){this.reads=reads;this.mapper=mapper;this.key=key;}
 public BigDecimal price(long productId,long categoryId,BigDecimal base){return send("/internal/promotions/price",Map.of("productId",productId,"categoryId",categoryId,"price",base)).path("price").decimalValue();}
 public JsonNode quote(String code,BigDecimal subtotal,long user){String url="/internal/coupons/quote?code="+URLEncoder.encode(code,StandardCharsets.UTF_8)+"&subtotal="+subtotal.toPlainString()+"&userId="+user;return reads.get(8086,url);}
 public JsonNode reserve(ShopOrder o){return send("/internal/coupons/reservations/"+o.id,Map.of("code",o.couponCode,"userId",o.userId,"subtotal",o.subtotal));}
 public JsonNode commit(ShopOrder o){return send("/internal/coupons/reservations/"+o.id+"/commit",Map.of());}
 public JsonNode release(ShopOrder o){return send("/internal/coupons/reservations/"+o.id+"/release",Map.of());}
 private JsonNode send(String path,Object body){try{
  var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:8086"+path)).header("X-Internal-Key",key).header("Content-Type","application/json").timeout(Duration.ofSeconds(2)).POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
  var response=client.send(request,HttpResponse.BodyHandlers.ofString());
  if(response.statusCode()==409)throw new ApiException(409,mapper.readTree(response.body()).path("message").asText("Mã giảm giá không còn hợp lệ"));
  if(response.statusCode()!=200)throw new ApiException(503,"Dịch vụ khuyến mãi chưa phản hồi thành công");
  return mapper.readTree(response.body());
 }catch(java.io.IOException e){throw new ApiException(503,"Chưa xác định kết quả mã giảm giá; đang đối soát");}
 catch(InterruptedException e){Thread.currentThread().interrupt();throw new ApiException(503,"Yêu cầu bị gián đoạn");}}
}
