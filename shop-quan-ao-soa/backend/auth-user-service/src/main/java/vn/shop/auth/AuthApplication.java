package vn.shop.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"vn.shop.auth", "vn.shop.common"})
public class AuthApplication {
    public static void main(String[] args) { SpringApplication.run(AuthApplication.class, args); }
}
