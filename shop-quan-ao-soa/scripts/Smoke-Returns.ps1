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
function AwaitOrder($id,$state){
 for($i=0;$i-lt 80;$i++){$o=Json (Req GET "/api/orders/$id" $null $customer);if($o.state-eq$state-and-not$o.pendingCommandId){return $o};Start-Sleep -Milliseconds 500}
 throw "Đơn $id chưa chuyển tới $state"
}
function AwaitReturn($id,$state){
 for($i=0;$i-lt 80;$i++){$r=Json (Req GET "/api/orders/$id/return" $null $customer);if($r.state-eq$state){return $r};Start-Sleep -Milliseconds 500}
 throw "Yêu cầu trả $id chưa chuyển tới $state"
}
function Fulfill($id,$action,$state,$actor,$extra=@{}){
 $body=@{action=$action};foreach($name in $extra.Keys){$body[$name]=$extra[$name]}
 Check "$action tiếp nhận" (Req POST "/api/orders/$id/fulfillment" $body $actor ([Guid]::NewGuid().ToString())) 202
 $null=AwaitOrder $id $state
}
function NewCompleted($method){
 $cart=Req PUT "/api/cart/items/$variant" @{quantity=1} $customer;Check "Giỏ $method" $cart 200
 $body=@{cartRevision=(Json $cart).revision;recipient='Khách thử nghiệm';phone='0901234567';address='Địa chỉ thử nghiệm local';paymentMethod=$method}
 $made=Req POST '/api/orders' $body $customer ([Guid]::NewGuid().ToString());Check "Tạo đơn $method" $made 202
 $id=(Json $made).id
 $state=if($method-eq'COD'){'PLACED'}else{'AWAITING_PAYMENT'}
 $null=AwaitOrder $id $state
 if($method-eq'SIMULATED'){Fulfill $id 'SIM_SUCCESS' 'PLACED' $customer}
 Check 'Xác nhận đơn' (Req POST "/api/orders/$id/state" @{state='CONFIRMED'} $staff) 200
 Check 'Đóng gói' (Req POST "/api/orders/$id/state" @{state='PACKING'} $staff) 200
 Fulfill $id 'SHIP' 'SHIPPED' $staff @{carrier='Giao hàng local';tracking="RET-$id";assignee='Nhân viên thử nghiệm';carrierCost=25000}
 $delivered=if($method-eq'COD'){'DELIVERED'}else{'COMPLETED'}
 Fulfill $id 'DELIVER' $delivered $staff
 if($method-eq'COD'){Fulfill $id 'COLLECT_COD' 'COMPLETED' $staff @{reference="COLLECT-$id"}}
 return $id
}
$suffix=[Guid]::NewGuid().ToString('N').Substring(0,10)
$admin=(Json (Req POST '/api/auth/login' @{email='admin@shop.local';password=$env:SEED_PASSWORD})).accessToken
$staff=(Json (Req POST '/api/auth/login' @{email='staff@shop.local';password=$env:SEED_PASSWORD})).accessToken
$password=[Guid]::NewGuid().ToString('N');$email="returns-$suffix@example.com"
Check 'Tạo khách' (Req POST '/api/auth/register' @{fullName='Khách trả hàng';email=$email;phone='0901234567';password=$password}) 201
$customer=(Json (Req POST '/api/auth/login' @{email=$email;password=$password})).accessToken
$other=(Json (Req POST '/api/auth/login' @{email='customer@shop.local';password=$env:SEED_PASSWORD})).accessToken
$categories=Json (Req GET '/api/catalog/categories');$brands=Json (Req GET '/api/catalog/brands')
$product=Json (Req POST '/api/catalog/manage/products' @{name="Sản phẩm trả hàng $suffix";categoryId=$categories[0].id;brandId=$brands[0].id;description='Dữ liệu thử nghiệm';material='Cotton';style='Suông';gender='UNISEX';active=$true;featured=$false;variants=@(@{sku="RET-$suffix";size='M';color='Đen';costPrice=50000;price=100000;active=$true})} $admin)
$variant=$product.variants[0].id
Check 'Nhập kho hai đơn vị' (Req POST '/api/inventory/adjustments' @{variantId=$variant;type='RECEIPT';quantity=2;reason='Smoke Returns'} $admin "stock-$suffix") 200
$cod=NewCompleted 'COD'
$sim=NewCompleted 'SIMULATED'
Assert 'Hai đơn đã xuất hết hai đơn vị' ((Json (Req GET "/api/inventory/availability/$variant")).available-eq 0)
foreach($id in @($cod,$sim)){
 $key="return-$id";$body=@{reason='Sản phẩm có lỗi đường may cần trả'}
 Check 'Người khác không xem yêu cầu' (Req GET "/api/orders/$id/return" $null $other) 404
 Check 'STAFF không tạo yêu cầu khách' (Req POST "/api/orders/$id/return" $body $staff $key) 403
 Check 'Khách tạo yêu cầu' (Req POST "/api/orders/$id/return" $body $customer $key) 201
 Check 'Khách không xem hàng đợi trả hàng' (Req GET '/api/orders/returns/manage?state=REQUESTED' $null $customer) 403
 $queue=Req GET '/api/orders/returns/manage?state=REQUESTED' $null $staff;Check 'STAFF đọc hàng đợi' $queue 200
 Assert 'Yêu cầu mới có trong hàng đợi' (@((Json $queue).content|Where-Object orderId -eq $id).Count-eq 1)
 Check 'Gửi lặp cùng key' (Req POST "/api/orders/$id/return" $body $customer $key) 201
 Check 'Key khác bị chặn' (Req POST "/api/orders/$id/return" $body $customer "other-$id") 409
 Check 'Khách không tự duyệt' (Req POST "/api/orders/$id/return/decision" @{action='APPROVE';note=''} $customer) 403
 Check 'Không thể nhận trước khi duyệt' (Req POST "/api/orders/$id/return/receive" @{note='Đã nhận'} $staff) 409
 Check 'STAFF duyệt' (Req POST "/api/orders/$id/return/decision" @{action='APPROVE';note='Đủ điều kiện'} $staff) 200
 Check 'STAFF nhận đủ hàng' (Req POST "/api/orders/$id/return/receive" @{note='Đã kiểm đủ hàng'} $staff) 200
 $expected=if($id-eq$cod){'REFUND_PENDING'}else{'REFUNDED'}
 $null=AwaitReturn $id $expected
}
Assert 'Hai đơn hoàn kho đúng hai đơn vị' ((Json (Req GET "/api/inventory/availability/$variant")).available-eq 2)
$codBeforeRefund=Json (Req GET "/api/orders/$cod" $null $customer)
Assert 'COD còn doanh thu trước khi hoàn tiền thực tế' ($codBeforeRefund.state-eq'COMPLETED'-and$codBeforeRefund.paymentState-eq'PAID')
$revenueBefore=[decimal](Json (Req GET '/api/reports/dashboard' $null $admin)).realizedRevenue
Check 'STAFF không xác nhận hoàn COD' (Req POST "/api/orders/$cod/return/confirm-refund" @{reference="REFUND-$suffix"} $staff) 403
Check 'ADMIN ghi chứng từ hoàn COD' (Req POST "/api/orders/$cod/return/confirm-refund" @{reference="REFUND-$suffix"} $admin) 200
$null=AwaitReturn $cod 'REFUNDED'
Check 'Ghi lặp cùng chứng từ' (Req POST "/api/orders/$cod/return/confirm-refund" @{reference="REFUND-$suffix"} $admin) 200
Check 'Chứng từ khác bị chặn' (Req POST "/api/orders/$cod/return/confirm-refund" @{reference='DIFFERENT'} $admin) 409
Assert 'COD đã hoàn và loại khỏi doanh thu' ((Json (Req GET "/api/orders/$cod" $null $customer)).state-eq'RETURNED')
$revenueAfter=[decimal](Json (Req GET '/api/reports/dashboard' $null $admin)).realizedRevenue
Assert 'Doanh thu giảm đúng tổng đơn COD đã hoàn' (($revenueBefore-$revenueAfter)-eq[decimal]$codBeforeRefund.total)
Assert 'Mô phỏng đã hoàn nhưng không chuyển tiền' ((Json (Req GET "/api/payments/$sim" $null $customer)).paymentState-eq'SIMULATED_REFUNDED')
Assert 'COD chỉ có một refund' ((Json (Req GET "/api/payments/$cod" $null $customer)).refunds.Count-eq 1)
Assert 'Hoàn lặp không cộng kho thêm' ((Json (Req GET "/api/inventory/availability/$variant")).available-eq 2)
$notices=Json (Req GET '/api/notifications' $null $customer)
Assert 'Khách nhận thông báo đơn trả hàng' (@($notices.content|Where-Object { $_.orderId-eq$cod-and$_.state-eq'RETURNED' }).Count-eq 1)
Check 'Ngừng bán sản phẩm kiểm thử' (Req DELETE "/api/catalog/manage/products/$($product.id)" $null $admin) 204
@{timestamp=(Get-Date).ToUniversalTime().ToString('o');database='MySQL localhost:3310';tests=@($checks.ToArray());note='Sản phẩm thử nghiệm ngừng bán; lịch sử đơn, hoàn tiền và kho được giữ để đối chiếu. Chứng từ REFUND-* là dữ liệu demo, không chứng minh đã chuyển khoản thật.'}|ConvertTo-Json -Depth 8|Set-Content (Join-Path $projectRoot 'docs/returns-smoke-result.json') -Encoding UTF8
Write-Host "$($checks.Count) kiểm tra trả hàng/hoàn tiền qua MySQL thành công."
