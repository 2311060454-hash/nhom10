package vn.shop.catalog.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.shop.catalog.repository.ProductRepository;
import vn.shop.catalog.repository.CategoryRepository;
import java.util.List;

@RestController
public class CatalogReportingController {
 private final ProductRepository products;
 private final CategoryRepository categories;
 public CatalogReportingController(ProductRepository products,CategoryRepository categories){this.products=products;this.categories=categories;}
 @GetMapping("/internal/catalog/report-summary") public Map<String,Long> summary(){return Map.of("productCount",products.count());}
 public record CategoryRow(long id,String name){}
 public record ProductRow(long productId,long categoryId){}
 public record CategoryMap(List<CategoryRow> categories,List<ProductRow> products){}
 @GetMapping("/internal/catalog/report-categories") public CategoryMap reportCategories(){
  return new CategoryMap(categories.findAll().stream().map(c->new CategoryRow(c.id,c.name)).toList(),products.findAll().stream().map(p->new ProductRow(p.id,p.categoryId)).toList());
 }
}
