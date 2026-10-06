package vn.shop.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.shop.common.PageResult;

/** Chỉ dành cho service nội bộ; snapshot không chứa địa chỉ hoặc thông tin người nhận. */
@RestController @Validated
public class OrderReportingController {
 private final OrderRepository orders;private final ReturnRepository returns;private final ReturnEventRepository returnEvents;
 public OrderReportingController(OrderRepository orders,ReturnRepository returns,ReturnEventRepository returnEvents){this.orders=orders;this.returns=returns;this.returnEvents=returnEvents;}
 public record Item(long productId,Long categoryId,String productName,int quantity,int returnedQuantity,BigDecimal unitPrice){}
 public record Event(String state,Instant createdAt){}
 public record ReturnNotice(long id,String returnId,String mode,String state,BigDecimal refundAmount,Instant createdAt){}
 public record Record(String id,long userId,String state,String paymentMethod,String paymentState,BigDecimal total,BigDecimal shippingFee,BigDecimal returnedAmount,Instant createdAt,List<Item> items,List<Event> events,List<ReturnNotice> returnEvents){}
 @GetMapping("/internal/orders/report") @Transactional(readOnly=true)
 public PageResult<Record> report(@RequestParam Instant from,@RequestParam Instant to,@RequestParam(required=false) Long userId,
  @RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="100") @Min(1) @Max(100) int size){
  if(!from.isBefore(to))throw new vn.shop.common.ApiException(400,"Khoảng thời gian không hợp lệ");
  var p=orders.report(from,to,userId,PageRequest.of(page,size,Sort.by("createdAt").descending().and(Sort.by("id").descending())));
  var mapped=p.map(o->{var request=returns.findByOrderId(o.id);var returned=request.filter(r->r.state.equals("REFUNDED")&&r.mode.equals("PARTIAL"));
   var notices=request.isEmpty()?List.<ReturnNotice>of():returnEvents.findByReturnIdOrderByIdAsc(request.get().id).stream()
    .map(e->new ReturnNotice(e.id,request.get().id,request.get().mode,e.state,request.get().refundAmount,e.createdAt)).toList();
   return new Record(o.id,o.userId,o.state,o.paymentMethod,o.paymentState,o.total,o.shippingFee,o.returnedAmount,o.createdAt,
   o.items.stream().map(i->new Item(i.productId,i.categoryId,i.productName,i.quantity,returned.map(r->r.items.stream().filter(x->x.variantId==i.variantId).mapToInt(x->x.quantity).sum()).orElse(0),i.unitPrice)).toList(),
   o.history.stream().map(h->new Event(h.state,h.createdAt)).toList(),notices);});
  return new PageResult<>(mapped.getContent(),mapped.getTotalElements(),mapped.getTotalPages(),page,size,mapped.isLast());
 }
}
