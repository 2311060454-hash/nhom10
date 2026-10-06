package vn.shop.promotion;
import vn.shop.common.*;
import vn.shop.promotion.PromotionDtos.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class PromotionIntegrationTest {
 @Autowired CommercialService commercial;@Autowired SocialService social;@Autowired CouponRepository coupons;@Autowired UsageRepository usages;
 @Autowired PromotionRepository promotions;@Autowired ReviewRepository reviews;@Autowired WishlistRepository wishlists;@Autowired GuardRepository guards;
 @Autowired MockMvc mvc;@Autowired JwtService jwt;@Autowired ObjectMapper mapper;
 @MockitoBean RemoteSessionVerifier verifier;@MockitoBean InternalHttp http;
 @BeforeEach void setup()throws Exception{usages.deleteAll();coupons.deleteAll();promotions.deleteAll();reviews.deleteAll();wishlists.deleteAll();if(!guards.existsById(1L)){PromotionGuard g=new PromotionGuard();g.id=1L;guards.save(g);}when(verifier.valid(any())).thenReturn(true);when(http.get(eq(8084),anyString())).thenReturn(mapper.readTree("{\"eligible\":true}"));when(http.get(eq(8082),anyString())).thenReturn(mapper.readTree("{\"id\":1}"));}
 CouponInput coupon(String code,int total,int perCustomer){return new CouponInput(code,"PERCENT",new BigDecimal("10"),new BigDecimal("100000"),new BigDecimal("15000"),total,perCustomer,Instant.now().minusSeconds(60),Instant.now().plusSeconds(3600),true);}
 BigDecimal subtotal(){return new BigDecimal("200000");}
 @Test void quoteHasMinimumCapAndNoNegative(){commercial.saveCoupon(null,coupon("TENOFF",5,2));var q=commercial.quote("tenoff",subtotal(),1);assertThat(q.discount()).isEqualByComparingTo("15000");assertThat(q.remainingTotal()).isEqualByComparingTo("185000");assertThatThrownBy(()->commercial.quote("TENOFF",new BigDecimal("99000"),1)).isInstanceOf(ApiException.class);}
 @Test void oneRemainingCouponTwoCustomersCannotBothReserve()throws Exception{commercial.saveCoupon(null,coupon("LASTONE",1,1));var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);Callable<Integer> task=()->{gate.await();try{commercial.reserve(UUID.randomUUID().toString(),new CouponReserve("LASTONE",Thread.currentThread().getId(),subtotal()));return 200;}catch(ApiException e){return e.status();}};try{var a=pool.submit(task);var b=pool.submit(task);gate.countDown();assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);}finally{pool.shutdownNow();}assertThat(usages.count()).isEqualTo(1);}
 @Test void idempotentCommitReleaseFreesQuotaExactlyOnce(){commercial.saveCoupon(null,coupon("RESTOCK",1,1));String id=UUID.randomUUID().toString();var hold=commercial.reserve(id,new CouponReserve("RESTOCK",1,subtotal()));assertThat(commercial.reserve(id,new CouponReserve("RESTOCK",1,subtotal())).discount()).isEqualByComparingTo(hold.discount());commercial.finish(id,"commit");commercial.finish(id,"commit");commercial.finish(id,"release");commercial.finish(id,"release");assertThat(commercial.quote("RESTOCK",subtotal(),1).discount()).isEqualByComparingTo("15000");assertThat(usages.findById(id).orElseThrow().state).isEqualTo("RELEASED");}
 @Test void reusedOrderIdDifferentSubtotalBlocked(){commercial.saveCoupon(null,coupon("HASHED",3,2));String id=UUID.randomUUID().toString();commercial.reserve(id,new CouponReserve("HASHED",1,subtotal()));assertThatThrownBy(()->commercial.reserve(id,new CouponReserve("HASHED",1,new BigDecimal("300000")))).isInstanceOf(ApiException.class);}
 @Test void expiredHoldReleasesQuota(){commercial.saveCoupon(null,coupon("EXPIRE",1,1));String id=UUID.randomUUID().toString();commercial.reserve(id,new CouponReserve("EXPIRE",1,subtotal()));var row=usages.findById(id).orElseThrow();row.expiresAt=Instant.now().minusSeconds(1);usages.save(row);commercial.expire(id);commercial.expire(id);assertThatThrownBy(()->commercial.finish(id,"commit")).isInstanceOf(ApiException.class);assertThat(commercial.quote("EXPIRE",subtotal(),1).discount()).isPositive();}
 @Test void inactiveExpiredOrPerCustomerLimitCannotQuote(){commercial.saveCoupon(null,coupon("ONEUSER",3,1));commercial.reserve(UUID.randomUUID().toString(),new CouponReserve("ONEUSER",1,subtotal()));assertThatThrownBy(()->commercial.quote("ONEUSER",subtotal(),1)).isInstanceOf(ApiException.class);assertThat(commercial.quote("ONEUSER",subtotal(),2).discount()).isPositive();var c=coupons.findByCode("ONEUSER").orElseThrow();c.active=false;coupons.save(c);assertThatThrownBy(()->commercial.quote("ONEUSER",subtotal(),2)).isInstanceOf(ApiException.class);}
 @Test void productPromotionPicksHighestSavingAndExpires()throws Exception{when(http.get(eq(8082),eq("/api/catalog/categories"))).thenReturn(mapper.readTree("[{\"id\":2}]"));var now=Instant.now();commercial.savePromotion(null,new PromotionInput("Giảm SP","PRODUCT",1,"PERCENT",new BigDecimal("10"),null,now.minusSeconds(10),now.plusSeconds(3600),true));commercial.savePromotion(null,new PromotionInput("Giảm danh mục","CATEGORY",2,"FIXED",new BigDecimal("30000"),null,now.minusSeconds(10),now.plusSeconds(3600),true));assertThat(commercial.price(new PriceQuote(1,2,subtotal())).price()).isEqualByComparingTo("170000");assertThat(commercial.price(new PriceQuote(3,4,subtotal())).price()).isEqualByComparingTo("200000");}
 @Test void marketingFeedOnlyListsCurrentCampaignsAfterConsent()throws Exception{Instant now=Instant.now();
  var live=commercial.savePromotion(null,new PromotionInput("Ưu đãi mới","PRODUCT",1,"PERCENT",new BigDecimal("10"),null,now.minusSeconds(60),now.plusSeconds(3600),true));
  commercial.savePromotion(null,new PromotionInput("Chưa bắt đầu","PRODUCT",1,"PERCENT",new BigDecimal("10"),null,now.plusSeconds(3600),now.plusSeconds(7200),true));
  commercial.savePromotion(null,new PromotionInput("Đã tắt","PRODUCT",1,"PERCENT",new BigDecimal("10"),null,now.minusSeconds(60),now.plusSeconds(3600),false));
  assertThat(commercial.marketing(now.minusSeconds(10),0,20).content()).extracting("id").containsExactly(live.id());
  assertThat(commercial.marketing(Instant.now(),0,20).content()).isEmpty();
 }
 @Test void onlyCompletedBuyerMayReviewAndApprovalControlsVisibility()throws Exception{when(http.get(eq(8084),anyString())).thenReturn(mapper.readTree("{\"eligible\":false}"));assertThatThrownBy(()->social.submit(1,new ReviewInput(1,5,"Tốt"))).isInstanceOf(ApiException.class);when(http.get(eq(8084),anyString())).thenReturn(mapper.readTree("{\"eligible\":true}"));var r=social.submit(1,new ReviewInput(1,5,"Tốt"));assertThat(social.publicList(1,0).content()).isEmpty();social.moderate(r.id(),new Moderate("APPROVED"));assertThat(social.publicList(1,0).content()).hasSize(1);social.submit(1,new ReviewInput(1,4,"Sửa nhận xét"));assertThat(social.publicList(1,0).content()).isEmpty();assertThatThrownBy(()->social.hideMine(r.id(),2)).isInstanceOf(ApiException.class);social.hideMine(r.id(),1);assertThat(social.mine(1,0).content().get(0).state()).isEqualTo("HIDDEN");}
 @Test void wishlistIsPerOwnerAndIdempotent(){social.favorite(1,1);social.favorite(1,1);social.favorite(2,1);assertThat(social.favorites(1)).hasSize(1);social.removeFavorite(2,1);assertThat(social.favorites(1)).hasSize(1);assertThat(social.favorites(2)).isEmpty();}
 @Test void roleAndPublicBoundaries()throws Exception{String customer=jwt.issue(1,"test",List.of("CUSTOMER"),false);mvc.perform(get("/api/reviews/products/1")).andExpect(status().isOk());mvc.perform(get("/api/reviews/mine")).andExpect(status().isUnauthorized());mvc.perform(get("/api/promotions/price").param("productId","1").param("categoryId","2").param("price","100000")).andExpect(status().isOk());mvc.perform(get("/api/coupons/manage").header("Authorization","Bearer "+customer)).andExpect(status().isForbidden());mvc.perform(get("/api/reviews/manage").header("Authorization","Bearer "+customer)).andExpect(status().isForbidden());}
}
