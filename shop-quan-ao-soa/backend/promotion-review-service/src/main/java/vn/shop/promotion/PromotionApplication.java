package vn.shop.promotion;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication(scanBasePackages={"vn.shop.promotion","vn.shop.common"}) @EnableScheduling
public class PromotionApplication { public static void main(String[] args){SpringApplication.run(PromotionApplication.class,args);} }
