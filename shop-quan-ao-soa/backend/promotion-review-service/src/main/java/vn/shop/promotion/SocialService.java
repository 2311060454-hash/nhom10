package vn.shop.promotion;
import vn.shop.common.*;
import vn.shop.promotion.PromotionDtos.*;
import java.time.Instant;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class SocialService {
 private final GuardRepository guard;private final ReviewRepository reviews;private final WishlistRepository wishlists;private final InternalHttp http;
 public SocialService(GuardRepository g,ReviewRepository r,WishlistRepository w,InternalHttp h){guard=g;reviews=r;wishlists=w;http=h;}
 private void lock(){if(guard.lock()==null)throw new IllegalStateException("Missing promotion guard");}
 private ReviewView view(Review r){return new ReviewView(r.id,r.userId,r.productId,r.stars,r.comment,r.state,r.createdAt,r.updatedAt);}
 @Transactional public ReviewView submit(long user,ReviewInput input){
  boolean purchased=http.get(8084,"/internal/orders/purchases?userId="+user+"&productId="+input.productId()).path("eligible").asBoolean(false);
  if(!purchased)throw new ApiException(403,"Chỉ khách đã hoàn tất mua sản phẩm được đánh giá");
  lock();Review r=reviews.findByUserIdAndProductId(user,input.productId()).orElseGet(()->{Review fresh=new Review();fresh.userId=user;fresh.productId=input.productId();return fresh;});
  r.stars=input.stars();r.comment=input.comment().trim();r.state="PENDING";r.updatedAt=Instant.now();return view(reviews.save(r));
 }
 @Transactional(readOnly=true) public PageResult<ReviewView> publicList(long product,int page){var p=reviews.findByProductIdAndStateOrderByCreatedAtDesc(product,"APPROVED",PageRequest.of(page,20));return new PageResult<>(p.map(this::view).getContent(),p.getTotalElements(),p.getTotalPages(),page,20,p.isLast());}
 @Transactional(readOnly=true) public PageResult<ReviewView> mine(long user,int page){var p=reviews.findByUserIdOrderByCreatedAtDesc(user,PageRequest.of(page,20));return new PageResult<>(p.map(this::view).getContent(),p.getTotalElements(),p.getTotalPages(),page,20,p.isLast());}
 @Transactional(readOnly=true) public PageResult<ReviewView> manage(Long product,Integer stars,String state,int page){var p=reviews.manage(product,stars,state,PageRequest.of(page,20,Sort.by("createdAt").descending()));return new PageResult<>(p.map(this::view).getContent(),p.getTotalElements(),p.getTotalPages(),page,20,p.isLast());}
 @Transactional public ReviewView moderate(long id,Moderate input){lock();var r=reviews.findById(id).orElseThrow(ApiException::missing);r.state=input.state();r.updatedAt=Instant.now();return view(r);}
 @Transactional public void hideMine(long id,long user){lock();var r=reviews.findById(id).orElseThrow(ApiException::missing);if(r.userId!=user)throw ApiException.missing();r.state="HIDDEN";r.updatedAt=Instant.now();}
 @Transactional(readOnly=true) public java.util.List<WishlistView> favorites(long user){return wishlists.findByUserIdOrderByCreatedAtDesc(user).stream().map(w->new WishlistView(w.productId,w.createdAt)).toList();}
 @Transactional public WishlistView favorite(long user,long product){http.get(8082,"/api/catalog/products/"+product);lock();var w=wishlists.findByUserIdAndProductId(user,product).orElseGet(()->{Wishlist fresh=new Wishlist();fresh.userId=user;fresh.productId=product;return wishlists.save(fresh);});return new WishlistView(w.productId,w.createdAt);}
 @Transactional public void removeFavorite(long user,long product){lock();wishlists.findByUserIdAndProductId(user,product).ifPresent(wishlists::delete);}
}
