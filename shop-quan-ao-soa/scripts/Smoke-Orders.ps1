param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$checks=[System.Collections.Generic.List[object]]::new()
function Req($method,$path,$body=$null,$token='',$key='') {
 $h=@{}; if($token){$h.Authorization="Bearer $token"}; if($key){$h['Idempotency-Key']=$key}
 $p=@{Uri="$BaseUrl$path";Method=$method;Headers=$h;ContentType='application/json; charset=utf-8';SkipHttpErrorCheck=$true;TimeoutSec=20}
 if($null -ne $body){$p.Body=[Text.Encoding]::UTF8.GetBytes(($body|ConvertTo-Json -Depth 10))}
 Invoke-WebRequest @p
}
function Check($name,$response,$status){if([int]$response.StatusCode-ne$status){throw "$name : HTTP $($response.StatusCode), mong đợi $status"};$checks.Add(@{name=$name;status='PASS'})}
function ReadJson($r){$r.Content|ConvertFrom-Json}
function AwaitState($id,$token,$expected){for($i=0;$i-lt 25;$i++){$o=ReadJson (Req GET "/api/orders/$id" $null $token);if($o.state-in$expected){return $o};Start-Sleep -Milliseconds 600};throw "Đơn $id vẫn ở $($o.state)"}
$suffix=[Guid]::NewGuid().ToString('N').Substring(0,10)
$admin=ReadJson (Req POST '/api/auth/login' @{email='admin@shop.local';password=$env:SEED_PASSWORD})
$tokens=@()
foreach($n in 1,2){$email="order-$suffix-$n@example.com";$password=[Guid]::NewGuid().ToString('N');Check "Đăng ký khách $n" (Req POST '/api/auth/register' @{fullName="Khách đơn $n";email=$email;phone='0901234567';password=$password}) 201;$tokens+=(ReadJson (Req POST '/api/auth/login' @{email=$email;password=$password})).accessToken}
$categories=ReadJson (Req GET '/api/catalog/categories');$brands=ReadJson (Req GET '/api/catalog/brands')
$product=ReadJson (Req POST '/api/catalog/manage/products' @{name="Kiểm thử đơn $suffix";categoryId=$categories[0].id;brandId=$brands[0].id;description='Kiểm thử tích hợp';material='Cotton';style='Suông';gender='UNISEX';active=$true;featured=$false;variants=@(@{sku="ORDER-$suffix";size='M';color='Đen';costPrice=50000;price=100000;salePrice=$null;active=$true})} $admin.accessToken)
if(-not$product.id){throw 'Không tạo được sản phẩm kiểm thử'}
$variant=$product.variants[0].id
Check 'Nhập đúng một đơn vị' (Req POST '/api/inventory/adjustments' @{variantId=$variant;type='RECEIPT';quantity=1;reason='Smoke Order'} $admin.accessToken "stock-$suffix") 200
$empty=@{cartRevision=0;recipient='Người nhận';phone='0901234567';address='12 Hà Nội';note='Smoke'}
Check 'Chặn giỏ trống' (Req POST '/api/orders' $empty $tokens[0] "empty-$suffix") 409
$orderIds=@()
foreach($n in 0,1){$c=Req PUT "/api/cart/items/$variant" @{quantity=1} $tokens[$n];Check "Lưu giỏ khách $n" $c 200;$body=@{cartRevision=(ReadJson $c).revision;recipient='Người nhận';phone='0901234567';address='12 Hà Nội';note='Smoke'};$r=Req POST '/api/orders' $body $tokens[$n] "order-$suffix-$n";Check "Nhận yêu cầu đặt $n" $r 202;$id=(ReadJson $r).id;$orderIds+=$id;$repeat=Req POST '/api/orders' $body $tokens[$n] "order-$suffix-$n";Check "Gọi lặp đặt $n" $repeat 202;if((ReadJson $repeat).id-ne$id){throw 'Tạo đơn trùng'};$body.note='Khác';Check "Chặn key khác payload $n" (Req POST '/api/orders' $body $tokens[$n] "order-$suffix-$n") 409}
$a=AwaitState $orderIds[0] $tokens[0] @('PLACED','FAILED');$b=AwaitState $orderIds[1] $tokens[1] @('PLACED','FAILED')
if(@($a.state,$b.state|Where-Object{$_-eq'PLACED'}).Count-ne 1){throw 'Không đảm bảo chỉ một đơn mua được hàng'}
$checks.Add(@{name='Hai khách tranh một đơn vị: chỉ một PLACED';status='PASS'})
$winner=if($a.state-eq'PLACED'){0}else{1};$id=$orderIds[$winner];$token=$tokens[$winner];$loser=1-$winner
$o=ReadJson (Req GET "/api/orders/$id" $null $token);if($o.total-ne 130000-or$o.paymentState-ne'UNPAID'){throw 'Sai tiền hoặc trạng thái COD'};$checks.Add(@{name='Giá snapshot + phí 30000, COD chưa thanh toán';status='PASS'})
Check 'Người khác không đọc được đơn' (Req GET "/api/orders/$id" $null $tokens[$loser]) 404
Check 'Khách không vào danh sách quản lý' (Req GET '/api/orders/manage' $null $token) 403
Check 'Không nhảy thẳng đóng gói' (Req POST "/api/orders/$id/state" @{state='PACKING'} $admin.accessToken) 409
Check 'Hủy đơn' (Req POST "/api/orders/$id/state" @{state='CANCELLED'} $token) 200
$null=AwaitState $id $token @('CANCELLED')
Check 'Hủy lặp an toàn' (Req POST "/api/orders/$id/state" @{state='CANCELLED'} $token) 200
$available=ReadJson (Req GET "/api/inventory/availability/$variant");if($available.available-ne 1){throw 'Hoàn kho sai số lượng'};$checks.Add(@{name='Hủy hoàn đúng một đơn vị';status='PASS'})
Check 'Đơn hủy không xác nhận lại' (Req POST "/api/orders/$id/state" @{state='CONFIRMED'} $admin.accessToken) 409
$staff=ReadJson (Req POST '/api/auth/login' @{email='staff@shop.local';password=$env:SEED_PASSWORD})
if(-not$staff.accessToken){throw 'Không đăng nhập được STAFF mẫu'}
Check 'STAFF xem danh sách quản lý' (Req GET '/api/orders/manage' $null $staff.accessToken) 200
$c=ReadJson (Req PUT "/api/cart/items/$variant" @{quantity=1} $token)
$body=@{cartRevision=$c.revision;recipient='Người nhận';phone='0901234567';address='12 Hà Nội';note='Luồng nhân viên'}
$r=Req POST '/api/orders' $body $token "staff-flow-$suffix"
Check 'Tạo đơn kiểm thử xử lý STAFF' $r 202
$staffOrder=(ReadJson $r).id
$null=AwaitState $staffOrder $token @('PLACED')
Check 'STAFF xác nhận đơn' (Req POST "/api/orders/$staffOrder/state" @{state='CONFIRMED'} $staff.accessToken) 200
Check 'Khách không hủy đơn đã xác nhận' (Req POST "/api/orders/$staffOrder/state" @{state='CANCELLED'} $token) 409
Check 'STAFF đóng gói' (Req POST "/api/orders/$staffOrder/state" @{state='PACKING'} $staff.accessToken) 200
Check 'STAFF hủy trước giao hàng' (Req POST "/api/orders/$staffOrder/state" @{state='CANCELLED'} $staff.accessToken) 200
$null=AwaitState $staffOrder $token @('CANCELLED')
$available=ReadJson (Req GET "/api/inventory/availability/$variant")
if($available.available-ne 1){throw 'Hủy đơn STAFF chưa hoàn kho đúng'}
$checks.Add(@{name='Hủy sau đóng gói hoàn kho đúng';status='PASS'})
Check 'Ngừng sản phẩm kiểm thử' (Req DELETE "/api/catalog/manage/products/$($product.id)" $null $admin.accessToken) 204
@{runAt=(Get-Date).ToString('o');checks=$checks}|ConvertTo-Json -Depth 8|Set-Content (Join-Path $projectRoot 'docs/orders-smoke-result.json') -Encoding utf8
Write-Host "PASS: $($checks.Count) kiểm tra Order qua Gateway/MySQL."

