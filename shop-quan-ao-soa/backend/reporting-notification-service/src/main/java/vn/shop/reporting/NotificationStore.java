package vn.shop.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.shop.common.*;
import vn.shop.reporting.ReportingDtos.NotificationView;

@Service
public class NotificationStore {
 private static final Map<String,String> LABELS=Map.of(
  "PLACED","Đơn đã đặt","AWAITING_PAYMENT","Chờ thanh toán mô phỏng","CONFIRMED","Đã xác nhận",
  "PACKING","Đang chuẩn bị hàng","SHIPPED","Đang giao hàng","DELIVERED","Đã giao hàng",
  "COMPLETED","Đơn hoàn tất","RETURNED","Đơn đã trả và hoàn tiền","CANCELLED","Đơn đã hủy","FAILED","Đặt hàng thất bại");
 private final NotificationRepository notifications;private final NotificationGuardRepository guard;
 public NotificationStore(NotificationRepository notifications,NotificationGuardRepository guard){this.notifications=notifications;this.guard=guard;}
 @Transactional
 public void importEvents(long userId,List<JsonNode> orders){
  guard.lock();
  for(JsonNode order:orders){if(order.path("userId").asLong()!=userId)throw new ApiException(503,"Dữ liệu sự kiện đơn không khớp tài khoản");
   String orderId=order.path("id").asText();
   for(JsonNode event:order.path("events")){
    String state=event.path("state").asText(),title=LABELS.get(state);if(title==null)continue;
    String time=event.path("createdAt").asText(),eventKey=orderId+":"+state+":"+time;
    if(notifications.existsByEventKey(eventKey))continue;
    Notification n=new Notification();n.userId=userId;n.eventKey=eventKey;n.orderId=orderId;n.state=state;n.title=title;n.body="Đơn "+orderId+": "+title.toLowerCase(new Locale("vi","VN"))+".";n.createdAt=Instant.parse(time);notifications.save(n);
   }
   for(JsonNode event:order.path("returnEvents")){
    String state=event.path("state").asText();String title=switch(state){
     case "REQUESTED" -> "Đã nhận yêu cầu trả hàng";
     case "APPROVED" -> "Yêu cầu trả hàng đã được duyệt";
     case "REJECTED" -> "Yêu cầu trả hàng đã bị từ chối";
     case "RECEIVED" -> "Cửa hàng đã nhận hàng trả";
     case "REFUND_PENDING" -> "Đang chờ hoàn tiền COD";
     case "REFUNDED" -> event.path("mode").asText().equals("PARTIAL")?"Đã xử lý hoàn tiền một phần":null;
     default -> null;
    };
    if(title==null)continue;
    String eventKey="return:"+event.path("returnId").asText()+":"+event.path("id").asLong();
    if(notifications.existsByEventKey(eventKey))continue;
    Notification n=new Notification();n.userId=userId;n.eventKey=eventKey;n.orderId=orderId;n.state="RETURN_"+state;n.title=title;
    String scope=event.path("mode").asText().equals("PARTIAL")?"một phần đơn":"toàn bộ đơn";
    BigDecimal amount=event.path("refundAmount").decimalValue();
    n.body="Đơn "+orderId+": "+title.toLowerCase(new Locale("vi","VN"))+" ("+scope+"). Số tiền yêu cầu hoàn: "+NumberFormat.getCurrencyInstance(new Locale("vi","VN")).format(amount)+".";
    n.createdAt=Instant.parse(event.path("createdAt").asText());notifications.save(n);
   }
  }
 }
 @Transactional
 public void importPromotions(long userId,List<JsonNode> promotions){
  guard.lock();
  for(JsonNode promotion:promotions){
   long id=promotion.path("id").asLong();if(id<1)throw new ApiException(503,"Dữ liệu khuyến mãi không hợp lệ");
   if(!promotion.path("active").asBoolean())throw new ApiException(503,"Chương trình khuyến mãi chưa hoạt động");
   String eventKey="promotion:"+userId+":"+id;if(notifications.existsByEventKey(eventKey))continue;
   String targetType=promotion.path("targetType").asText();long targetId=promotion.path("targetId").asLong();
   if(!Set.of("PRODUCT","CATEGORY").contains(targetType)||targetId<1)throw new ApiException(503,"Đích khuyến mãi không hợp lệ");
   Notification n=new Notification();n.userId=userId;n.eventKey=eventKey;n.state="PROMOTION";
   n.title="Chương trình khuyến mãi mới";
   String name=promotion.path("name").asText();if(name.isBlank()||name.length()>120)throw new ApiException(503,"Tên khuyến mãi không hợp lệ");
   Instant endsAt=Instant.parse(promotion.path("endsAt").asText());
   n.body=name+". Xem ưu đãi đang có hiệu lực trước "+DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(endsAt)+".";
   n.linkPath=targetType.equals("PRODUCT")?"/products/"+targetId:"/?categoryId="+targetId;
   n.createdAt=StreamMax.latest(promotion);notifications.save(n);
  }
 }
 private static final class StreamMax {
  static Instant latest(JsonNode p){
   Instant result=Instant.parse(p.path("startsAt").asText());
   for(String field:List.of("createdAt","updatedAt"))result=result.isAfter(Instant.parse(p.path(field).asText()))?result:Instant.parse(p.path(field).asText());
   return result;
  }
 }
 @Transactional(readOnly=true) public PageResult<NotificationView> list(long userId,int page){var p=notifications.findByUserIdOrderByCreatedAtDescIdDesc(userId,PageRequest.of(page,20));return new PageResult<>(p.map(this::view).getContent(),p.getTotalElements(),p.getTotalPages(),page,20,p.isLast());}
 @Transactional(readOnly=true) public long unread(long userId){return notifications.countByUserIdAndReadAtIsNull(userId);}
 @Transactional public NotificationView read(long userId,long id){Notification n=notifications.findById(id).orElseThrow(ApiException::missing);if(n.userId!=userId)throw ApiException.missing();if(n.readAt==null)n.readAt=Instant.now();return view(n);}
 private NotificationView view(Notification n){return new NotificationView(n.id,n.orderId,n.linkPath,n.state,n.title,n.body,n.createdAt,n.readAt);}
}
