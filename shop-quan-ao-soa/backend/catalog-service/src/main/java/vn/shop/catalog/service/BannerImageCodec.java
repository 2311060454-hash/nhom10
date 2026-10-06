package vn.shop.catalog.service;

import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;
import vn.shop.common.ApiException;

final class BannerImageCodec {
 private BannerImageCodec(){}
 static byte[] png(MultipartFile file){
  if(file.isEmpty()||file.getSize()>5*1024*1024)throw new ApiException(400,"Ảnh banner phải từ 1 byte đến 5 MB");
  String filename=Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
  if(!(filename.endsWith(".png")||filename.endsWith(".jpg")||filename.endsWith(".jpeg")))throw new ApiException(400,"Chỉ chấp nhận ảnh JPEG hoặc PNG");
  if(!Set.of("image/png","image/jpeg").contains(Optional.ofNullable(file.getContentType()).orElse("")))throw new ApiException(400,"Định dạng ảnh không hợp lệ");
  try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))){
   var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new ApiException(400,"Tệp không phải ảnh hợp lệ");
   var reader=readers.next();try{reader.setInput(input,true,true);String format=reader.getFormatName().toLowerCase(Locale.ROOT);
    if(!Set.of("png","jpeg","jpg").contains(format))throw new ApiException(400,"Chỉ chấp nhận JPEG hoặc PNG");
    long pixels=(long)reader.getWidth(0)*reader.getHeight(0);if(pixels<1||pixels>6000000)throw new ApiException(400,"Ảnh tối đa 6 triệu điểm ảnh");
    var output=new ByteArrayOutputStream();ImageIO.write(reader.read(0),"png",output);byte[] encoded=output.toByteArray();
    if(encoded.length>8*1024*1024)throw new ApiException(400,"Ảnh sau xử lý vượt 8 MB");return encoded;
   }finally{reader.dispose();}
  }catch(IOException e){throw new ApiException(400,"Không đọc được ảnh banner");}
 }
}
