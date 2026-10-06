package vn.shop.promotion;
import vn.shop.common.*;
import vn.shop.promotion.PromotionDtos.*;
import java.math.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommercialService {
 private final GuardRepository guard;private final CouponRepository coupons;private final UsageRepository usages;private final PromotionRepository promotions;private final InternalHttp http;
 public CommercialService(GuardRepository g,CouponRepository c,UsageRepository u,PromotionRepository p,InternalHttp h){guard=g;coupons=c;usages=u;promotions=p;http=h;}
 private void lock(){if(guard.lock()==null)throw new IllegalStateException("Missing promotion guard");}
 private static String code(String text){String value=text==null?"":text.trim().toUpperCase(Locale.ROOT);if(!value.matches("[A-Z0-9_-]{4,40}"))throw new ApiException(400,"Mã giảm giá không hợp lệ");return value;}
 private static void dates(Instant from,Instant to){if(!from.isBefore(to))throw new ApiException(400,"Ngày kết thúc phải sau ngày bắt đầu");}
 private static void amount(String type,BigDecimal value){if(type.equals("PERCENT")&&value.compareTo(new BigDecimal("100"))>0)throw new ApiException(400,"Phần trăm không vượt 100");}
 private static BigDecimal discount(String type,BigDecimal value,BigDecimal cap,BigDecimal base){BigDecimal result=type.equals("PERCENT")?base.multiply(value).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP):value;if(cap!=null)result=result.min(cap);return result.min(base).max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP);}
 private CouponView view(Coupon c){return new CouponView(c.id,c.code,c.type,c.value,c.minimumTotal,c.maximumDiscount,c.totalLimit,c.perCustomerLimit,c.startsAt,c.endsAt,c.active,usages.countByCouponIdAndStateIn(c.id,List.of("HELD","COMMITTED")));}
 private PromotionView view(ProductPromotion p){return new PromotionView(p.id,p.name,p.targetType,p.targetId,p.type,p.value,p.maximumDiscount,p.startsAt,p.endsAt,p.active,p.createdAt,p.updatedAt);}
 @Transactional(readOnly=true) public PageResult<CouponView> coupons(int page){var p=coupons.findAll(PageRequest.of(page,20,Sort.by("id").descending()));return new PageResult<>(p.map(this::view).getContent(),p.getTotalElements(),p.getTotalPages(),page,20,p.isLast());}
 @Transactional public CouponView saveCoupon(Long id,CouponInput in){dates(in.startsAt(),in.endsAt());amount(in.type(),in.value());lock();String code=code(in.code());var found=coupons.findByCode(code);if(found.isPresent()&&!found.get().id.equals(id))throw new ApiException(409,"Mã giảm giá đã tồn tại");Coupon c=id==null?new Coupon():coupons.findById(id).orElseThrow(ApiException::missing);c.code=code;c.type=in.type();c.value=in.value();c.minimumTotal=in.minimumTotal();c.maximumDiscount=in.maximumDiscount();c.totalLimit=in.totalLimit();c.perCustomerLimit=in.perCustomerLimit();c.active=in.active();c.startsAt=in.startsAt();c.endsAt=in.endsAt();c.updatedAt=Instant.now();return view(coupons.saveAndFlush(c));}
 @Transactional(readOnly=true) public QuoteView quote(String input,BigDecimal subtotal,long user){Coupon c=coupons.findByCode(code(input)).orElseThrow(()->new ApiException(409,"Mã giảm giá không tồn tại"));Instant now=Instant.now();if(!c.active||now.isBefore(c.startsAt)||!now.isBefore(c.endsAt))throw new ApiException(409,"Mã chưa có hiệu lực hoặc đã hết hạn");if(subtotal.compareTo(c.minimumTotal)<0)throw new ApiException(409,"Đơn hàng chưa đạt giá trị tối thiểu");if(usages.countByCouponIdAndStateIn(c.id,List.of("HELD","COMMITTED"))>=c.totalLimit||usages.countByCouponIdAndUserIdAndStateIn(c.id,user,List.of("HELD","COMMITTED"))>=c.perCustomerLimit)throw new ApiException(409,"Mã giảm giá đã hết lượt sử dụng");BigDecimal d=discount(c.type,c.value,c.maximumDiscount,subtotal);return new QuoteView(c.code,d,subtotal.subtract(d));}
 @Transactional public UsageView reserve(String orderId,CouponReserve in){UUID.fromString(orderId);lock();var existing=usages.findById(orderId);if(existing.isPresent()){CouponUsage u=existing.get();Coupon c=coupons.findById(u.couponId).orElseThrow(ApiException::missing);if(u.userId!=in.userId()||!c.code.equals(code(in.code()))||u.originalSubtotal.compareTo(in.subtotal())!=0)throw new ApiException(409,"Đơn đã giữ mã giảm giá khác");if(!Set.of("HELD","COMMITTED").contains(u.state))throw new ApiException(409,"Lượt mã đã giải phóng");return usage(u,c.code);}var q=quote(in.code(),in.subtotal(),in.userId());Coupon c=coupons.findByCode(q.code()).orElseThrow(ApiException::missing);CouponUsage u=new CouponUsage();u.orderId=orderId;u.couponId=c.id;u.userId=in.userId();u.discount=q.discount();u.originalSubtotal=in.subtotal();u.expiresAt=Instant.now().plusSeconds(900);return usage(usages.save(u),c.code);}
 @Transactional public UsageView finish(String orderId,String action){lock();CouponUsage u=usages.findById(orderId).orElse(null);if(u==null){if(action.equals("release"))return new UsageView(orderId,0,null,"ABSENT",BigDecimal.ZERO,null);throw ApiException.missing();}Coupon c=coupons.findById(u.couponId).orElseThrow(ApiException::missing);
  switch(action){case "commit"->{if(u.state.equals("COMMITTED"))break;if(!u.state.equals("HELD")||!u.expiresAt.isAfter(Instant.now()))throw new ApiException(409,"Lượt mã đã hết hạn hoặc được giải phóng");u.state="COMMITTED";}
   case "release"->{if(u.state.equals("RELEASED")||u.state.equals("EXPIRED"))break;u.state="RELEASED";}
   default->throw new ApiException(400,"Thao tác mã giảm giá không hỗ trợ");}
  u.updatedAt=Instant.now();return usage(u,c.code);
 }
 @Transactional public void expire(String orderId){lock();var found=usages.findById(orderId);if(found.isPresent()){var u=found.get();if(u.state.equals("HELD")&&!u.expiresAt.isAfter(Instant.now())){u.state="EXPIRED";u.updatedAt=Instant.now();}}}
 public List<String> expiredIds(){return usages.findTop100ByStateAndExpiresAtBefore("HELD",Instant.now()).stream().map(u->u.orderId).toList();}
 private UsageView usage(CouponUsage u,String code){return new UsageView(u.orderId,u.userId,code,u.state,u.discount,u.expiresAt);}
 @Transactional(readOnly=true) public PageResult<PromotionView> promotions(int page){var p=promotions.findAll(PageRequest.of(page,20,Sort.by("id").descending()));return new PageResult<>(p.map(this::view).getContent(),p.getTotalElements(),p.getTotalPages(),page,20,p.isLast());}
 @Transactional(readOnly=true) public PageResult<PromotionView> marketing(Instant since,int page,int size){
  if(since==null||since.isAfter(Instant.now())||since.isBefore(Instant.parse("2000-01-01T00:00:00Z")))throw new ApiException(400,"Thời điểm đồng ý không hợp lệ");
  var p=promotions.marketing(since,Instant.now(),PageRequest.of(page,size,Sort.by("id")));
  return new PageResult<>(p.map(this::view).getContent(),p.getTotalElements(),p.getTotalPages(),page,size,p.isLast());
 }
 @Transactional public PromotionView savePromotion(Long id,PromotionInput in){dates(in.startsAt(),in.endsAt());amount(in.type(),in.value());
  if(in.targetType().equals("PRODUCT"))http.get(8082,"/api/catalog/products/"+in.targetId());
  else {var categories=http.get(8082,"/api/catalog/categories");boolean exists=false;for(var category:categories)if(category.path("id").asLong()==in.targetId())exists=true;if(!exists)throw ApiException.missing();}
  lock();ProductPromotion p=id==null?new ProductPromotion():promotions.findById(id).orElseThrow(ApiException::missing);p.name=in.name().trim();p.targetType=in.targetType();p.targetId=in.targetId();p.type=in.type();p.value=in.value();p.maximumDiscount=in.maximumDiscount();p.active=in.active();p.startsAt=in.startsAt();p.endsAt=in.endsAt();p.updatedAt=Instant.now();return view(promotions.save(p));
 }
 @Transactional(readOnly=true) public List<PromotionView> active(long product,long category){return promotions.active(product,category,Instant.now()).stream().map(this::view).toList();}
 @Transactional(readOnly=true) public PriceView price(PriceQuote quote){BigDecimal base=quote.price();BigDecimal best=BigDecimal.ZERO;Long bestId=null;for(var p:promotions.active(quote.productId(),quote.categoryId(),Instant.now())){BigDecimal d=discount(p.type,p.value,p.maximumDiscount,base);if(d.compareTo(best)>0){best=d;bestId=p.id;}}return new PriceView(base.subtract(best),base,bestId);}
}

