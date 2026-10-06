package vn.shop.catalog.controller;
import vn.shop.catalog.service.ImageService;
import vn.shop.catalog.dto.CatalogDtos.ImageView;
import vn.shop.common.Errors;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;
import org.springframework.security.access.prepost.PreAuthorize;
@RestController
public class ImageController {
    private final ImageService service;
    public ImageController(ImageService service){this.service=service;}
    @PostMapping(value="/api/catalog/manage/products/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasRole('ADMIN')")
    public ImageView upload(@PathVariable long id,@RequestParam("file") MultipartFile file){return service.upload(id,file);}
    @GetMapping("/api/catalog/images/{id}") public ResponseEntity<byte[]> read(@PathVariable String id){return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff").body(service.read(id));}
    @DeleteMapping("/api/catalog/manage/products/{productId}/images/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long productId,@PathVariable String id){service.delete(productId,id);}
    @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> tooLarge(){return ResponseEntity.status(413).body(Errors.body(413,"Ảnh tối đa 5 MB"));}
}
