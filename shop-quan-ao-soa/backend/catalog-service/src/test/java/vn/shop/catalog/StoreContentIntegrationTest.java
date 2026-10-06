package vn.shop.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.shop.catalog.dto.StoreDtos.BannerInput;
import vn.shop.catalog.dto.StoreDtos.ContentInput;
import vn.shop.catalog.repository.BannerRepository;
import vn.shop.catalog.repository.StoreContentRepository;
import vn.shop.common.JwtService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class StoreContentIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper mapper;
 @Autowired JwtService jwt;
 @Autowired BannerRepository banners;
 @Autowired StoreContentRepository content;
 @MockitoBean RemoteSessionVerifier verifier;
 String admin, staff;
 @BeforeEach void setup(){
  banners.deleteAll(); content.deleteAll();
  when(verifier.valid(any())).thenReturn(true);
  admin="Bearer "+jwt.issue(1,"admin",List.of("ADMIN"),true);
  staff="Bearer "+jwt.issue(2,"staff",List.of("STAFF"),true);
 }
 BannerInput input(boolean active,Instant start,Instant end){
  return new BannerInput("Bộ sưu tập mới","Chất liệu thoáng mát","/",1,start,end,active);
 }
 @Test void publicOnlySeesActiveBannersInTimeWindow() throws Exception {
  Instant now=Instant.now();
  var active=input(true,now.minus(1,ChronoUnit.DAYS),now.plus(1,ChronoUnit.DAYS));
  mvc.perform(post("/api/catalog/manage/banners").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(active))).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/catalog/manage/banners").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(active))).andExpect(status().isForbidden());
  mvc.perform(post("/api/catalog/manage/banners").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(active))).andExpect(status().isCreated());
  mvc.perform(post("/api/catalog/manage/banners").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(input(false,now.minusSeconds(60),now.plusSeconds(60))))).andExpect(status().isCreated());
  mvc.perform(post("/api/catalog/manage/banners").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(input(true,now.plus(1,ChronoUnit.DAYS),now.plus(2,ChronoUnit.DAYS))))).andExpect(status().isCreated());
  mvc.perform(get("/api/catalog/banners")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(get("/api/catalog/manage/banners").header("Authorization",admin)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
 }
 @Test void rejectsExternalLinksAndInvalidTimeRange() throws Exception {
  Instant now=Instant.now();
  mvc.perform(post("/api/catalog/manage/banners").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
   .content(mapper.writeValueAsBytes(new BannerInput("Sai link","","https://example.com",1,now,now.plusSeconds(60),true)))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/catalog/manage/banners").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
   .content(mapper.writeValueAsBytes(input(true,now,now)))).andExpect(status().isBadRequest());
 }
 @Test void imageIsRealPngAndOnlyAdminCanUpload() throws Exception {
  Instant now=Instant.now();
  var created=mvc.perform(post("/api/catalog/manage/banners").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
   .content(mapper.writeValueAsBytes(input(true,now.minusSeconds(60),now.plusSeconds(60))))).andExpect(status().isCreated()).andReturn();
  long id=mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
  var out=new ByteArrayOutputStream();
  ImageIO.write(new BufferedImage(3,3,BufferedImage.TYPE_INT_RGB),"png",out);
  var file=new MockMultipartFile("file","banner.png","image/png",out.toByteArray());
  mvc.perform(multipart("/api/catalog/manage/banners/"+id+"/image").file(file).header("Authorization",staff)).andExpect(status().isForbidden());
  var uploaded=mvc.perform(multipart("/api/catalog/manage/banners/"+id+"/image").file(file).header("Authorization",admin)).andExpect(status().isOk()).andReturn();
  String url=mapper.readTree(uploaded.getResponse().getContentAsString()).get("imageUrl").asText();
  mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));
  mvc.perform(multipart("/api/catalog/manage/banners/"+id+"/image").file(new MockMultipartFile("file","fake.png","image/png","fake".getBytes())).header("Authorization",admin)).andExpect(status().isBadRequest());
  mvc.perform(delete("/api/catalog/manage/banners/"+id+"/image").header("Authorization",admin)).andExpect(status().isOk()).andExpect(jsonPath("$.imageUrl").doesNotExist());
  mvc.perform(get(url)).andExpect(status().isNotFound());
 }
 @Test void contentPersistsAndAdminOnlyCanEdit() throws Exception {
  var payload=mapper.writeValueAsBytes(new ContentInput("Liên hệ qua cửa hàng."));
  mvc.perform(put("/api/catalog/manage/content/CONTACT").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isUnauthorized());
  mvc.perform(put("/api/catalog/manage/content/CONTACT").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
  mvc.perform(put("/api/catalog/manage/content/CONTACT").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk()).andExpect(jsonPath("$.text").value("Liên hệ qua cửa hàng."));
  mvc.perform(get("/api/catalog/content")).andExpect(status().isOk()).andExpect(jsonPath("$[0].key").value("CONTACT")).andExpect(jsonPath("$[0].text").value("Liên hệ qua cửa hàng."));
  mvc.perform(put("/api/catalog/manage/content/NOT_A_KEY").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isNotFound());
 }
}
