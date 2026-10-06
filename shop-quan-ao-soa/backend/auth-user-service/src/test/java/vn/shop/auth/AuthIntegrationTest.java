package vn.shop.auth;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.http.MediaType;
import vn.shop.auth.repository.*;
import vn.shop.auth.entity.*;
import vn.shop.auth.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired SessionRepository sessions;
    @Autowired ResetRepository resets;
    @Autowired AddressRepository addresses;
    @Autowired AuditRepository audits;
    @Autowired PasswordEncoder encoder;
    private static final String PASSWORD="Test-password-123";
    @BeforeEach void clear() { addresses.deleteAll(); resets.deleteAll(); sessions.deleteAll(); audits.deleteAll(); users.deleteAll(); }
    String json(Object value) throws Exception { return mapper.writeValueAsString(value); }
    User seed(String email,String role) { User u=new User(); u.email=email; u.fullName="Nguyễn Văn An"; u.phone="0901234567"; u.passwordHash=encoder.encode(PASSWORD); u.roles.add(role); return users.save(u); }
    String login(String email) throws Exception {
        var result=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email",email,"password",PASSWORD))))
            .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
    @Test void registrationHashesPasswordAndCannotEscalateRole() throws Exception {
        var input=new HashMap<String,Object>(Map.of("fullName","Nguyễn An","email","an@example.com","phone","0901234567","password",PASSWORD));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(input)))
            .andExpect(status().isCreated()).andExpect(jsonPath("roles[0]").value("CUSTOMER")).andExpect(jsonPath("passwordHash").doesNotExist());
        assertThat(encoder.matches(PASSWORD,users.findByEmail("an@example.com").orElseThrow().passwordHash)).isTrue();
        input.put("email","b@example.com"); input.put("role","ADMIN");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(input))).andExpect(status().isBadRequest());
    }
    @Test void duplicateEmailAndValidation() throws Exception {
        seed("an@example.com","CUSTOMER");
        var input=Map.of("fullName","An","email","an@example.com","phone","0901234567","password",PASSWORD);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(input))).andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }
    @Test void logoutRevokesToken() throws Exception {
        seed("an@example.com","CUSTOMER"); String token=login("an@example.com");
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void marketingConsentIsOptInAndCanBeWithdrawn() throws Exception {
        User customer=seed("marketing@example.com","CUSTOMER");String token=login(customer.email);
        var preference=users.findById(customer.id).orElseThrow();
        assertThat(preference.marketingConsent).isFalse();assertThat(preference.marketingConsentAt).isNull();
        mvc.perform(put("/api/auth/me").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("fullName","Khách thử","phone","0901234567","marketingConsent",true)))).andExpect(status().isOk()).andExpect(jsonPath("marketingConsent").value(true));
        Instant since=users.findById(customer.id).orElseThrow().marketingConsentAt;assertThat(since).isNotNull();
        mvc.perform(put("/api/auth/me").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("fullName","Khách đổi tên","phone","0901234567","marketingConsent",true)))).andExpect(status().isOk());
        assertThat(users.findById(customer.id).orElseThrow().marketingConsentAt).isEqualTo(since);
        mvc.perform(put("/api/auth/me").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("fullName","Khách thử","phone","0901234567","marketingConsent",false)))).andExpect(status().isOk());
        assertThat(users.findById(customer.id).orElseThrow().marketingConsentAt).isNull();
        mvc.perform(get("/internal/users/"+customer.id+"/marketing-preference")).andExpect(status().isUnauthorized());
    }
    @Test void staffAndCustomerCannotManageAccounts() throws Exception {
        for(String role:List.of("STAFF","CUSTOMER")) {
            String email=role.toLowerCase()+"@example.com"; seed(email,role); String token=login(email);
            mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        }
        seed("admin@example.com","ADMIN");
        mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+login("admin@example.com"))).andExpect(status().isOk()).andExpect(jsonPath("content[0].passwordHash").doesNotExist());
    }
    @Test void ownerCannotModifyOtherPersonsAddress() throws Exception {
        seed("a@example.com","CUSTOMER"); seed("b@example.com","CUSTOMER"); String a=login("a@example.com"),b=login("b@example.com");
        String input=json(Map.of("recipient","An","phone","0901234567","detail","12 Nguyễn Trãi, Hà Nội","defaultAddress",true));
        var res=mvc.perform(post("/api/addresses").header("Authorization","Bearer "+a).contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isCreated()).andReturn();
        long id=mapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(put("/api/addresses/"+id).header("Authorization","Bearer "+b).contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/addresses/"+id).header("Authorization","Bearer "+b)).andExpect(status().isNotFound());
        mvc.perform(get("/api/addresses").header("Authorization","Bearer "+b)).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void passwordChangeRevokesAllSessions() throws Exception {
        seed("a@example.com","CUSTOMER"); String one=login("a@example.com"),two=login("a@example.com");
        mvc.perform(post("/api/auth/change-password").header("Authorization","Bearer "+one).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("currentPassword",PASSWORD,"newPassword","New-password-456")))).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+two)).andExpect(status().isUnauthorized());
    }
    @Test void resetTokenIsSingleUseAndExpires() throws Exception {
        User u=seed("a@example.com","CUSTOMER");
        PasswordResetToken t=new PasswordResetToken(); t.tokenHash=AuthService.hash("reset-test"); t.userId=u.id; t.expiresAt=Instant.now().plusSeconds(60); resets.save(t);
        String input=json(Map.of("token","reset-test","password","New-password-456"));
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isBadRequest());
        t=new PasswordResetToken(); t.tokenHash=AuthService.hash("expired-test"); t.userId=u.id; t.expiresAt=Instant.now().minusSeconds(1); resets.save(t);
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("token","expired-test","password",PASSWORD)))).andExpect(status().isBadRequest());
    }
    @Test void adminLockInvalidatesExistingSession() throws Exception {
        User customer=seed("a@example.com","CUSTOMER"); seed("admin@example.com","ADMIN");
        String token=login("a@example.com"),admin=login("admin@example.com");
        mvc.perform(put("/api/admin/users/"+customer.id+"/access").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("role","CUSTOMER","active",false,"inventoryWrite",false)))).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void internalEndpointsRequireSeparateKey() throws Exception {
        mvc.perform(get("/internal/sessions/missing?userId=1")).andExpect(status().isUnauthorized());
        seed("a@example.com","CUSTOMER");
        mvc.perform(get("/internal/sessions/missing?userId=1").header("Authorization","Bearer "+login("a@example.com"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/sessions/missing?userId=1").header("X-Internal-Key","test-only-internal-key-at-least-32-bytes-long")).andExpect(status().isOk()).andExpect(jsonPath("valid").value(false));
    }
    @Test void corsAllowsBothLocalOriginsAndRejectsOthers() throws Exception {
        for(String origin:List.of("http://localhost:5173","http://127.0.0.1:5173")) {
            mvc.perform(options("/api/auth/login").header("Origin",origin).header("Access-Control-Request-Method","POST"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin",origin));
        }
        mvc.perform(options("/api/auth/login").header("Origin","https://untrusted.example").header("Access-Control-Request-Method","POST"))
            .andExpect(status().isForbidden());
    }
    @Test void concurrentResetConsumesTokenExactlyOnce() throws Exception {
        User u=seed("race@example.com","CUSTOMER");
        PasswordResetToken t=new PasswordResetToken(); t.tokenHash=AuthService.hash("concurrent-reset"); t.userId=u.id; t.expiresAt=Instant.now().plusSeconds(60); resets.save(t);
        String input=json(Map.of("token","concurrent-reset","password","New-password-789"));
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var gate=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Integer> call=() -> { gate.await(); return mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(input)).andReturn().getResponse().getStatus(); };
        try {
            var first=pool.submit(call); var second=pool.submit(call); gate.countDown();
            assertThat(List.of(first.get(10,java.util.concurrent.TimeUnit.SECONDS),second.get(10,java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,400);
        } finally { pool.shutdownNow(); }
    }
}
