package vn.shop.catalog.controller;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;
import vn.shop.catalog.service.*;
import vn.shop.catalog.dto.StoreDtos.*;
import vn.shop.common.*;

@RestController @Validated
public class StoreContentController {
 private final BannerService banners;private final StoreContentService content;
 public StoreContentController(BannerService banners,StoreContentService content){this.banners=banners;this.content=content;}
 @GetMapping("/api/catalog/banners") public List<BannerView> active(){return banners.active();}
 @GetMapping("/api/catalog/manage/banners") @PreAuthorize("hasRole('ADMIN')") public PageResult<BannerView> manage(@RequestParam(defaultValue="0") @Min(0) int page){return banners.manage(page);}
 @PostMapping("/api/catalog/manage/banners") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED) public BannerView create(@Valid @RequestBody BannerInput input){return banners.save(null,input);}
 @PutMapping("/api/catalog/manage/banners/{id}") @PreAuthorize("hasRole('ADMIN')") public BannerView update(@PathVariable long id,@Valid @RequestBody BannerInput input){return banners.save(id,input);}
 @PostMapping(value="/api/catalog/manage/banners/{id}/image",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasRole('ADMIN')") public BannerView upload(@PathVariable long id,@RequestParam("file") MultipartFile file){return banners.upload(id,file);}
 @DeleteMapping("/api/catalog/manage/banners/{id}/image") @PreAuthorize("hasRole('ADMIN')") public BannerView remove(@PathVariable long id){return banners.removeImage(id);}
 @GetMapping("/api/catalog/banners/images/{id}") public ResponseEntity<byte[]> image(@PathVariable String id){return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff").body(banners.image(id));}
 @GetMapping("/api/catalog/content") public List<ContentView> content(){return content.list();}
 @PutMapping("/api/catalog/manage/content/{key}") @PreAuthorize("hasRole('ADMIN')") public ContentView updateContent(@PathVariable String key,@Valid @RequestBody ContentInput input){return content.update(key,input);}
 @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> tooLarge(){return ResponseEntity.status(413).body(Errors.body(413,"Ảnh tối đa 5 MB"));}
}
