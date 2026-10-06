package vn.shop.payment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages={"vn.shop.payment","vn.shop.common"})
public class PaymentApplication { public static void main(String[] args){SpringApplication.run(PaymentApplication.class,args);} }
