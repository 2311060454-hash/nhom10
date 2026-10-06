package vn.shop.reporting;

import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.shop.common.*;
import vn.shop.reporting.SupportDtos.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class SupportIntegrationTest {
 @Autowired SupportService service;@Autowired SupportGuardRepository guards;@Autowired SupportTicketRepository tickets;
 @Autowired SupportMessageRepository messages;@Autowired SupportHistoryRepository history;@Autowired MockMvc mvc;@Autowired JwtService jwt;
 @MockitoBean RemoteSessionVerifier verifier;
 @BeforeEach void setup(){history.deleteAll();messages.deleteAll();tickets.deleteAll();if(!guards.existsById(1L)){SupportGuard g=new SupportGuard();g.id=1L;guards.save(g);}when(verifier.valid(any())).thenReturn(true);}
 @Test void creationRetryDoesNotDuplicateAndDifferentPayloadConflicts(){var a=service.create(1,"support-key-0001",new Create("Không nhận được hàng","Xin kiểm tra tình trạng giao hàng giúp tôi"));var b=service.create(1,"support-key-0001",new Create("Không nhận được hàng","Xin kiểm tra tình trạng giao hàng giúp tôi"));assertThat(a.ticket().id()).isEqualTo(b.ticket().id());assertThat(messages.count()).isEqualTo(1);assertThatThrownBy(()->service.create(1,"support-key-0001",new Create("Hàng hỏng rồi","Vui lòng đổi hàng giúp tôi"))).isInstanceOf(ApiException.class);}
 @Test void ownerIsolationAndStaffWorkflow(){long id=service.create(1,"support-key-0002",new Create("Cần hỗ trợ đơn hàng","Đơn hàng của tôi giao chậm nhiều ngày")).ticket().id();assertThatThrownBy(()->service.detail(id,2,false)).isInstanceOf(ApiException.class);assertThat(service.list(2L,null,null,0).content()).isEmpty();service.change(id,5,true,"IN_PROGRESS");service.reply(id,5,"STAFF","reply-key-0001",new Reply("Chúng tôi đang kiểm tra với đơn vị vận chuyển"));assertThat(service.detail(id,1,false).ticket().state()).isEqualTo("WAITING_CUSTOMER");service.reply(id,1,"CUSTOMER","reply-key-0002",new Reply("Cảm ơn, vui lòng cập nhật khi có tin mới"));assertThat(service.detail(id,1,false).ticket().state()).isEqualTo("IN_PROGRESS");service.change(id,5,true,"RESOLVED");service.change(id,1,false,"CLOSED");assertThat(service.detail(id,1,false).history()).hasSize(6);assertThatThrownBy(()->service.reply(id,1,"CUSTOMER","reply-key-0003",new Reply("Tôi muốn nhắn thêm"))).isInstanceOf(ApiException.class);}
 @Test void replyRetryAndInvalidStateDoNotDuplicateHistory(){long id=service.create(1,"support-key-0003",new Create("Cần hỗ trợ size áo","Xin tư vấn đổi size áo đã mua")).ticket().id();service.reply(id,5,"STAFF","reply-key-0010",new Reply("Vui lòng cung cấp size mong muốn"));service.reply(id,5,"STAFF","reply-key-0010",new Reply("Vui lòng cung cấp size mong muốn"));assertThat(messages.findByTicketIdOrderByCreatedAtAscIdAsc(id)).hasSize(2);assertThat(history.findByTicketIdOrderByCreatedAtAscIdAsc(id)).hasSize(2);assertThatThrownBy(()->service.reply(id,5,"STAFF","reply-key-0010",new Reply("Nội dung khác"))).isInstanceOf(ApiException.class);assertThatThrownBy(()->service.change(id,1,false,"IN_PROGRESS")).isInstanceOf(ApiException.class);assertThatThrownBy(()->service.change(id,5,true,"CLOSED")).isInstanceOf(ApiException.class);}
 @Test void backendRolesProtectManagementAndCreate()throws Exception{String customer=jwt.issue(1,"customer",List.of("CUSTOMER"),false),staff=jwt.issue(5,"staff",List.of("STAFF"),false);mvc.perform(get("/api/support/manage").header("Authorization","Bearer "+customer)).andExpect(status().isForbidden());mvc.perform(post("/api/support").header("Authorization","Bearer "+staff).header("Idempotency-Key","support-key-0004").contentType("application/json").content("{\"subject\":\"Cần trợ giúp\",\"message\":\"Xin kiểm tra hộ tôi\"}")).andExpect(status().isForbidden());mvc.perform(get("/api/support/manage").header("Authorization","Bearer "+staff)).andExpect(status().isOk());mvc.perform(get("/api/support")).andExpect(status().isUnauthorized());}
 @Test void concurrentSameKeyCreatesOneTicket()throws Exception{var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);Callable<Long> task=()->{gate.await();return service.create(1,"support-key-race",new Create("Cần kiểm tra đơn","Tôi đang chờ thông tin của đơn hàng")).ticket().id();};try{var a=pool.submit(task);var b=pool.submit(task);gate.countDown();assertThat(a.get(10,TimeUnit.SECONDS)).isEqualTo(b.get(10,TimeUnit.SECONDS));assertThat(tickets.count()).isEqualTo(1);assertThat(messages.count()).isEqualTo(1);}finally{pool.shutdownNow();}}
}
