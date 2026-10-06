param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$checks=[System.Collections.Generic.List[object]]::new()
function Req($method,$path,$body=$null,$token='',$key='') {
 $headers=@{};if($token){$headers.Authorization="Bearer $token"};if($key){$headers['Idempotency-Key']=$key}
 $args=@{Uri="$BaseUrl$path";Method=$method;Headers=$headers;ContentType='application/json; charset=utf-8';SkipHttpErrorCheck=$true;TimeoutSec=20}
 if($null-ne$body){$args.Body=[Text.Encoding]::UTF8.GetBytes(($body|ConvertTo-Json -Depth 10))}
 Invoke-WebRequest @args
}
function ReadJson($r){$r.Content|ConvertFrom-Json}
function Check($name,$r,$status){if([int]$r.StatusCode-ne$status){throw "$name HTTP $($r.StatusCode): $($r.Content)"};$checks.Add(@{name=$name;status='PASS'})}
function Assert($name,$valid){if(-not$valid){throw $name};$checks.Add(@{name=$name;status='PASS'})}
function AwaitOrder($id,$state,$token){for($i=0;$i-lt 50;$i++){$o=ReadJson (Req GET "/api/orders/$id" $null $token);if($o.state-eq$state-and-not$o.pendingCommandId){return $o};Start-Sleep -Milliseconds 500};throw "Đơn $id còn $($o.state), cần $state"}
function Fulfill($id,$action,$target,$token,$extra=@{}){$body=@{action=$action};foreach($k in $extra.Keys){$body[$k]=$extra[$k]};Check "$action nhận lệnh" (Req POST "/api/orders/$id/fulfillment" $body $token ([Guid]::NewGuid().ToString())) 202;$null=AwaitOrder $id $target $second}
$suffix=[Guid]::NewGuid().ToString('N').Substring(0,10).ToUpperInvariant()
$admin=(ReadJson (Req POST '/api/auth/login' @{email='admin@shop.local';password=$env:SEED_PASSWORD})).accessToken
$staff=(ReadJson (Req POST '/api/auth/login' @{email='staff@shop.local';password=$env:SEED_PASSWORD})).accessToken
Assert 'Đăng nhập ADMIN và STAFF' ($admin-and$staff)
$customers=@();foreach($n in 1..2){$email="promotion-$n-$suffix@example.com";$password=[Guid]::NewGuid().ToString('N');Check "Tạo khách $n" (Req POST '/api/auth/register' @{fullName="Khách $n";email=$email;phone='0901234567';password=$password}) 201;$customers+=,(ReadJson (Req POST '/api/auth/login' @{email=$email;password=$password})).accessToken}
$first=$customers[0];$second=$customers[1]
$categories=ReadJson (Req GET '/api/catalog/categories');$brands=ReadJson (Req GET '/api/catalog/brands')
$created=Req POST '/api/catalog/manage/products' @{name="Áo thử khuyến mãi $suffix";categoryId=$categories[0].id;brandId=$brands[0].id;description='Kiểm thử tích hợp';material='Cotton';style='Suông';gender='UNISEX';active=$true;featured=$false;variants=@(@{sku="PROMO-$suffix";size='M';color='Đen';costPrice=50000;price=100000;salePrice=$null;active=$true})} $admin
Check 'Tạo sản phẩm thử' $created 201;$product=ReadJson $created;$variant=$product.variants[0].id
Check 'Nhập kho thử' (Req POST '/api/inventory/adjustments' @{variantId=$variant;type='RECEIPT';quantity=5;reason='Smoke PromotionReview'} $admin "stock-$suffix") 200
$before=ReadJson (Req GET "/api/promotions/price?productId=$($product.id)&categoryId=$($product.categoryId)&price=100000")
Assert 'Chưa có giá khuyến mãi' ($before.price-eq 100000)
$now=[DateTime]::UtcNow
$promotion=Req POST '/api/promotions/manage' @{name="Giảm SP $suffix";targetType='PRODUCT';targetId=$product.id;type='PERCENT';value=10;maximumDiscount=$null;startsAt=$now.AddMinutes(-1).ToString('o');endsAt=$now.AddDays(1).ToString('o');active=$true} $admin
Check 'ADMIN tạo khuyến mãi' $promotion 200
$price=ReadJson (Req GET "/api/promotions/price?productId=$($product.id)&categoryId=$($product.categoryId)&price=100000")
Assert 'Giá giảm 10 phần trăm' ($price.price-eq 90000)
$code="SAVE-$suffix"
Check 'STAFF bị từ chối quản lý coupon' (Req GET '/api/coupons/manage' $null $staff) 403
Check 'ADMIN tạo coupon' (Req POST '/api/coupons/manage' @{code=$code;type='FIXED';value=15000;minimumTotal=50000;maximumDiscount=$null;totalLimit=1;perCustomerLimit=1;startsAt=$now.AddMinutes(-1).ToString('o');endsAt=$now.AddDays(1).ToString('o');active=$true} $admin) 200
Check 'Chưa mua không thể đánh giá' (Req POST '/api/reviews' @{productId=$product.id;stars=5;comment='Đánh giá thử'} $second) 403
Check 'Lưu yêu thích' (Req PUT "/api/wishlists/$($product.id)" $null $second) 200
Assert 'Chỉ chủ tài khoản thấy yêu thích' ((@(ReadJson (Req GET '/api/wishlists' $null $second)).Count-eq 1)-and(@(ReadJson (Req GET '/api/wishlists' $null $first)).Count-eq 0))
function MakeOrder($token){$cart=ReadJson (Req PUT "/api/cart/items/$variant" @{quantity=1} $token);$order=Req POST '/api/orders' @{cartRevision=$cart.revision;recipient='Khách kiểm thử';phone='0901234567';address='Địa chỉ local';note='Smoke PromotionReview';paymentMethod='COD';couponCode=$code} $token ([Guid]::NewGuid().ToString());Check 'Đặt hàng có coupon' $order 202;return (ReadJson $order).id}
$firstOrder=MakeOrder $first
$placed=AwaitOrder $firstOrder 'PLACED' $first
Assert 'Đơn lưu giá đã khuyến mãi và giảm coupon' ($placed.subtotal-eq 90000-and$placed.discount-eq 15000-and$placed.total-eq 105000-and$placed.items[0].unitPrice-eq 90000)
Check 'Coupon hết lượt khi đang giữ' (Req GET "/api/coupons/quote?code=$code&subtotal=90000" $null $second) 409
Check 'Hủy đơn đầu tiên' (Req POST "/api/orders/$firstOrder/state" @{state='CANCELLED'} $first) 200
$null=AwaitOrder $firstOrder 'CANCELLED' $first
Check 'Coupon được giải phóng khi hủy' (Req GET "/api/coupons/quote?code=$code&subtotal=90000" $null $second) 200
$secondOrder=MakeOrder $second
$placed=AwaitOrder $secondOrder 'PLACED' $second
Assert 'Đơn thứ hai giữ cùng coupon đúng một lần' ($placed.discount-eq 15000)
Check 'Không xem đơn người khác' (Req GET "/api/orders/$secondOrder" $null $first) 404
Check 'STAFF xác nhận' (Req POST "/api/orders/$secondOrder/state" @{state='CONFIRMED'} $staff) 200
Check 'STAFF đóng gói' (Req POST "/api/orders/$secondOrder/state" @{state='PACKING'} $staff) 200
Fulfill $secondOrder 'SHIP' 'SHIPPED' $staff @{carrier='Giao hàng local';tracking="PROMO-$suffix";assignee='Nhân viên kiểm thử';carrierCost=25000}
Fulfill $secondOrder 'DELIVER' 'DELIVERED' $staff
Fulfill $secondOrder 'COLLECT_COD' 'COMPLETED' $staff @{reference="COD-$suffix"}
Check 'Khách đã mua gửi đánh giá' (Req POST '/api/reviews' @{productId=$product.id;stars=5;comment='Áo đẹp và giao đúng hẹn'} $second) 200
$public=ReadJson (Req GET "/api/reviews/products/$($product.id)");Assert 'Chờ duyệt không công khai' ($public.content.Count-eq 0)
$mine=ReadJson (Req GET '/api/reviews/mine' $null $second);$review=$mine.content|Where-Object productId -eq $product.id|Select-Object -First 1
Check 'Không thể ẩn đánh giá của người khác' (Req DELETE "/api/reviews/$($review.id)" $null $first) 404
Check 'STAFF không duyệt đánh giá' (Req PUT "/api/reviews/manage/$($review.id)" @{state='APPROVED'} $staff) 403
Check 'ADMIN duyệt đánh giá' (Req PUT "/api/reviews/manage/$($review.id)" @{state='APPROVED'} $admin) 200
$public=ReadJson (Req GET "/api/reviews/products/$($product.id)");Assert 'Đánh giá đã duyệt được công khai' ($public.content.Count-eq 1)
Check 'Bỏ yêu thích' (Req DELETE "/api/wishlists/$($product.id)" $null $second) 204
Assert 'Danh sách yêu thích rỗng sau khi xóa' (@(ReadJson (Req GET '/api/wishlists' $null $second)).Count-eq 0)
$result=@{at=[DateTime]::UtcNow.ToString('o');checks=$checks;passed=$checks.Count;productId=$product.id;completedOrderId=$secondOrder;cancelledOrderId=$firstOrder}
$result|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $projectRoot 'docs/promotion-review-smoke-result.json') -Encoding UTF8
Write-Host "PASS $($checks.Count) kiểm tra PromotionReview trên MySQL qua Gateway."


