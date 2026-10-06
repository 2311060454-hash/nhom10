package vn.shop.catalog.service;
import vn.shop.catalog.repository.ProductRepository;
import vn.shop.catalog.dto.CatalogDtos.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.*;
@Component @Profile("local")
public class CatalogSeed implements ApplicationRunner {
    private final CatalogService service;private final ProductRepository products;
    public CatalogSeed(CatalogService s,ProductRepository p){service=s;products=p;}
    @Override public void run(ApplicationArguments args){
        if(products.count()>0)return;
        var categories=service.categories(true);
        long men=categories.stream().filter(c -> c.name().equals("Thời trang nam")).map(CategoryView::id).findFirst().orElseGet(() -> service.category(null,new CategoryInput("Thời trang nam",null,true)).id());
        long women=service.categories(true).stream().filter(c -> c.name().equals("Thời trang nữ")).map(CategoryView::id).findFirst().orElseGet(() -> service.category(null,new CategoryInput("Thời trang nữ",null,true)).id());
        long brand=service.brands(true).stream().filter(b -> b.name().equals("Lụa Studio")).map(BrandView::id).findFirst().orElseGet(() -> service.brand(null,new BrandInput("Lụa Studio",true)).id());
        String[] names={"Áo thun cotton Essential","Sơ mi linen thanh lịch","Quần dáng suông Everyday","Áo polo Classic","Chân váy midi mềm mại","Áo kiểu cổ tròn"};
        for(int i=0;i<names.length;i++){
            List<VariantInput> variants=new ArrayList<>();
            for(String size:List.of("S","M","L","XL"))for(String color:List.of("Trắng","Đen"))variants.add(new VariantInput(null,"LUA-"+(i+1)+"-"+size+"-"+(color.equals("Trắng")?"W":"B"),size,color,new BigDecimal("120000"),BigDecimal.valueOf(250000+i*40000),i==0?new BigDecimal("199000"):null,true));
            service.save(null,new ProductInput(names[i],i<4?men:women,brand,"Thiết kế dễ phối, phù hợp mặc hằng ngày. Giặt nhẹ và phơi nơi thoáng mát.",i==1?"Linen":"Cotton","Dáng thoải mái",i<4?"NAM":"NU",true,i<2,null,variants));
        }
    }
}
