package vn.shop.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages={"vn.shop.gateway","vn.shop.common"})
public class GatewayApplication {
    public static void main(String[] args) { SpringApplication.run(GatewayApplication.class,args); }
}
