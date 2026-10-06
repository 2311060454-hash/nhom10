package vn.shop.reporting;

import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.shop.common.*;
import vn.shop.reporting.GuestContactDtos.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class GuestContactIntegrationTest {
 @Autowired GuestContactService service; @Autowired GuestContactRepository contacts;
 @Autowired SupportGuardRepository guards; @Autowired MockMvc mvc; @Autowired JwtService jwt;
 @MockitoBean RemoteSessionVerifier verifier;
 @BeforeEach void setup(){contacts.deleteAll();if(!guards.existsById(1L)){SupportGuard g=new SupportGuard();g.id=1L;guards.save(g);}when(verifier.valid(any())).thenReturn(true);}
 private Create input(String email){return new Create("Nguyễn Minh Anh",email,"0901234567","Hỏi về đổi size","Tôi muốn hỏi cách đổi size sau khi mua hàng");}
 @Test void retriesAndRateLimit(){
  var first=service.create("guest-key-0001",input("guest@example.com"));
  assertThat(service.create("guest-key-0001",input("guest@example.com")).id()).isEqualTo(first.id());
  assertThat(contacts.count()).isEqualTo(1);
  assertThatThrownBy(()->service.create("guest-key-0001",new Create("Nguyễn Minh Anh","guest@example.com","0901234567","Hỏi về giao hàng","Tôi muốn hỏi cách đổi size sau khi mua hàng"))).isInstanceOf(ApiException.class);
  service.create("guest-key-0002",input("guest@example.com"));service.create("guest-key-0003",input("guest@example.com"));
  assertThatThrownBy(()->service.create("guest-key-0004",input("guest@example.com"))).isInstanceOf(ApiException.class);
  assertThat(contacts.count()).isEqualTo(3);
 }
 @Test void publicReceiptDoesNotRevealPersonalDataAndManagementRequiresRole()throws Exception{
  String body="{\"fullName\":\"Nguyễn Minh Anh\",\"email\":\"guest2@example.com\",\"phone\":\"0901234567\",\"subject\":\"Hỏi về đổi size\",\"message\":\"Tôi muốn hỏi cách đổi size sau khi mua hàng\"}";
  mvc.perform(post("/api/support/guest").header("Idempotency-Key","guest-key-http-1").contentType("application/json").content(body))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.email").doesNotExist());
  mvc.perform(post("/api/support/guest").header("Idempotency-Key","guest-key-http-2").contentType("application/json").content("{}"))
   .andExpect(status().isBadRequest());
  mvc.perform(get("/api/support/guest/manage")).andExpect(status().isUnauthorized());
  String customer=jwt.issue(1,"customer",List.of("CUSTOMER"),false),staff=jwt.issue(5,"staff",List.of("STAFF"),false);
  mvc.perform(get("/api/support/guest/manage").header("Authorization","Bearer "+customer)).andExpect(status().isForbidden());
  mvc.perform(get("/api/support/guest/manage").header("Authorization","Bearer "+staff)).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].email").value("guest2@example.com"));
  long id=contacts.findAll().get(0).id;
  mvc.perform(put("/api/support/guest/manage/"+id).header("Authorization","Bearer "+staff).contentType("application/json").content("{\"state\":\"RESOLVED\",\"note\":\"Đã gọi điện hỗ trợ khách\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("RESOLVED"));
  mvc.perform(get("/api/support/guest/manage/"+id).header("Authorization","Bearer "+staff)).andExpect(status().isOk()).andExpect(jsonPath("$.staffNote").value("Đã gọi điện hỗ trợ khách"));
 }
 @Test void invalidWorkflowAndBlankPhoneNormalization(){
  long id=service.create("guest-key-normal",new Create("Nguyễn Minh Anh","guest3@example.com","","Hỏi về đổi size","Tôi muốn hỏi cách đổi size sau khi mua hàng")).id();
  assertThat(service.create("guest-key-normal",new Create("Nguyễn Minh Anh","guest3@example.com",null,"Hỏi về đổi size","Tôi muốn hỏi cách đổi size sau khi mua hàng")).id()).isEqualTo(id);
  assertThatThrownBy(()->service.update(id,5,new Update("RESOLVED",""))).isInstanceOf(ApiException.class);
  assertThat(service.update(id,5,new Update("IN_PROGRESS","Đang kiểm tra")).state()).isEqualTo("IN_PROGRESS");
  assertThat(service.update(id,5,new Update("RESOLVED","Đã trả lời qua email")).state()).isEqualTo("RESOLVED");
  assertThatThrownBy(()->service.update(id,5,new Update("RESOLVED","Nội dung khác"))).isInstanceOf(ApiException.class);
 }
}
