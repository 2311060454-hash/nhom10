package vn.shop.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import vn.shop.auth.dto.AuthDtos.*;
import vn.shop.auth.service.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    private final AttemptLimiter limiter;
    public AuthController(AuthService service,AttemptLimiter limiter) { this.service=service; this.limiter=limiter; }
    @PostMapping("/register") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody Register input,HttpServletRequest req) { limiter.check("register:"+req.getRemoteAddr()); return service.register(input); }
    @PostMapping("/login") public LoginResult login(@Valid @RequestBody Login input,HttpServletRequest req) { limiter.check("login:"+req.getRemoteAddr()); return service.login(input); }
    @PostMapping("/logout") public Map<String,String> logout() { service.logout(); return Map.of("message","Đã đăng xuất"); }
    @GetMapping("/me") public UserView me() { return service.me(); }
    @PutMapping("/me") public UserView profile(@Valid @RequestBody Profile input) { return service.profile(input); }
    @PostMapping("/change-password") public Map<String,String> password(@Valid @RequestBody ChangePassword input) { service.password(input); return Map.of("message","Đã đổi mật khẩu. Vui lòng đăng nhập lại"); }
    @PostMapping("/forgot-password") public Map<String,String> forgot(@Valid @RequestBody Forgot input,HttpServletRequest req) { limiter.check("forgot:"+req.getRemoteAddr()); service.forgot(input); return Map.of("message","Nếu email tồn tại, hướng dẫn đã được tạo trong hộp thư local"); }
    @PostMapping("/reset-password") public Map<String,String> reset(@Valid @RequestBody Reset input,HttpServletRequest req) { limiter.check("reset:"+req.getRemoteAddr()); service.reset(input); return Map.of("message","Đã đặt lại mật khẩu"); }
}
