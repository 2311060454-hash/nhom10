package vn.shop.order;
import vn.shop.common.*;
import vn.shop.order.OrderDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class OrderService {
 private final GuardRepository guard; private final CartRepository carts; private final OrderRepository orders; private final OrderRemote remote; private final PromotionBridge promotion; private final ObjectMapper mapper;
 @org.springframework.beans.factory.annotation.Value("${bank.account-number:}") private String bankAccountNumber;
 @org.springframework.beans.factory.annotation.Value("${bank.account-name:}") private String bankAccountName;
 @org.springframework.beans.factory.annotation.Value("${bank.name:}") private String bankName;
 public OrderService(GuardRepository g,CartRepository c,OrderRepository o,OrderRemote r,PromotionBridge promo,ObjectMapper m){guard=g;carts=c;orders=o;remote=r;promotion=promo;mapper=m;}
 private void lock(){if(guard.lock()==null)throw new IllegalStateException("Missing order guard");}
 private Cart cart(long user){return carts.findById(user).orElseGet(()->{Cart c=new Cart();c.userId=user;return carts.save(c);});}
 private Line line(CartItem i){var q=remote.quote(i.variantId);long category=q.path("categoryId").asLong();if(category<=0)throw new ApiException(503,"Catalog chưa trả danh mục hợp lệ cho biến thể");return new Line(i.variantId,q.path("productId").asLong(),category,q.path("sku").asText(),q.path("productName").asText(),q.path("size").asText(),q.path("color").asText(),i.quantity,promotion.price(q.path("productId").asLong(),category,q.path("price").decimalValue()),q.path("active").asBoolean(),q.path("imageUrl").isMissingNode()||q.path("imageUrl").isNull()?null:q.path("imageUrl").asText());}
 private CartView view(Cart c){var lines=c.items.stream().map(this::line).toList();BigDecimal sum=lines.stream().map(i->i.unitPrice().multiply(BigDecimal.valueOf(i.quantity()))).reduce(BigDecimal.ZERO,BigDecimal::add);BigDecimal fee=lines.isEmpty()?BigDecimal.ZERO:new BigDecimal("30000");return new CartView(c.revision,lines,sum,fee,sum.add(fee));}
 @Transactional public CartView cartView(long user){lock();return view(cart(user));}
 @Transactional public CartView setQuantity(long user,long variant,int quantity){
  if(quantity<1||quantity>99)throw new ApiException(400,"Số lượng từ 1 đến 99");
  lock();if(!remote.quote(variant).path("active").asBoolean())throw new ApiException(409,"Biến thể đã ngừng bán");
  Cart c=cart(user);CartItem item=c.items.stream().filter(i->i.variantId==variant).findFirst().orElse(null);
  if(item==null){if(c.items.size()>=20)throw new ApiException(409,"Giỏ hàng tối đa 20 biến thể");item=new CartItem();item.variantId=variant;c.items.add(item);}item.quantity=quantity;c.revision++;c.updatedAt=Instant.now();return view(c);
 }
 @Transactional public void remove(long user,Long variant){lock();Cart c=cart(user);if(variant==null)c.items.clear();else c.items.removeIf(i->i.variantId==variant);c.revision++;c.updatedAt=Instant.now();}
 @Transactional public OrderView checkout(long user,String key,Checkout in){
  if(!key.matches("[A-Za-z0-9_-]{8,100}"))throw new ApiException(400,"Idempotency-Key không hợp lệ");
  if("BANK_TRANSFER".equals(in.paymentMethod())&&(bankAccountNumber==null||bankAccountNumber.isBlank()||bankAccountName==null||bankAccountName.isBlank()||bankName==null||bankName.isBlank()))throw new ApiException(409,"Chuyển khoản ngân hàng chưa được cấu hình");
  lock();String hash=hash(in);var previous=orders.findByUserIdAndRequestKey(user,key);
  if(previous.isPresent()){if(!previous.get().requestHash.equals(hash))throw new ApiException(409,"Key đã dùng với nội dung khác");return orderView(previous.get());}
  Cart c=cart(user);if(c.revision!=in.cartRevision())throw new ApiException(409,"Giỏ hàng đã thay đổi. Hãy tải lại trước khi đặt");
  CartView cv=view(c);if(cv.items().isEmpty())throw new ApiException(409,"Giỏ hàng rỗng");if(cv.items().stream().anyMatch(i->!i.active()))throw new ApiException(409,"Có sản phẩm đã ngừng bán");
  ShopOrder o=new ShopOrder();o.id=UUID.randomUUID().toString();o.userId=user;o.requestKey=key;o.requestHash=hash;o.recipient=in.recipient().trim();o.phone=in.phone().trim();o.address=in.address().trim();o.note=in.note();o.paymentMethod=in.paymentMethod()==null?"COD":in.paymentMethod();o.subtotal=cv.subtotal();o.shippingFee=cv.shippingFee();if(in.couponCode()!=null){var cq=promotion.quote(in.couponCode(),cv.subtotal(),user);o.couponCode=cq.path("code").asText();o.discount=cq.path("discount").decimalValue();}o.total=o.subtotal.subtract(o.discount).add(o.shippingFee);
  for(Line l:cv.items()){OrderItem i=new OrderItem();i.variantId=l.variantId();i.productId=l.productId();i.categoryId=l.categoryId();i.sku=l.sku();i.productName=l.productName();i.size=l.size();i.color=l.color();i.quantity=l.quantity();i.unitPrice=l.unitPrice();o.items.add(i);}
  change(o,"PROCESSING",user);orders.save(o);c.items.clear();c.revision++;c.updatedAt=Instant.now();return orderView(o);
 }
 @Transactional(readOnly=true) public OrderView get(String id,long user,boolean manage){return orderView(owned(id,user,manage));}
 @Transactional(readOnly=true) public PageResult<OrderView> list(long user,boolean manage,String state,int page){return list(user,manage,state,null,null,null,null,page);}
 @Transactional(readOnly=true) public PageResult<OrderView> list(long user,boolean manage,String state,String q,LocalDate from,LocalDate to,int page){return list(user,manage,state,q,null,from,to,page);}
 @Transactional(readOnly=true) public PageResult<OrderView> list(long user,boolean manage,String state,String q,Long customerId,LocalDate from,LocalDate to,int page){
  if(!manage&&(q!=null||from!=null||to!=null||customerId!=null))throw new ApiException(403,"Bộ lọc quản lý chỉ dành cho nhân viên");
  if(from!=null&&to!=null&&from.isAfter(to))throw new ApiException(400,"Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
  if(q!=null){q=q.trim();if(q.isEmpty())q=null;}
  ZoneId zone=ZoneId.of("Asia/Ho_Chi_Minh");
  Instant start=from==null?null:from.atStartOfDay(zone).toInstant(),end=to==null?null:to.plusDays(1).atStartOfDay(zone).toInstant();
  Long owner=manage?customerId:Long.valueOf(user);
  var p=orders.search(owner,state,q,start,end,PageRequest.of(page,20,Sort.by("createdAt").descending().and(Sort.by("id").descending())));
  return new PageResult<>(p.map(this::orderView).getContent(),p.getTotalElements(),p.getTotalPages(),page,20,p.isLast());
 }
 @Transactional public OrderView transition(String id,long user,boolean manage,String state){
  lock();ShopOrder o=owned(id,user,manage);if(o.pendingCommandId!=null)throw new ApiException(409,"Thao tác thanh toán/vận chuyển đang được đối soát");
  if(state.equals("CANCELLED")){
   if(o.state.equals("CANCELLED")||o.state.equals("CANCEL_PENDING"))return orderView(o);
   if(o.paymentMethod.equals("BANK_TRANSFER")&&o.paymentState.equals("PAID"))throw new ApiException(409,"Đơn chuyển khoản đã được xác nhận; cần quy trình hoàn tiền trước khi hủy");
   if(!Set.of("PROCESSING","AWAITING_PAYMENT","AWAITING_BANK","PLACED").contains(o.state)&&!(manage&&Set.of("CONFIRMED","PACKING").contains(o.state)))throw new ApiException(409,"Đơn không còn được phép hủy");
   change(o,"CANCEL_PENDING",user);
  }else{
   if(!manage)throw new ApiException(403,"Không có quyền xử lý đơn");
   if(o.state.equals(state))return orderView(o);
   var next=Map.of("PLACED","CONFIRMED","CONFIRMED","PACKING");
   if(!state.equals(next.get(o.state)))throw new ApiException(409,"Chuyển trạng thái không hợp lệ");change(o,state,user);
  }return orderView(o);
 }
 private ShopOrder owned(String id,long user,boolean manage){var o=orders.findById(id).orElseThrow(ApiException::missing);if(!manage&&o.userId!=user)throw ApiException.missing();return o;}
 static void change(ShopOrder o,String state,long actor){o.state=state;o.updatedAt=Instant.now();OrderHistory h=new OrderHistory();h.state=state;h.actorId=actor;o.history.add(h);}
 OrderView orderView(ShopOrder o){return new OrderView(o.id,o.userId,o.state,o.paymentMethod,o.paymentState,o.shippingState,o.pendingCommandId,o.recipient,o.phone,o.address,o.note,o.subtotal,o.discount,o.couponCode,o.shippingFee,o.total,o.returnedAmount,o.createdAt,o.items.stream().map(i->new Line(i.variantId,i.productId,i.categoryId,i.sku,i.productName,i.size,i.color,i.quantity,i.unitPrice,true,null)).toList(),o.history.stream().map(h->new History(h.state,h.actorId,h.createdAt)).toList());}
 private String checkoutJson(Checkout in)throws Exception{var json=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.valueToTree(in);if(in.paymentMethod()==null)json.remove("paymentMethod");if(in.couponCode()==null)json.remove("couponCode");return mapper.writeValueAsString(json);}
 private String hash(Checkout in){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(checkoutJson(in).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}


