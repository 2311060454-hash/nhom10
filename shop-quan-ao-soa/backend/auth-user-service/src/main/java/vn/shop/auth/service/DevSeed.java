package vn.shop.auth.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.shop.auth.repository.UserRepository;
import vn.shop.auth.entity.User;

@Component @Profile("local")
public class DevSeed implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String password;
    public DevSeed(UserRepository users,PasswordEncoder encoder,@Value("${app.seed-password}") String password) { this.users=users; this.encoder=encoder; this.password=password; }
    @Override @Transactional public void run(ApplicationArguments args) {
        if(password.length()<10 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new IllegalArgumentException("SEED_PASSWORD phải có ít nhất 10 ký tự và tối đa 72 byte");
        seed("admin@shop.local","Quản trị viên","ADMIN"); seed("staff@shop.local","Nhân viên","STAFF"); seed("customer@shop.local","Khách hàng mẫu","CUSTOMER");
    }
    private void seed(String email,String name,String role) {
        if(users.existsByEmail(email)) return;
        User u=new User(); u.email=email; u.fullName=name; u.phone="0900000000"; u.passwordHash=encoder.encode(password); u.roles.add(role); u.inventoryWrite=role.equals("STAFF"); users.save(u);
    }
}
