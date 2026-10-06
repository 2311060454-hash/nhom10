package vn.shop.promotion;
import vn.shop.common.*;
import vn.shop.promotion.PromotionDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @Validated
public class PromotionController {
 private final CommercialService commercial;private final SocialService social;
 public PromotionController(CommercialService c,SocialService s){commercial=c;social=s;}
 @GetMapping("/api/coupons/quote") public QuoteView quote(@RequestParam String code,@RequestParam BigDecimal subtotal){return commercial.quote(code,subtotal,Caller.id());}
 @GetMapping("/api/coupons/manage") @PreAuthorize("hasRole('ADMIN')") public PageResult<CouponView> coupons(@RequestParam(defaultValue="0") @Min(0) int page){return commercial.coupons(page);}
 @PostMapping("/api/coupons/manage") @PreAuthorize("hasRole('ADMIN')") public CouponView createCoupon(@Valid @RequestBody CouponInput input){return commercial.saveCoupon(null,input);}
 @PutMapping("/api/coupons/manage/{id}") @PreAuthorize("hasRole('ADMIN')") public CouponView updateCoupon(@PathVariable long id,@Valid @RequestBody CouponInput input){return commercial.saveCoupon(id,input);}
 @GetMapping("/api/promotions/active") public List<PromotionView> active(@RequestParam @Min(1) long productId,@RequestParam @Min(1) long categoryId){return commercial.active(productId,categoryId);}
 @GetMapping("/api/promotions/price") public PriceView effectivePrice(@RequestParam @Min(1) long productId,@RequestParam @Min(1) long categoryId,@RequestParam BigDecimal price){return commercial.price(new PriceQuote(productId,categoryId,price));}
 @GetMapping("/api/promotions/manage") @PreAuthorize("hasRole('ADMIN')") public PageResult<PromotionView> promotions(@RequestParam(defaultValue="0") @Min(0) int page){return commercial.promotions(page);}
 @PostMapping("/api/promotions/manage") @PreAuthorize("hasRole('ADMIN')") public PromotionView createPromotion(@Valid @RequestBody PromotionInput input){return commercial.savePromotion(null,input);}
 @PutMapping("/api/promotions/manage/{id}") @PreAuthorize("hasRole('ADMIN')") public PromotionView updatePromotion(@PathVariable long id,@Valid @RequestBody PromotionInput input){return commercial.savePromotion(id,input);}
 @GetMapping("/api/reviews/products/{id}") public PageResult<ReviewView> publicReviews(@PathVariable @Min(1) long id,@RequestParam(defaultValue="0") @Min(0) int page){return social.publicList(id,page);}
 @GetMapping("/api/reviews/mine") public PageResult<ReviewView> myReviews(@RequestParam(defaultValue="0") @Min(0) int page){return social.mine(Caller.id(),page);}
 @PostMapping("/api/reviews") public ReviewView review(@Valid @RequestBody ReviewInput input){return social.submit(Caller.id(),input);}
 @DeleteMapping("/api/reviews/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void hideOwn(@PathVariable long id){social.hideMine(id,Caller.id());}
 @GetMapping("/api/reviews/manage") @PreAuthorize("hasRole('ADMIN')") public PageResult<ReviewView> manageReviews(@RequestParam(required=false) Long productId,@RequestParam(required=false) Integer stars,@RequestParam(required=false) String state,@RequestParam(defaultValue="0") @Min(0) int page){return social.manage(productId,stars,state,page);}
 @PutMapping("/api/reviews/manage/{id}") @PreAuthorize("hasRole('ADMIN')") public ReviewView moderate(@PathVariable long id,@Valid @RequestBody Moderate input){return social.moderate(id,input);}
 @GetMapping("/api/wishlists") public List<WishlistView> wishlist(){return social.favorites(Caller.id());}
 @PutMapping("/api/wishlists/{productId}") public WishlistView favorite(@PathVariable @Min(1) long productId){return social.favorite(Caller.id(),productId);}
 @DeleteMapping("/api/wishlists/{productId}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void unFavorite(@PathVariable @Min(1) long productId){social.removeFavorite(Caller.id(),productId);}
 @GetMapping("/internal/coupons/quote") public QuoteView internalQuote(@RequestParam String code,@RequestParam BigDecimal subtotal,@RequestParam long userId){return commercial.quote(code,subtotal,userId);}
 @PostMapping("/internal/coupons/reservations/{orderId}") public UsageView reserve(@PathVariable String orderId,@Valid @RequestBody CouponReserve input){return commercial.reserve(orderId,input);}
 @PostMapping("/internal/coupons/reservations/{orderId}/commit") public UsageView commit(@PathVariable String orderId){return commercial.finish(orderId,"commit");}
 @PostMapping("/internal/coupons/reservations/{orderId}/release") public UsageView release(@PathVariable String orderId){return commercial.finish(orderId,"release");}
 @PostMapping("/internal/promotions/price") public PriceView price(@Valid @RequestBody PriceQuote input){return commercial.price(input);}
 @GetMapping("/internal/promotions/marketing") public PageResult<PromotionView> marketing(@RequestParam java.time.Instant since,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="100") @Min(1) @Max(100) int size){return commercial.marketing(since,page,size);}
}
