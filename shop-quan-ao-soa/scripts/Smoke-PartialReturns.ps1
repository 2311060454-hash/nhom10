param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$checks=[Collections.Generic.List[object]]::new()
function Req($method,$path,$body=$null,$token='',$key=''){
 $headers=@{};if($token){$headers.Authorization="Bearer $token"};if($key){$headers['Idempotency-Key']=$key}
 $args=@{Uri="$BaseUrl$path";Method=$method;Headers=$headers;ContentType='application/json; charset=utf-8';SkipHttpErrorCheck=$true;TimeoutSec=20}
 if($null-ne$body){$args.Body=[Text.Encoding]::UTF8.GetBytes(($body|ConvertTo-Json -Depth 10))}
 Invoke-WebRequest @args
}
function Json($response){$response.Content|ConvertFrom-Json}
function Check($name,$response,$status){if([int]$response.StatusCode-ne$status){throw "$name HTTP $($response.StatusCode): $($response.Content)"};$checks.Add(@{name=$name;status='PASS'})}
function Assert($name,$condition){if(-not$condition){throw $name};$checks.Add(@{name=$name;status='PASS'})}
function AwaitOrder($id,$state){for($i=0;$i-lt 80;$i++){$o=Json (Req GET "/api/orders/$id" $null $customer);if($o.state-eq$state-and-not$o.pendingCommandId){return $o};Start-Sleep -Milliseconds 500};throw "Đơn $id chưa tới $state"}
function AwaitReturn($id,$state){for($i=0;$i-lt 80;$i++){$r=Json (Req GET "/api/orders/$id/return" $null $customer);if($r.state-eq$state){return $r};Start-Sleep -Milliseconds 500};throw "Yêu cầu trả $id chưa tới $state"}
function Fulfill($id,$action,$state,$actor,$extra=@{}){$body=@{action=$action};foreach($name in $extra.Keys){$body[$name]=$extra[$name]};Check "$action tiếp nhận" (Req POST "/api/orders/$id/fulfillment" $body $actor ([Guid]::NewGuid().ToString())) 202;$null=AwaitOrder $id $state}
$suffix=[Guid]::NewGuid().ToString('N').Substring(0,10)
$admin=(Json (Req POST '/api/auth/login' @{email='admin@shop.local';password=$env:SEED_PASSWORD})).accessToken
$staff=(Json (Req POST '/api/auth/login' @{email='staff@shop.local';password=$env:SEED_PASSWORD})).accessToken
$password=[Guid]::NewGuid().ToString('N');$email="partial-$suffix@example.com"
Check 'Tạo khách' (Req POST '/api/auth/register' @{fullName='Khách trả một phần';email=$email;phone='0901234567';password=$password}) 201
$customer=(Json (Req POST '/api/auth/login' @{email=$email;password=$password})).accessToken
$other=(Json (Req POST '/api/auth/login' @{email='customer@shop.local';password=$env:SEED_PASSWORD})).accessToken
$categories=Json (Req GET '/api/catalog/categories');$brands=Json (Req GET '/api/catalog/brands')
$product=Json (Req POST '/api/catalog/manage/products' @{name="Sản phẩm trả một phần $suffix";categoryId=$categories[0].id;brandId=$brands[0].id;description='Dữ liệu thử nghiệm';material='Cotton';style='Suông';gender='UNISEX';active=$true;featured=$false;variants=@(@{sku="PART-$suffix";size='M';color='Đen';costPrice=50000;price=100000;active=$true})} $admin)
$variant=$product.variants[0].id
Check 'Nhập kho ba đơn vị' (Req POST '/api/inventory/adjustments' @{variantId=$variant;type='RECEIPT';quantity=3;reason='Smoke partial return'} $admin "stock-$suffix") 200
$cart=Req PUT "/api/cart/items/$variant" @{quantity=2} $customer;Check 'Giỏ hai áo' $cart 200
$made=Req POST '/api/orders' @{cartRevision=(Json $cart).revision;recipient='Khách thử nghiệm';phone='0901234567';address='Địa chỉ thử nghiệm local';paymentMethod='COD'} $customer ([Guid]::NewGuid().ToString());Check 'Tạo đơn' $made 202
$id=(Json $made).id;$null=AwaitOrder $id 'PLACED'
Check 'Xác nhận đơn' (Req POST "/api/orders/$id/state" @{state='CONFIRMED'} $staff) 200
Check 'Đóng gói' (Req POST "/api/orders/$id/state" @{state='PACKING'} $staff) 200
Fulfill $id 'SHIP' 'SHIPPED' $staff @{carrier='Giao hàng local';tracking="PART-$id";assignee='Nhân viên thử nghiệm';carrierCost=25000}
Fulfill $id 'DELIVER' 'DELIVERED' $staff
Fulfill $id 'COLLECT_COD' 'COMPLETED' $staff @{reference="COLLECT-$id"}
Assert 'Đã xuất hai, còn một' ((Json (Req GET "/api/inventory/availability/$variant")).available-eq 1)
$before=[decimal](Json (Req GET '/api/reports/dashboard' $null $admin)).realizedRevenue
$body=@{reason='Một chiếc áo bị lỗi đường may';items=@(@{variantId=$variant;quantity=1})};$key="partial-$id"
Check 'Khách khác không xem yêu cầu' (Req GET "/api/orders/$id/return" $null $other) 404
Check 'STAFF không tạo yêu cầu' (Req POST "/api/orders/$id/return" $body $staff $key) 403
$created=Req POST "/api/orders/$id/return" $body $customer $key;Check 'Tạo yêu cầu trả một phần' $created 201
Assert 'Tiền hoàn theo giá gốc, không gồm vận chuyển' ((Json $created).refundAmount-eq 100000-and(Json $created).mode-eq'PARTIAL')
Check 'Gửi lại cùng key' (Req POST "/api/orders/$id/return" $body $customer $key) 201
Check 'Đổi số lượng dưới cùng key bị chặn' (Req POST "/api/orders/$id/return" @{reason=$body.reason;items=@(@{variantId=$variant;quantity=2})} $customer $key) 409
Check 'STAFF duyệt' (Req POST "/api/orders/$id/return/decision" @{action='APPROVE';note='Một áo hợp lệ'} $staff) 200
Check 'STAFF nhận hàng' (Req POST "/api/orders/$id/return/receive" @{note='Đã nhận một áo'} $staff) 200
$null=AwaitReturn $id 'REFUND_PENDING'
Assert 'Kho chỉ cộng một áo' ((Json (Req GET "/api/inventory/availability/$variant")).available-eq 2)
Assert 'COD còn tính đủ trước chứng từ' ([decimal](Json (Req GET '/api/reports/dashboard' $null $admin)).realizedRevenue-eq$before)
Check 'STAFF không ghi hoàn COD' (Req POST "/api/orders/$id/return/confirm-refund" @{reference="REFUND-$suffix"} $staff) 403
Check 'ADMIN ghi chứng từ hoàn COD' (Req POST "/api/orders/$id/return/confirm-refund" @{reference="REFUND-$suffix"} $admin) 200
$null=AwaitReturn $id 'REFUNDED'
$after=[decimal](Json (Req GET '/api/reports/dashboard' $null $admin)).realizedRevenue
Assert 'Doanh thu thuần giảm đúng tiền hoàn' (($before-$after)-eq 100000)
$final=Json (Req GET "/api/orders/$id" $null $customer)
Assert 'Đơn còn hoàn tất với một phần giữ lại' ($final.state-eq'COMPLETED'-and$final.paymentState-eq'PAID'-and[decimal]$final.returnedAmount-eq 100000)
Assert 'Kho không cộng lặp' ((Json (Req GET "/api/inventory/availability/$variant")).available-eq 2)
Assert 'Payment có một refund' ((Json (Req GET "/api/payments/$id" $null $customer)).refunds.Count-eq 1)
$notices=Json (Req GET '/api/notifications' $null $customer)
Assert 'Khách nhận thông báo hoàn một phần' (@($notices.content|Where-Object { $_.orderId-eq$id-and$_.state-eq'RETURN_REFUNDED' }).Count-eq 1)
Assert 'Khách nhận thông báo duyệt trả hàng' (@($notices.content|Where-Object { $_.orderId-eq$id-and$_.state-eq'RETURN_APPROVED' }).Count-eq 1)
Check 'Ngừng bán sản phẩm thử nghiệm' (Req DELETE "/api/catalog/manage/products/$($product.id)" $null $admin) 204
@{timestamp=(Get-Date).ToUniversalTime().ToString('o');database='MySQL localhost:3310';tests=@($checks.ToArray());note='Chứng từ REFUND-* chỉ là dữ liệu demo; không chứng minh chuyển khoản thật.'}|ConvertTo-Json -Depth 8|Set-Content (Join-Path $projectRoot 'docs/partial-returns-smoke-result.json') -Encoding UTF8
Write-Host "$($checks.Count) kiểm tra trả hàng một phần qua MySQL thành công."
