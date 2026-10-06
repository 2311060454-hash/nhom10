param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$checks=[System.Collections.Generic.List[object]]::new()
function Req($method,$path,$body=$null,$token='',$key=''){
 $headers=@{};if($token){$headers.Authorization="Bearer $token"};if($key){$headers['Idempotency-Key']=$key}
 $args=@{Uri="$BaseUrl$path";Method=$method;Headers=$headers;ContentType='application/json; charset=utf-8';SkipHttpErrorCheck=$true;TimeoutSec=25}
 if($null-ne$body){$args.Body=[Text.Encoding]::UTF8.GetBytes(($body|ConvertTo-Json -Depth 10))};Invoke-WebRequest @args
}
function ReadJson($r){$r.Content|ConvertFrom-Json}
function Check($name,$r,$status){if([int]$r.StatusCode-ne$status){throw "$name HTTP $($r.StatusCode): $($r.Content)"};$checks.Add(@{name=$name;status='PASS'})}
function Assert($name,$valid){if(-not$valid){throw $name};$checks.Add(@{name=$name;status='PASS'})}
function AwaitOrder($id,$state,$token){for($i=0;$i-lt 50;$i++){$o=ReadJson (Req GET "/api/orders/$id" $null $token);if($o.state-eq$state-and-not$o.pendingCommandId){return $o};Start-Sleep -Milliseconds 500};throw "Đơn $id còn $($o.state), cần $state"}
function Fulfill($id,$action,$target,$actor,$customer,$extra=@{}){$body=@{action=$action};foreach($k in $extra.Keys){$body[$k]=$extra[$k]};Check "$action nhận lệnh" (Req POST "/api/orders/$id/fulfillment" $body $actor ([Guid]::NewGuid().ToString())) 202;$null=AwaitOrder $id $target $customer}
$suffix=[Guid]::NewGuid().ToString('N').Substring(0,10).ToUpperInvariant()
$admin=(ReadJson (Req POST '/api/auth/login' @{email='admin@shop.local';password=$env:SEED_PASSWORD})).accessToken
$staff=(ReadJson (Req POST '/api/auth/login' @{email='staff@shop.local';password=$env:SEED_PASSWORD})).accessToken
Assert 'Đăng nhập ADMIN/STAFF' ($admin-and$staff)
$before=ReadJson (Req GET '/api/reports/dashboard' $null $admin)
Assert 'Dashboard lấy số liệu thật từ các service' ($before.productCount-gt 0-and$before.customerCount-gt 0-and$before.totalOrders-gt 0)
Check 'STAFF không đọc doanh thu' (Req GET '/api/reports/dashboard' $null $staff) 403
Check 'Ẩn dashboard với khách chưa đăng nhập' (Req GET '/api/reports/dashboard') 401
$password=[Guid]::NewGuid().ToString('N');$email="report-$suffix@example.com"
Check 'Tạo khách kiểm thử' (Req POST '/api/auth/register' @{fullName='Khách báo cáo';email=$email;phone='0901234567';password=$password}) 201
$customer=(ReadJson (Req POST '/api/auth/login' @{email=$email;password=$password})).accessToken
$other=(ReadJson (Req POST '/api/auth/login' @{email='customer@shop.local';password=$env:SEED_PASSWORD})).accessToken
$categories=ReadJson (Req GET '/api/catalog/categories');$brands=ReadJson (Req GET '/api/catalog/brands')
$made=Req POST '/api/catalog/manage/products' @{name="Áo báo cáo $suffix";categoryId=$categories[0].id;brandId=$brands[0].id;description='Kiểm thử báo cáo';material='Cotton';style='Suông';gender='UNISEX';active=$true;featured=$false;variants=@(@{sku="REPORT-$suffix";size='L';color='Trắng';costPrice=50000;price=100000;salePrice=$null;active=$true})} $admin
Check 'Tạo sản phẩm báo cáo' $made 201;$product=ReadJson $made;$variant=$product.variants[0].id
Check 'Nhập kho sản phẩm báo cáo' (Req POST '/api/inventory/adjustments' @{variantId=$variant;type='RECEIPT';quantity=2;reason='Smoke Reporting'} $admin "stock-$suffix") 200
$cart=ReadJson (Req PUT "/api/cart/items/$variant" @{quantity=1} $customer)
$placed=Req POST '/api/orders' @{cartRevision=$cart.revision;recipient='Khách báo cáo';phone='0901234567';address='Địa chỉ kiểm thử';note='Smoke Reporting';paymentMethod='COD'} $customer ([Guid]::NewGuid().ToString())
Check 'Đặt đơn COD' $placed 202;$id=(ReadJson $placed).id;$null=AwaitOrder $id 'PLACED' $customer
$firstNotifications=ReadJson (Req GET '/api/notifications' $null $customer)
Assert 'Có thông báo đơn mới' (@($firstNotifications.content|Where-Object orderId -eq $id).Count-ge 1)
$notice=$firstNotifications.content|Where-Object orderId -eq $id|Select-Object -First 1
Check 'Khách khác không đọc thông báo' (Req PUT "/api/notifications/$($notice.id)/read" $null $other) 404
Check 'Chủ thông báo đánh dấu đã đọc' (Req PUT "/api/notifications/$($notice.id)/read" $null $customer) 200
Check 'STAFF xác nhận đơn' (Req POST "/api/orders/$id/state" @{state='CONFIRMED'} $staff) 200
Check 'STAFF đóng gói' (Req POST "/api/orders/$id/state" @{state='PACKING'} $staff) 200
Fulfill $id 'SHIP' 'SHIPPED' $staff $customer @{carrier='Giao hàng local';tracking="REPORT-$suffix";assignee='Nhân viên kiểm thử';carrierCost=25000}
Fulfill $id 'DELIVER' 'DELIVERED' $staff $customer
Fulfill $id 'COLLECT_COD' 'COMPLETED' $staff $customer @{reference="REPORT-COD-$suffix"}
$after=ReadJson (Req GET '/api/reports/dashboard' $null $admin)
Assert 'Doanh thu chỉ cộng COD đã thu' ([decimal]$after.realizedRevenue-[decimal]$before.realizedRevenue-eq 130000)
Assert 'Đếm đơn và sản phẩm từ database' ($after.totalOrders-eq($before.totalOrders+1)-and$after.productCount-eq($before.productCount+1))
$today=[DateTime]::Now.ToString('yyyy-MM-dd');$revenue=ReadJson (Req GET "/api/reports/revenue?from=$today&to=$today&group=DAY" $null $admin)
Assert 'Báo cáo theo ngày có đơn hoàn tất' ([decimal]$revenue.realizedRevenue-ge 130000-and$revenue.buckets.Count-ge 1)
$csv=Req GET "/api/reports/revenue.csv?from=$today&to=$today&group=DAY" $null $admin
Check 'Tải CSV doanh thu' $csv 200
Assert 'CSV có kỳ và số liệu thực' ($csv.Content.Contains($today)-and$csv.Content.Contains('doanh_thu_cod_vnd'))
$latest=ReadJson (Req GET '/api/notifications' $null $customer)
Assert 'Đơn hoàn tất xuất hiện trong thông báo' (@($latest.content|Where-Object {$_.orderId-eq$id-and$_.state-eq'COMPLETED'}).Count-eq 1)
$again=ReadJson (Req GET '/api/notifications' $null $customer)
Assert 'Đồng bộ lặp không tạo thông báo trùng' ($latest.totalElements-eq$again.totalElements)
$result=@{at=[DateTime]::UtcNow.ToString('o');checks=$checks;passed=$checks.Count;completedOrderId=$id;productId=$product.id}
$result|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $projectRoot 'docs/reporting-notification-smoke-result.json') -Encoding UTF8
Write-Host "PASS $($checks.Count) kiểm tra Reporting/Notification qua Gateway/MySQL."
