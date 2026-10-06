package vn.shop.inventory.service;
import vn.shop.inventory.repository.StockRepository;
import vn.shop.inventory.dto.InventoryDtos.Adjustment;
import vn.shop.common.InternalHttp;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component @Profile("local")
public class InventorySeed implements ApplicationRunner {
    private final StockRepository stocks;private final InventoryService service;private final InternalHttp http;
    public InventorySeed(StockRepository s,InventoryService i,InternalHttp h){stocks=s;service=i;http=h;}
    @Override public void run(ApplicationArguments args){if(stocks.count()>0)return;var page=http.get(8082,"/api/catalog/products?limit=100");for(var p:page.path("content"))for(var v:p.path("variants")){if(v.path("sku").asText().startsWith("LUA-")){long id=v.path("id").asLong();service.adjust("seed-stock-"+id,new Adjustment(id,"RECEIPT",30,"Nhập kho mẫu local"),0L);}}}
}
