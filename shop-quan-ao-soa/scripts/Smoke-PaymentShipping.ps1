param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$checks=[System.Collections.Generic.List[object]]::new()
function Req($method,$path,$body=$null,$token='',$key='') {
 $h=@{};if($token){$h.Authorization="Bearer $token"};if($key){$h['Idempotency-Key']=$key}
 $p=@{Uri="$BaseUrl$path";Method=$method;Headers=$h;ContentType='application/json; charset=utf-8';SkipHttpErrorCheck=$true;TimeoutSec=20}
 if($null-ne$body){$p.Body=[Text.Encoding]::UTF8.GetBytes(($body|ConvertTo-Json -Depth 10))};Invoke-WebRequest @p
}
function ReadJson($r){$r.Content|ConvertFrom-Json}
function Check($name,$r,$status){if([int]$r.StatusCode-ne$status){throw "$name : HTTP $($r.StatusCode), mong đợi $status"};$checks.Add(@{name=$name;status='PASS'})}
function Assert($name,$condition){if(-not$condition){throw $name};$checks.Add(@{name=$name;status='PASS'})}
function AwaitOrder($id,$expected){for($i=0;$i-lt 60;$i++){$r=Req GET "/api/orders/$id" $null $customer;$o=ReadJson $r;if($o.state-eq$expected-and-not$o.pendingCommandId){return $o};Start-Sleep -Milliseconds 500};throw "Đơn $id còn $($o.state), chờ $expected"}
function Execute($id,$action,$expected,$actor,$extra=@{}) {
 $key=[Guid]::NewGuid().ToString();$body=@{action=$action};foreach($k in $extra.Keys){$body[$k]=$extra[$k]}
 $r=Req POST "/api/orders/$id/fulfillment" $body $actor $key;Check "$action được tiếp nhận" $r 202;$cmd=ReadJson $r
 $again=Req POST "/api/orders/$id/fulfillment" $body $actor $key;Check "$action retry cùng key" $again 202;Assert "$action không tạo command trùng" ((ReadJson $again).id-eq$cmd.id)
 $null=AwaitOrder $id $expected
 $commands=ReadJson (Req GET "/api/orders/$id/fulfillment" $null $customer);Assert "$action command hoàn tất" (($commands|Where-Object id -eq $cmd.id).state-eq'DONE')
 return @{key=$key;body=$body;id=$cmd.id}
}
function NewOrder($method) {
 $c=Req PUT "/api/cart/items/$variant" @{quantity=1} $customer;Check "Giỏ $method" $c 200
 $r=Req POST '/api/orders' @{cartRevision=(ReadJson $c).revision;recipient='Khách kiểm thử';phone='0901234567';address='Địa chỉ thử nghiệm local';note='SMOKE PaymentShipping - dữ liệu kiểm thử';paymentMethod=$method} $customer ([Guid]::NewGuid().ToString());Check "Tạo $method" $r 202
 $id=(ReadJson $r).id;$expected=if($method-eq'COD'){'PLACED'}else{'AWAITING_PAYMENT'};$null=AwaitOrder $id $expected;return $id
}
function Pack($id){Check 'STAFF xác nhận' (Req POST "/api/orders/$id/state" @{state='CONFIRMED'} $staff) 200;Check 'STAFF đóng gói' (Req POST "/api/orders/$id/state" @{state='PACKING'} $staff) 200}
function Ship($id){$null=Execute $id 'SHIP' 'SHIPPED' $staff @{carrier='Giao hàng local';tracking="TEST-$id";assignee='Nhân viên kiểm thử';carrierCost=25000}}
$suffix=[Guid]::NewGuid().ToString('N').Substring(0,10)
$admin=(ReadJson (Req POST '/api/auth/login' @{email='admin@shop.local';password=$env:SEED_PASSWORD})).accessToken
$staff=(ReadJson (Req POST '/api/auth/login' @{email='staff@shop.local';password=$env:SEED_PASSWORD})).accessToken
if(-not$admin-or-not$staff){throw 'Không đăng nhập được ADMIN/STAFF mẫu'}
$password=[Guid]::NewGuid().ToString('N');$email="fulfillment-$suffix@example.com"
Check 'Tạo khách kiểm thử' (Req POST '/api/auth/register' @{fullName='Khách thanh toán';email=$email;phone='0901234567';password=$password}) 201
$customer=(ReadJson (Req POST '/api/auth/login' @{email=$email;password=$password})).accessToken
$other=(ReadJson (Req POST '/api/auth/login' @{email='customer@shop.local';password=$env:SEED_PASSWORD})).accessToken
$categories=ReadJson (Req GET '/api/catalog/categories');$brands=ReadJson (Req GET '/api/catalog/brands')
$r=Req POST '/api/catalog/manage/products' @{name="Kiểm thử giao hàng $suffix";categoryId=$categories[0].id;brandId=$brands[0].id;description='Dữ liệu thử nghiệm';material='Cotton';style='Suông';gender='UNISEX';active=$true;featured=$false;variants=@(@{sku="PAY-$suffix";size='M';color='Đen';costPrice=50000;price=100000;salePrice=$null;active=$true})} $admin
Check 'Sản phẩm riêng cho smoke' $r 201;$product=ReadJson $r;$variant=$product.variants[0].id
Check 'Nhập 10 đơn vị thử nghiệm' (Req POST '/api/inventory/adjustments' @{variantId=$variant;type='RECEIPT';quantity=10;reason='Smoke PaymentShipping'} $admin "stock-$suffix") 200

