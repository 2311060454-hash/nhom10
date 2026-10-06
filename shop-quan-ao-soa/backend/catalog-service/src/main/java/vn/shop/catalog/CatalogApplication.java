package vn.shop.catalog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages={"vn.shop.catalog","vn.shop.common"})
public class CatalogApplication { public static void main(String[] args) { SpringApplication.run(CatalogApplication.class,args); } }
