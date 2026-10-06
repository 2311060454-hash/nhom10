package vn.shop.order;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication(scanBasePackages={"vn.shop.order","vn.shop.common"}) @EnableScheduling
public class OrderApplication { public static void main(String[] args){SpringApplication.run(OrderApplication.class,args);} }
