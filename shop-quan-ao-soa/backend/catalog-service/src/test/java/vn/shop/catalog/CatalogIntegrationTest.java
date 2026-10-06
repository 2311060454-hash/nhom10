package vn.shop.catalog;
import vn.shop.catalog.service.CatalogService;
import vn.shop.catalog.dto.CatalogDtos.*;
import vn.shop.catalog.repository.*;
import vn.shop.catalog.controller.CatalogReportingController;
import vn.shop.common.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class CatalogIntegrationTest {
 @Autowired CatalogService service;@Autowired ProductRepository products;@Autowired ImageRepository images;@Autowired CategoryRepository categories;@Autowired BrandRepository brands;@Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JwtService jwt;
 @Autowired CatalogReportingController reporting;
 @MockitoBean RemoteSessionVerifier verifier;@MockitoBean InternalHttp internal;
 long category,brand;String admin,staff;
 @BeforeEach void setup(){images.deleteAll();products.deleteAll();categories.deleteAll();brands.deleteAll();category=service.category(null,new CategoryInput("Áo",null,true)).id();brand=service.brand(null,new BrandInput("Lụa",true)).id();when(verifier.valid(any())).thenReturn(true);admin="Bearer "+jwt.issue(1,"test",List.of("ADMIN"),true);staff="Bearer "+jwt.issue(2,"test",List.of("STAFF"),true);}
 ProductInput input(List<VariantInput> list,boolean active,Long version){return new ProductInput("Áo cotton",category,brand,"Mô tả","Cotton","Suông","UNISEX",active,true,version,list);}
 VariantInput variant(String sku,String size){return new VariantInput(null,sku,size,"Đen",new BigDecimal("100000"),new BigDecimal("250000"),null,true);}
 @Test void createMultipleVariantsAndHideCostFromPublic()throws Exception{
  var p=service.save(null,input(List.of(variant("A-S","S"),variant("A-M","M")),true,null));
  mvc.perform(get("/api/catalog/products/"+p.id())).andExpect(status().isOk()).andExpect(jsonPath("variants.length()").value(2)).andExpect(jsonPath("variants[0].costPrice").isEmpty());
  mvc.perform(get("/api/catalog/manage/products/"+p.id()).header("Authorization",admin)).andExpect(status().isOk()).andExpect(jsonPath("variants[0].costPrice").value(100000));
 }
 @Test void reportingCategoryMapIncludesSavedProduct(){var p=service.save(null,input(List.of(variant("REPORT-S","S")),true,null));var map=reporting.reportCategories();assertThat(map.categories()).anySatisfy(c->{assertThat(c.id()).isEqualTo(category);assertThat(c.name()).isEqualTo("Áo");});assertThat(map.products()).anySatisfy(row->{assertThat(row.productId()).isEqualTo(p.id());assertThat(row.categoryId()).isEqualTo(category);});}
 @Test void customerAndStaffCannotWrite()throws Exception{
  String payload=mapper.writeValueAsString(input(List.of(variant("A-S","S")),true,null));
  mvc.perform(post("/api/catalog/manage/products").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/catalog/manage/products").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
 }
 @Test void refusesDuplicateSkuAndInvalidPrices(){
  service.save(null,input(List.of(variant("A-S","S")),true,null));
  assertThatThrownBy(() -> service.save(null,input(List.of(variant("A-S","S")),true,null))).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  var wrong=new VariantInput(null,"X","S","Đen",BigDecimal.ONE,BigDecimal.TEN,new BigDecimal("11"),true);
  assertThatThrownBy(() -> service.save(null,input(List.of(wrong),true,null))).isInstanceOf(ApiException.class);
 }
 @Test void cannotPublishWithoutActiveVariantAndCannotLoseVariantId(){
  var v=new VariantInput(null,"X","S","Đen",BigDecimal.ONE,BigDecimal.TEN,null,false);
  assertThatThrownBy(() -> service.save(null,input(List.of(v),true,null))).isInstanceOf(ApiException.class);
  var p=service.save(null,input(List.of(variant("A-S","S")),true,null));
  assertThatThrownBy(() -> service.save(p.id(),input(List.of(variant("B-M","M")),true,p.version()))).isInstanceOf(ApiException.class);
 }
 @Test void updateVersionAndSoftDeletePreserveVariantHistory()throws Exception{
  var p=service.save(null,input(List.of(variant("A-S","S")),true,null));var v=p.variants().get(0);
  var update=new VariantInput(v.id(),v.sku(),v.size(),v.color(),BigDecimal.ONE,new BigDecimal("300000"),null,true);
  service.save(p.id(),new ProductInput("Áo sửa",category,brand,"Mô tả","Cotton","Suông","UNISEX",true,false,p.version(),List.of(update)));
  assertThatThrownBy(() -> service.save(p.id(),input(List.of(update),true,p.version()))).isInstanceOf(ApiException.class);
  service.deactivate(p.id());mvc.perform(get("/api/catalog/products/"+p.id())).andExpect(status().isNotFound());assertThat(service.quote(v.id()).active()).isFalse();
 }
 @Test void filtersAndSortsPrice()throws Exception{
  service.save(null,input(List.of(variant("A-S","S")),true,null));
  service.save(null,input(List.of(new VariantInput(null,"B-M","M","Trắng",BigDecimal.ONE,new BigDecimal("100000"),null,true)),true,null));
  mvc.perform(get("/api/catalog/products?sort=priceAsc")).andExpect(status().isOk()).andExpect(jsonPath("content[0].variants[0].sku").value("B-M"));
  mvc.perform(get("/api/catalog/products?size=S&min=200000&max=300000")).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
 }
 @Test void bestSellingSortUsesOrderStatisticsBeforePagination()throws Exception{
  var first=service.save(null,input(List.of(variant("TOP-A","S")),true,null));
  var second=service.save(null,input(List.of(variant("TOP-B","M")),true,null));
  when(internal.get(8087,"/internal/reports/best-sellers")).thenReturn(mapper.readTree("[{\"productId\":"+first.id()+",\"quantity\":8},{\"productId\":"+second.id()+",\"quantity\":2}]"));
  mvc.perform(get("/api/catalog/products").param("sort","bestSelling").param("limit","1")).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(2)).andExpect(jsonPath("content[0].id").value(first.id()));
  mvc.perform(get("/api/catalog/products").param("sort","bestSelling").param("limit","1").param("page","1")).andExpect(status().isOk()).andExpect(jsonPath("content[0].id").value(second.id()));
  mvc.perform(get("/api/catalog/products").param("sort","bestSelling").param("q","không có")).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(0));
  when(internal.get(8087,"/internal/reports/best-sellers")).thenThrow(new ApiException(503,"Reporting không phản hồi"));
  mvc.perform(get("/api/catalog/products").param("sort","bestSelling")).andExpect(status().isServiceUnavailable());
 }
 @Test void categoryCyclesAndDisablingInUseRejected(){
  var child=service.category(null,new CategoryInput("Áo con",category,true));
  assertThatThrownBy(() -> service.category(category,new CategoryInput("Áo",child.id(),true))).isInstanceOf(ApiException.class);
  service.save(null,input(List.of(variant("A-S","S")),true,null));assertThatThrownBy(() -> service.brand(brand,new BrandInput("Lụa",false))).isInstanceOf(ApiException.class);
 }
 @Test void parentCategoryFilterIncludesAllDescendantsButNotSiblings()throws Exception{
  var child=service.category(null,new CategoryInput("Áo nam",category,true));
  var grandchild=service.category(null,new CategoryInput("Áo nam mùa hè",child.id(),true));
  var sibling=service.category(null,new CategoryInput("Phụ kiện",null,true));
  service.save(null,input(List.of(variant("PARENT-S","S")),true,null));
  service.save(null,new ProductInput("Áo nam",child.id(),brand,"Mô tả","Cotton","Suông","NAM",true,false,null,List.of(variant("CHILD-S","S"))));
  service.save(null,new ProductInput("Áo mùa hè",grandchild.id(),brand,"Mô tả","Cotton","Suông","NAM",true,false,null,List.of(variant("GRANDCHILD-S","S"))));
  service.save(null,new ProductInput("Túi",sibling.id(),brand,"Mô tả","Cotton","Suông","UNISEX",true,false,null,List.of(variant("SIBLING-S","S"))));
  mvc.perform(get("/api/catalog/products").param("categoryId",String.valueOf(category))).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(3));
  mvc.perform(get("/api/catalog/products").param("categoryId",String.valueOf(child.id()))).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(2));
  mvc.perform(get("/api/catalog/products").param("categoryId",String.valueOf(grandchild.id()))).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
 }
 @Test void uploadReencodesAndRejectsFakeImage()throws Exception{
  var p=service.save(null,input(List.of(variant("A-S","S")),true,null));
  var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB),"png",out);
  var result=mvc.perform(multipart("/api/catalog/manage/products/"+p.id()+"/images").file(new MockMultipartFile("file","image.png","image/png",out.toByteArray())).header("Authorization",admin)).andExpect(status().isOk()).andReturn();
  String url=mapper.readTree(result.getResponse().getContentAsString()).get("url").asText();mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));
  assertThat(service.quote(p.variants().get(0).id()).imageUrl()).isEqualTo(url);
  mvc.perform(multipart("/api/catalog/manage/products/"+p.id()+"/images").file(new MockMultipartFile("file","image.png","image/png","not-image".getBytes())).header("Authorization",admin)).andExpect(status().isBadRequest());
 }
}