$cod=NewOrder 'COD';Pack $cod
Check 'Khách không được giao hàng' (Req POST "/api/orders/$cod/fulfillment" @{action='SHIP'} $customer "denied-$suffix") 403
Check 'Không thu COD trước khi giao' (Req POST "/api/orders/$cod/fulfillment" @{action='COLLECT_COD';reference='TEST'} $staff "early-$suffix") 409
Ship $cod
Check 'Không hủy khi hàng đang giao' (Req POST "/api/orders/$cod/state" @{state='CANCELLED'} $admin) 409
$null=Execute $cod 'DELIVER' 'DELIVERED' $staff
$pay=ReadJson (Req GET "/api/payments/$cod" $null $customer);Assert 'Giao thành công không tự ghi đã thu COD' ($pay.paymentState-eq'UNPAID')
$command=Execute $cod 'COLLECT_COD' 'COMPLETED' $staff @{reference="TEST-COD-$suffix"}
Check 'Gọi lại thu COD sau hoàn tất' (Req POST "/api/orders/$cod/fulfillment" $command.body $staff $command.key) 202
Check 'Cùng key khác payload bị chặn' (Req POST "/api/orders/$cod/fulfillment" @{action='COLLECT_COD';reference='KHAC'} $staff $command.key) 409
$pay=ReadJson (Req GET "/api/payments/$cod" $null $customer);Assert 'COD ghi nhận một lần' ($pay.paymentState-eq'PAID'-and@($pay.history|Where-Object action -eq 'COLLECT_COD').Count-eq 1)
Assert 'Khách không nhận giá vốn vận chuyển' ($null-eq$pay.carrierCost)
Check 'Người khác không đọc thanh toán' (Req GET "/api/payments/$cod" $null $other) 404
Check 'Người khác không đọc giao hàng' (Req GET "/api/shipments/$cod" $null $other) 404
Write-Host 'PASS luồng COD hoàn tất.'

$cancel=NewOrder 'SIMULATED'
Check 'STAFF không mô phỏng thay chủ đơn' (Req POST "/api/orders/$cancel/fulfillment" @{action='SIM_SUCCESS'} $staff "staff-sim-$suffix") 403
$null=Execute $cancel 'SIM_SUCCESS' 'PLACED' $customer
Check 'Hủy đơn đã thanh toán mô phỏng' (Req POST "/api/orders/$cancel/state" @{state='CANCELLED'} $customer) 200
$null=AwaitOrder $cancel 'CANCELLED'
Check 'Hủy lặp đơn mô phỏng' (Req POST "/api/orders/$cancel/state" @{state='CANCELLED'} $customer) 200
$pay=ReadJson (Req GET "/api/payments/$cancel" $null $customer);Assert 'Hoàn mô phỏng đúng một lần, không phải PAID thật' ($pay.paymentState-eq'SIMULATED_REFUNDED'-and$pay.refunds.Count-eq 1)
$failed=NewOrder 'SIMULATED';$null=Execute $failed 'SIM_FAILURE' 'FAILED' $customer
$pay=ReadJson (Req GET "/api/payments/$failed" $null $customer);Assert 'Mô phỏng thất bại không ghi thành công' ($pay.paymentState-eq'SIMULATED_FAILED')
Write-Host 'PASS mô phỏng thất bại và hủy/hoàn mô phỏng.'

$returned=NewOrder 'SIMULATED';$null=Execute $returned 'SIM_SUCCESS' 'PLACED' $customer;Pack $returned;Ship $returned
$null=Execute $returned 'DELIVERY_FAIL' 'DELIVERY_FAILED' $staff @{note='Không liên hệ được - kiểm thử'}
$null=Execute $returned 'RETRY_SHIP' 'SHIPPED' $staff
$null=Execute $returned 'DELIVERY_FAIL' 'DELIVERY_FAILED' $staff @{note='Khách từ chối - kiểm thử'}
$null=Execute $returned 'RETURN_RECEIVED' 'CANCELLED' $staff @{note='Đã nhận đủ hàng vào kho - kiểm thử'}
$pay=ReadJson (Req GET "/api/shipments/$returned" $null $customer);Assert 'Hàng hoàn đã nhập và hoàn mô phỏng' ($pay.shippingState-eq'RETURNED'-and$pay.paymentState-eq'SIMULATED_REFUNDED'-and$pay.refunds.Count-eq 1)
Write-Host 'PASS giao thất bại, giao lại, nhận hàng hoàn.'

$completed=NewOrder 'SIMULATED';$null=Execute $completed 'SIM_SUCCESS' 'PLACED' $customer;Pack $completed;Ship $completed
$null=Execute $completed 'DELIVER' 'COMPLETED' $staff
$pay=ReadJson (Req GET "/api/payments/$completed" $null $customer);Assert 'Hoàn tất mô phỏng vẫn giữ nhãn mô phỏng' ($pay.paymentState-eq'SIMULATED_PAID')
$stock=ReadJson (Req GET "/api/inventory/availability/$variant");Assert 'Tồn cuối 8: chỉ 2 đơn hoàn tất tiêu thụ hàng' ($stock.available-eq 8)
Check 'Ngừng bán sản phẩm smoke' (Req DELETE "/api/catalog/manage/products/$($product.id)" $null $admin) 204
@{runAt=(Get-Date).ToString('o');checks=$checks}|ConvertTo-Json -Depth 8|Set-Content (Join-Path $projectRoot 'docs/payment-shipping-smoke-result.json') -Encoding utf8
Write-Host "PASS: $($checks.Count) kiểm tra Payment/Shipping qua Gateway và MySQL."
