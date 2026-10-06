package vn.shop.reporting;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages={"vn.shop.reporting","vn.shop.common"})
public class ReportingApplication {public static void main(String[] args){SpringApplication.run(ReportingApplication.class,args);}}
