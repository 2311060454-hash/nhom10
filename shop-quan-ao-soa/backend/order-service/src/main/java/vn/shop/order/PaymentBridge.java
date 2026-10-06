package vn.shop.order;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.shop.common.ApiException;
import vn.shop.order.OrderDtos.FulfillmentInput;
@Component
public class PaymentBridge {
 private final ObjectMapper mapper;private final String key;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
 public PaymentBridge(ObjectMapper m,@Value("${security.internal-key}") String key){mapper=m;this.key=key;}
 public JsonNode initialize(ShopOrder o){return execute(o,o.id+"-init",0,new FulfillmentInput("INIT",null,null,null,null,null,null));}
 public JsonNode cancel(ShopOrder o){return execute(o,o.id+"-cancel",0,new FulfillmentInput("CANCEL",null,null,null,null,null,null));}
 public JsonNode partial(ShopOrder o,String commandId,long actor,String action,BigDecimal refundAmount,String reason,String reference){
  var input=new FulfillmentInput(action,null,null,null,reference,null,reason);
  var json=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.valueToTree(input);
  json.put("userId",o.userId).put("method",o.paymentMethod).put("amount",o.total).put("shippingFee",o.shippingFee)
   .put("recipient",o.recipient).put("phone",o.phone).put("address",o.address).put("actorId",actor).put("refundAmount",refundAmount);
  return send(o.id,commandId,json);
 }
 public JsonNode execute(ShopOrder o,String commandId,long actor,FulfillmentInput in){
  var body=mapper.valueToTree(in);var json=(com.fasterxml.jackson.databind.node.ObjectNode)body;
  json.put("userId",o.userId).put("method",o.paymentMethod).put("amount",o.total).put("shippingFee",o.shippingFee)
   .put("recipient",o.recipient).put("phone",o.phone).put("address",o.address).put("actorId",actor);
  return send(o.id,commandId,json);
 }
 private JsonNode send(String orderId,String commandId,JsonNode json){
  try{
   var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:8085/internal/fulfillment/"+orderId+"/commands/"+commandId))
    .header("X-Internal-Key",key).header("Content-Type","application/json").timeout(Duration.ofSeconds(2)).POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(json))).build();
   var res=client.send(req,HttpResponse.BodyHandlers.ofString());
   if(res.statusCode()==409)throw new ApiException(409,mapper.readTree(res.body()).path("message").asText("Trạng thái thanh toán/vận chuyển không hợp lệ"));
   if(res.statusCode()!=200)throw new ApiException(503,"Dịch vụ thanh toán/vận chuyển chưa phản hồi thành công");
   return mapper.readTree(res.body());
  }catch(java.io.IOException e){throw new ApiException(503,"Chưa xác định kết quả thanh toán/vận chuyển; đang đối soát");}
  catch(InterruptedException e){Thread.currentThread().interrupt();throw new ApiException(503,"Yêu cầu bị gián đoạn");}
 }
 static void project(ShopOrder o,JsonNode result){o.paymentState=result.path("paymentState").asText();o.shippingState=result.path("shippingState").asText();}
}
