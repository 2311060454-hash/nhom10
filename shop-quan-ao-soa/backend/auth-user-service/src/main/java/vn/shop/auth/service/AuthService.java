package vn.shop.auth.service;

import java.time.Instant;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Value;
import vn.shop.auth.dto.AuthDtos.*;
import vn.shop.auth.entity.*;
import vn.shop.auth.repository.*;
import vn.shop.common.*;

@Service
public class AuthService implements SessionVerifier {
    private final UserRepository users;
    private final SessionRepository sessions;
    private final ResetRepository resets;
    private final AuditRepository audits;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Path mailbox;
    private final String dummyHash;
    public AuthService(UserRepository users, SessionRepository sessions, ResetRepository resets, AuditRepository audits,
            PasswordEncoder encoder, JwtService jwt, @Value("${app.mailbox-dir:.runtime/mail}") String mailbox) {
        this.users=users; this.sessions=sessions; this.resets=resets; this.audits=audits; this.encoder=encoder; this.jwt=jwt;
        this.mailbox=Path.of(mailbox); this.dummyHash=encoder.encode(UUID.randomUUID().toString());
    }
    @Transactional
    public UserView register(Register input) { return UserView.of(create(input, "CUSTOMER", false)); }
    @Transactional
    public UserView staff(Staff input) {
        var u=create(new Register(input.fullName(),input.email(),input.phone(),input.password()),"STAFF",input.inventoryWrite());
        audit(Caller.id(),"CREATE_STAFF",u.id); return UserView.of(u);
    }
    private User create(Register input, String role, boolean inventoryWrite) {
        String email=input.email().trim().toLowerCase(Locale.ROOT);
        if(users.existsByEmail(email)) throw new ApiException(409,"Email đã được sử dụng");
        User u=new User(); u.email=email; u.fullName=input.fullName().trim(); u.phone=input.phone().trim();
        checkPassword(input.password()); u.passwordHash=encoder.encode(input.password()); u.roles.add(role); u.inventoryWrite=inventoryWrite;
        return users.saveAndFlush(u);
    }
    @Transactional
    public LoginResult login(Login input) {
        var found=users.lockByEmail(input.email().trim().toLowerCase(Locale.ROOT));
        if(found.isEmpty()) { encoder.matches(input.password(),dummyHash); throw new ApiException(401,"Email hoặc mật khẩu không đúng"); }
        User u=found.get();
        if(!encoder.matches(input.password(),u.passwordHash) || !u.active) throw new ApiException(401,"Email hoặc mật khẩu không đúng hoặc tài khoản bị khóa");
        AuthSession s=new AuthSession(); s.id=UUID.randomUUID().toString(); s.userId=u.id; s.expiresAt=Instant.now().plusSeconds(1800); sessions.save(s);
        audit(u.id,"LOGIN",u.id);
        return new LoginResult(jwt.issue(u.id,s.id,new ArrayList<>(u.roles),u.inventoryWrite),"Bearer",1800,UserView.of(u));
    }
    @Override @Transactional(readOnly=true)
    public boolean valid(Jwt token) {
        return validSession(token.getId(),Long.parseLong(token.getSubject()));
    }
    @Transactional(readOnly=true)
    public boolean validSession(String id,long userId) {
        return sessions.findById(id).filter(s -> s.userId==userId && !s.revoked && s.expiresAt.isAfter(Instant.now()))
            .flatMap(s -> users.findById(s.userId)).filter(u -> u.active).isPresent();
    }
    @Transactional
    public void logout() {
        sessions.findById(Caller.jwt().getId()).ifPresent(s -> { s.revoked=true; sessions.save(s); });
        audit(Caller.id(),"LOGOUT",Caller.id());
    }
    @Transactional(readOnly=true)
    public UserView me() { return UserView.of(users.findById(Caller.id()).orElseThrow(ApiException::missing)); }
    @Transactional
    public UserView profile(Profile input) {
        User u=users.lock(Caller.id()).orElseThrow(ApiException::missing);
        u.fullName=input.fullName().trim(); u.phone=input.phone().trim();
        if(input.marketingConsent()&&!u.marketingConsent)u.marketingConsentAt=Instant.now();
        if(!input.marketingConsent())u.marketingConsentAt=null;
        u.marketingConsent=input.marketingConsent(); return UserView.of(users.save(u));
    }
    @Transactional(readOnly=true)
    public MarketingPreference marketing(long userId){
        User u=users.findById(userId).orElseThrow(ApiException::missing);
        boolean enabled=u.active&&u.roles.contains("CUSTOMER")&&u.marketingConsent&&u.marketingConsentAt!=null;
        return new MarketingPreference(enabled,enabled?u.marketingConsentAt:null);
    }
    @Transactional
    public void password(ChangePassword input) {
        User u=users.lock(Caller.id()).orElseThrow(ApiException::missing);
        if(!encoder.matches(input.currentPassword(),u.passwordHash)) throw new ApiException(400,"Mật khẩu hiện tại không đúng");
        checkPassword(input.newPassword()); u.passwordHash=encoder.encode(input.newPassword()); users.save(u); sessions.revokeAll(u.id); resets.invalidate(u.id);
        audit(u.id,"CHANGE_PASSWORD",u.id);
    }
    @Transactional
    public void forgot(Forgot input) {
        var found=users.lockByEmail(input.email().trim().toLowerCase(Locale.ROOT));
        if(found.isEmpty() || !found.get().active) return;
        User u=found.get();
        resets.invalidate(u.id);
        byte[] random=new byte[32]; new SecureRandom().nextBytes(random);
        String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        PasswordResetToken token=new PasswordResetToken(); token.tokenHash=hash(raw); token.userId=u.id; token.expiresAt=Instant.now().plusSeconds(900); resets.save(token);
        // Local delivery adapter: chỉ ghi file ngoài HTTP, không trả token trong API/log.
        try {
            Files.createDirectories(mailbox);
            Files.writeString(mailbox.resolve(UUID.randomUUID()+".txt"),"To: "+u.email+"\nToken đặt lại mật khẩu (15 phút): "+raw+"\nĐây là hộp thư local, không phải email đã gửi.\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
        } catch(java.io.IOException e) { throw new ApiException(503,"Không thể tạo thư đặt lại mật khẩu"); }
    }
    @Transactional
    public void reset(Reset input) {
        String tokenHash=hash(input.token());
        long ownerId=resets.owner(tokenHash).orElseThrow(() -> new ApiException(400,"Token không hợp lệ hoặc hết hạn"));
        // Giữ cùng thứ tự khóa với forgot/password: user trước, token sau.
        User u=users.lock(ownerId).orElseThrow(ApiException::missing);
        PasswordResetToken t=resets.lock(tokenHash).orElseThrow(() -> new ApiException(400,"Token không hợp lệ hoặc hết hạn"));
        if(t.used || !t.expiresAt.isAfter(Instant.now())) throw new ApiException(400,"Token không hợp lệ hoặc hết hạn");
        checkPassword(input.password()); u.passwordHash=encoder.encode(input.password()); users.save(u); t.used=true; resets.save(t); sessions.revokeAll(u.id);
        audit(u.id,"RESET_PASSWORD",u.id);
    }
    @Transactional
    public UserView access(long id,Access input) {
        User u=users.lock(id).orElseThrow(ApiException::missing);
        if(u.roles.contains("ADMIN")) throw new ApiException(409,"Không thể thay đổi quyền hoặc khóa quản trị viên bằng API này");
        u.roles=new HashSet<>(Set.of(input.role())); u.active=input.active(); u.inventoryWrite=input.role().equals("STAFF") && input.inventoryWrite();
        sessions.revokeAll(id); audit(Caller.id(),"CHANGE_ACCESS",id); return UserView.of(users.save(u));
    }
    public void audit(Long actor,String action,Long target) { AuditLog log=new AuditLog(); log.actorId=actor; log.action=action; log.targetId=target; audits.save(log); }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static void checkPassword(String password) {
        if(password.getBytes(StandardCharsets.UTF_8).length>72) throw new ApiException(400,"Mật khẩu tối đa 72 byte UTF-8");
    }
}
