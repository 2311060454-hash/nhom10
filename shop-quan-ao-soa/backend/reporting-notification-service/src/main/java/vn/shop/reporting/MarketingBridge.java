package vn.shop.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import vn.shop.common.*;

@Component
public class MarketingBridge {
 private final InternalHttp http;
 public MarketingBridge(InternalHttp http){this.http=http;}
 public List<JsonNode> active(long userId){
  JsonNode preference=http.get(8081,"/internal/users/"+userId+"/marketing-preference");
  if(!preference.path("enabled").asBoolean())return List.of();
  Instant since=Instant.parse(preference.path("since").asText());
  String encoded=URLEncoder.encode(since.toString(),StandardCharsets.UTF_8);
  var result=new ArrayList<JsonNode>();
  for(int page=0;page<100;page++){
   JsonNode data=http.get(8086,"/internal/promotions/marketing?since="+encoded+"&page="+page+"&size=100");
   for(JsonNode row:data.path("content"))result.add(row);
   if(data.path("last").asBoolean())return result;
  }
  throw new ApiException(503,"Quá 10.000 chương trình khuyến mãi đang hiệu lực");
 }
}
