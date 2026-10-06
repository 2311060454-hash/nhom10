package vn.shop.catalog.service;

import java.util.*;
import java.time.Instant;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.shop.catalog.repository.StoreContentRepository;
import vn.shop.catalog.entity.StoreContent;
import vn.shop.catalog.dto.StoreDtos.*;
import vn.shop.common.ApiException;

@Service
public class StoreContentService {
 public static final Set<String> KEYS=Set.of("ABOUT","CONTACT","PURCHASE_POLICY","RETURN_POLICY","SHIPPING_POLICY","ANNOUNCEMENT");
 private final StoreContentRepository content;
 public StoreContentService(StoreContentRepository content){this.content=content;}
 private ContentView view(StoreContent row){return new ContentView(row.contentKey,row.contentText,row.updatedAt);}
 @Transactional(readOnly=true) public List<ContentView> list(){return content.findAll(Sort.by("contentKey")).stream().map(this::view).toList();}
 @Transactional public ContentView update(String key,ContentInput input){if(!KEYS.contains(key))throw ApiException.missing();StoreContent row=content.findById(key).orElseGet(()->{StoreContent created=new StoreContent();created.contentKey=key;created.contentText="";return created;});row.contentText=input.text().trim();row.updatedAt=Instant.now();return view(content.save(row));}
}
