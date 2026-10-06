package vn.shop.catalog.service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import vn.shop.catalog.entity.Banner;
import vn.shop.catalog.repository.BannerRepository;
import vn.shop.catalog.dto.StoreDtos.*;
import vn.shop.common.*;

@Service
public class BannerService {
 private final BannerRepository banners;private final Path root;
 public BannerService(BannerRepository banners,@Value("${app.upload-dir}") String uploadDir){this.banners=banners;this.root=Path.of(uploadDir).toAbsolutePath().normalize().resolve("banners");}
 private BannerView view(Banner b){return new BannerView(b.id,b.title,b.subtitle,b.linkPath,b.imageId==null?null:"/api/catalog/banners/images/"+b.imageId,b.sortOrder,b.startsAt,b.endsAt,b.active,b.createdAt,b.updatedAt);}
 @Transactional(readOnly=true) public List<BannerView> active(){return banners.active(Instant.now()).stream().map(this::view).toList();}
 @Transactional(readOnly=true) public PageResult<BannerView> manage(int page){var p=banners.findAll(PageRequest.of(page,20,Sort.by("sortOrder").and(Sort.by("id"))));return new PageResult<>(p.map(this::view).getContent(),p.getTotalElements(),p.getTotalPages(),page,20,p.isLast());}
 @Transactional public BannerView save(Long id,BannerInput input){if(!input.startsAt().isBefore(input.endsAt()))throw new ApiException(400,"Ngày kết thúc phải sau ngày bắt đầu");
  Banner b=id==null?new Banner():banners.lock(id).orElseThrow(ApiException::missing);b.title=input.title().trim();b.subtitle=input.subtitle()==null?null:input.subtitle().trim();b.linkPath=input.linkPath();b.sortOrder=input.sortOrder();b.startsAt=input.startsAt();b.endsAt=input.endsAt();b.active=input.active();b.updatedAt=Instant.now();return view(banners.save(b));}
 @Transactional public BannerView upload(long bannerId,MultipartFile file){Banner b=banners.lock(bannerId).orElseThrow(ApiException::missing);byte[] encoded=BannerImageCodec.png(file);String old=b.imageId;String id=UUID.randomUUID()+".png";Path destination=root.resolve(id);
  try{Files.createDirectories(root);Files.write(destination,encoded,StandardOpenOption.CREATE_NEW);}catch(IOException e){throw new ApiException(500,"Không lưu được ảnh banner");}
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){try{if(status==STATUS_COMMITTED){if(old!=null)Files.deleteIfExists(root.resolve(old));}else Files.deleteIfExists(destination);}catch(IOException ignored){}}});
  b.imageId=id;b.updatedAt=Instant.now();banners.saveAndFlush(b);return view(b);
 }
 @Transactional public BannerView removeImage(long bannerId){Banner b=banners.lock(bannerId).orElseThrow(ApiException::missing);String old=b.imageId;if(old==null)return view(b);b.imageId=null;b.updatedAt=Instant.now();banners.saveAndFlush(b);TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){try{Files.deleteIfExists(root.resolve(old));}catch(IOException ignored){}}});return view(b);}
 @Transactional(readOnly=true) public byte[] image(String id){if(!id.matches("[a-f0-9-]{36}\\.png")||!banners.existsByImageId(id))throw ApiException.missing();try{return Files.readAllBytes(root.resolve(id));}catch(IOException e){throw ApiException.missing();}}
}
