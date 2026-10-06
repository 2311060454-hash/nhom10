package vn.shop.catalog.service;
import vn.shop.catalog.entity.*;
import vn.shop.catalog.repository.*;
import vn.shop.catalog.dto.CatalogDtos.*;
import vn.shop.common.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.util.*;

@Service
public class CatalogService {
    private final ProductRepository products; private final VariantRepository variants; private final CategoryRepository categories;
    private final BrandRepository brands; private final ImageRepository images; private final InternalHttp internal;
    public CatalogService(ProductRepository p,VariantRepository v,CategoryRepository c,BrandRepository b,ImageRepository i,InternalHttp internal) { products=p;variants=v;categories=c;brands=b;images=i;this.internal=internal; }
    @Transactional(readOnly=true)
    public PageResult<ProductView> search(String q,Long categoryId,Long brandId,String gender,String size,String color,BigDecimal min,BigDecimal max,boolean sale,String sort,int page,int limit,boolean manage) {
        return search(q,categoryId,brandId,gender,size,color,min,max,sale,null,sort,page,limit,manage);
    }
    @Transactional(readOnly=true)
    public PageResult<ProductView> search(String q,Long categoryId,Long brandId,String gender,String size,String color,BigDecimal min,BigDecimal max,boolean sale,Boolean featured,String sort,int page,int limit,boolean manage) {
        if(min!=null && max!=null && min.compareTo(max)>0) throw new ApiException(400,"Khoảng giá không hợp lệ");
        Set<Long> categoryIds=null;
        if(categoryId!=null) {
            categoryIds=new HashSet<>();categoryIds.add(categoryId);
            var categoryTree=categories.findAll();
            boolean added;
            do {
                added=false;
                for(Category category:categoryTree) if(category.parentId!=null && categoryIds.contains(category.parentId)) added|=categoryIds.add(category.id);
            } while(added);
        }
        final Set<Long> selectedCategories=categoryIds;
        Specification<Product> spec=(root,query,cb) -> {
            var conditions=new ArrayList<Predicate>();
            if(!manage) conditions.add(cb.isTrue(root.get("active")));
            if(featured!=null && featured) conditions.add(cb.isTrue(root.get("featured")));
            if(!q.isBlank()) { String term="%"+q.toLowerCase(Locale.ROOT)+"%"; conditions.add(cb.or(cb.like(cb.lower(root.get("name")),term),cb.like(cb.lower(root.get("code")),term))); }
            if(selectedCategories!=null) conditions.add(root.get("categoryId").in(selectedCategories));
            if(brandId!=null) conditions.add(cb.equal(root.get("brandId"),brandId));
            if(!gender.isBlank()) conditions.add(cb.equal(root.get("gender"),gender));
            var sub=query.subquery(Long.class); var variant=sub.from(Variant.class);
            var vp=new ArrayList<Predicate>(); vp.add(cb.equal(variant.get("product"),root)); vp.add(cb.isTrue(variant.get("active")));
            if(!size.isBlank()) vp.add(cb.equal(variant.get("size"),size)); if(!color.isBlank()) vp.add(cb.equal(variant.get("color"),color));
            Expression<BigDecimal> effective=cb.coalesce(variant.<BigDecimal>get("salePrice"),variant.<BigDecimal>get("price"));
            if(min!=null) vp.add(cb.greaterThanOrEqualTo(effective,min)); if(max!=null) vp.add(cb.lessThanOrEqualTo(effective,max));
            if(sale) vp.add(cb.isNotNull(variant.get("salePrice")));
            sub.select(variant.get("id")).where(vp.toArray(Predicate[]::new));
            if(!manage || !size.isBlank() || !color.isBlank() || min!=null || max!=null || sale) conditions.add(cb.exists(sub));
            if((sort.equals("priceAsc") || sort.equals("priceDesc")) && query.getResultType()!=Long.class && query.getResultType()!=long.class) {
                var priceQuery=query.subquery(BigDecimal.class); var pv=priceQuery.from(Variant.class);
                priceQuery.select(cb.min(cb.coalesce(pv.<BigDecimal>get("salePrice"),pv.<BigDecimal>get("price"))))
                    .where(cb.equal(pv.get("product"),root),cb.isTrue(pv.get("active")));
                query.orderBy(sort.equals("priceAsc")?cb.asc(priceQuery):cb.desc(priceQuery),cb.desc(root.get("id")));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
        if(sort.equals("bestSelling")) {
            var matches=products.findAll(spec,PageRequest.of(0,10001));
            if(matches.getTotalElements()>10000)throw new ApiException(503,"Quá 10.000 sản phẩm phù hợp; hãy thu hẹp bộ lọc bán chạy");
            if(matches.isEmpty())return new PageResult<>(List.of(),0,0,page,limit,true);
            Map<Long,Long> quantities=new HashMap<>();
            for(var item:internal.get(8087,"/internal/reports/best-sellers"))quantities.put(item.path("productId").asLong(),item.path("quantity").asLong());
            var ranked=new ArrayList<>(matches.getContent());
            ranked.sort(Comparator.comparingLong((Product product)->quantities.getOrDefault(product.id,0L)).reversed().thenComparing(Comparator.comparingLong((Product product)->product.id).reversed()));
            int from=(int)Math.min(ranked.size(),(long)page*limit),to=Math.min(ranked.size(),from+limit);
            return new PageResult<>(ranked.subList(from,to).stream().map(p->view(p,manage)).toList(),ranked.size(),(ranked.size()+limit-1)/limit,page,limit,to==ranked.size());
        }
        var pageable=PageRequest.of(page,limit,sort.equals("newest")?Sort.by("id").descending():Sort.unsorted());
        var result=products.findAll(spec,pageable).map(p -> view(p,manage));
        return new PageResult<>(result.getContent(),result.getTotalElements(),result.getTotalPages(),page,limit,result.isLast());
    }
    @Transactional(readOnly=true) public ProductView detail(long id,boolean manage) { Product p=products.findById(id).orElseThrow(ApiException::missing); if(!manage && !p.active) throw ApiException.missing(); return view(p,manage); }
    @Transactional
    public ProductView save(Long id,ProductInput input) {
        categories.lockAll();
        brands.lock(input.brandId()).orElseThrow(ApiException::missing);
        Product p=id==null?new Product():products.lock(id).orElseThrow(ApiException::missing);
        if(id!=null && (input.version()==null || input.version()!=p.version)) throw new ApiException(409,"Sản phẩm đã thay đổi. Tải lại trước khi lưu");
        Category category=categories.findById(input.categoryId()).orElseThrow(ApiException::missing);
        Brand brand=brands.findById(input.brandId()).orElseThrow(ApiException::missing);
        if(input.active() && (!category.active || !brand.active)) throw new ApiException(409,"Danh mục hoặc thương hiệu đã ngừng sử dụng");
        if(input.active() && input.variants().stream().noneMatch(VariantInput::active)) throw new ApiException(400,"Cần ít nhất một biến thể hoạt động trước khi mở bán");
        Set<Long> seen=new HashSet<>(); Set<String> combinations=new HashSet<>(); Set<String> skus=new HashSet<>();
        for(var v:input.variants()) {
            if(v.salePrice()!=null && v.salePrice().compareTo(v.price())>0) throw new ApiException(400,"Giá khuyến mãi không được vượt giá bán");
            if(!combinations.add(v.size().trim().toLowerCase(Locale.ROOT)+"|"+v.color().trim().toLowerCase(Locale.ROOT)) || !skus.add(v.sku().toUpperCase(Locale.ROOT))) throw new ApiException(400,"Trùng SKU hoặc size/màu");
            if(v.id()!=null && !seen.add(v.id())) throw new ApiException(400,"Trùng ID biến thể");
        }
        for(Variant old:p.variants) if(!seen.contains(old.id)) throw new ApiException(409,"Không xóa biến thể đã tạo; hãy ngừng bán biến thể");
        if(id==null) p.code="SP-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(Locale.ROOT);
        p.name=input.name().trim();p.categoryId=input.categoryId();p.brandId=input.brandId();p.description=input.description().trim();p.material=input.material().trim();p.style=input.style().trim();p.gender=input.gender();p.active=input.active();p.featured=input.featured();
        for(var dto:input.variants()) {
            Variant v=dto.id()==null?new Variant():p.variants.stream().filter(x -> x.id.equals(dto.id())).findFirst().orElseThrow(() -> new ApiException(400,"Biến thể không thuộc sản phẩm"));
            if(dto.id()==null) {v.product=p;p.variants.add(v);}
            v.sku=dto.sku().toUpperCase(Locale.ROOT);v.size=dto.size().trim();v.color=dto.color().trim();v.costPrice=dto.costPrice();v.price=dto.price();v.salePrice=dto.salePrice();v.active=dto.active();
        }
        p.updatedAt=java.time.Instant.now();
        return view(products.saveAndFlush(p),true);
    }
    @Transactional public void deactivate(long id) { var p=products.lock(id).orElseThrow(ApiException::missing);p.active=false;products.save(p); }
    @Transactional(readOnly=true) public Quote quote(long id) {
        var v=variants.findById(id).orElseThrow(ApiException::missing);var p=(Product)org.hibernate.Hibernate.unproxy(v.product);
        var productImages=images.findByProductIdOrderByCreatedAt(p.id);
        String imageUrl=productImages.isEmpty()?null:"/api/catalog/images/"+productImages.get(0).id;
        return new Quote(v.id,p.id,p.categoryId,v.sku,p.name,v.size,v.color,v.salePrice==null?v.price:v.salePrice,p.active&&v.active,imageUrl);
    }
    @Transactional(readOnly=true) public List<CategoryView> categories(boolean manage) { return categories.findAll(Sort.by("name")).stream().filter(c -> manage||c.active).map(c -> new CategoryView(c.id,c.name,c.parentId,c.active)).toList(); }
    @Transactional(readOnly=true) public List<BrandView> brands(boolean manage) { return brands.findAll(Sort.by("name")).stream().filter(b -> manage||b.active).map(b -> new BrandView(b.id,b.name,b.active)).toList(); }
    @Transactional public CategoryView category(Long id,CategoryInput in) {
        categories.lockAll();Category c=id==null?new Category():categories.findById(id).orElseThrow(ApiException::missing);
        Long parent=in.parentId();Set<Long> chain=new HashSet<>();if(id!=null)chain.add(id);
        while(parent!=null) { if(!chain.add(parent)) throw new ApiException(400,"Danh mục cha tạo chu trình"); var next=categories.findById(parent).orElseThrow(ApiException::missing); if(in.active()&&!next.active)throw new ApiException(400,"Danh mục cha đang ngừng sử dụng");parent=next.parentId; }
        if(id!=null&&!in.active()&&(products.existsByCategoryIdAndActiveTrue(id)||categories.existsByParentIdAndActiveTrue(id))) throw new ApiException(409,"Ngừng bán sản phẩm và danh mục con trước");
        c.name=in.name().trim();c.parentId=in.parentId();c.active=in.active();categories.saveAndFlush(c);return new CategoryView(c.id,c.name,c.parentId,c.active);
    }
    @Transactional public BrandView brand(Long id,BrandInput in) {
        Brand b=id==null?new Brand():brands.lock(id).orElseThrow(ApiException::missing);
        if(id!=null&&!in.active()&&products.existsByBrandIdAndActiveTrue(id))throw new ApiException(409,"Ngừng bán sản phẩm thuộc thương hiệu trước");
        b.name=in.name().trim();b.active=in.active();brands.saveAndFlush(b);return new BrandView(b.id,b.name,b.active);
    }
    private ProductView view(Product p,boolean manage) {
        return new ProductView(p.id,p.code,p.name,p.categoryId,categories.findById(p.categoryId).orElseThrow(ApiException::missing).name,p.brandId,brands.findById(p.brandId).orElseThrow(ApiException::missing).name,p.description,p.material,p.style,p.gender,p.active,p.featured,p.version,p.createdAt,
            p.variants.stream().filter(v -> manage||v.active).map(v -> new VariantView(v.id,v.sku,v.size,v.color,v.price,v.salePrice,v.active,manage?v.costPrice:null)).toList(),
            images.findByProductIdOrderByCreatedAt(p.id).stream().map(i -> new ImageView(i.id,"/api/catalog/images/"+i.id)).toList());
    }
}


