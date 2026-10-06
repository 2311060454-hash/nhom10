package vn.shop.catalog.service;
import vn.shop.catalog.entity.ProductImage;
import vn.shop.catalog.repository.*;
import vn.shop.catalog.dto.CatalogDtos.ImageView;
import vn.shop.common.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {
    private final Path root;private final ImageRepository images;private final ProductRepository products;
    public ImageService(@Value("${app.upload-dir}") String root,ImageRepository i,ProductRepository p) {this.root=Path.of(root).toAbsolutePath().normalize();images=i;products=p;}
    @Transactional public ImageView upload(long productId,MultipartFile file) {
        products.lock(productId).orElseThrow(ApiException::missing);
        if(images.countByProductId(productId)>=10)throw new ApiException(400,"Tối đa 10 ảnh cho mỗi sản phẩm");
        if(file.isEmpty()||file.getSize()>5*1024*1024)throw new ApiException(400,"Ảnh phải từ 1 byte đến 5 MB");
        String filename=Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if(!(filename.endsWith(".png")||filename.endsWith(".jpg")||filename.endsWith(".jpeg")))throw new ApiException(400,"Chỉ chấp nhận ảnh JPEG hoặc PNG");
        if(!Set.of("image/png","image/jpeg").contains(Optional.ofNullable(file.getContentType()).orElse("")))throw new ApiException(400,"Định dạng ảnh không hợp lệ");
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new ApiException(400,"Tệp không phải ảnh hợp lệ");
            var reader=readers.next();
            byte[] encoded;
            try {
                reader.setInput(input,true,true);
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!Set.of("png","jpeg","jpg").contains(format))throw new ApiException(400,"Chỉ chấp nhận JPEG hoặc PNG");
                long pixels=(long)reader.getWidth(0)*reader.getHeight(0);
                if(pixels>6000000||pixels<1)throw new ApiException(400,"Ảnh tối đa 6 triệu điểm ảnh");
                var decoded=reader.read(0);var output=new ByteArrayOutputStream();ImageIO.write(decoded,"png",output);encoded=output.toByteArray();
                if(encoded.length>8*1024*1024)throw new ApiException(400,"Ảnh sau xử lý vượt 8 MB");
            } finally {reader.dispose();}
            String id=UUID.randomUUID()+".png";Files.createDirectories(root);Path destination=root.resolve(id);
            Files.write(destination,encoded,StandardOpenOption.CREATE_NEW);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(destination);}catch(IOException ignored){}}});
            ProductImage image=new ProductImage();image.id=id;image.productId=productId;image.mediaType="image/png";image.byteSize=encoded.length;images.saveAndFlush(image);
            return new ImageView(id,"/api/catalog/images/"+id);
        } catch(IOException e) {throw new ApiException(400,"Không đọc hoặc lưu được ảnh");}
    }
    @Transactional(readOnly=true) public byte[] read(String id) {
        if(!id.matches("[a-f0-9-]{36}\\.png"))throw ApiException.missing();
        images.findById(id).orElseThrow(ApiException::missing);
        try{return Files.readAllBytes(root.resolve(id));}catch(IOException e){throw ApiException.missing();}
    }
    @Transactional public void delete(long productId,String id) {
        products.lock(productId).orElseThrow(ApiException::missing);
        var image=images.findById(id).filter(i -> i.productId==productId).orElseThrow(ApiException::missing);images.delete(image);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){try{Files.deleteIfExists(root.resolve(image.id));}catch(IOException ignored){}}});
    }
}
