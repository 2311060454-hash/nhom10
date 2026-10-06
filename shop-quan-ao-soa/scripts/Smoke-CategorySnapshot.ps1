param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
$checks=[System.Collections.Generic.List[object]]::new()
function Request($method,$path,$body=$null,$token='',$key='') {
 $headers=@{};if($token){$headers.Authorization="Bearer $token"};if($key){$headers['Idempotency-Key']=$key}
 $params=@{Uri="$BaseUrl$path";Method=$method;Headers=$headers;ContentType='application/json; charset=utf-8';SkipHttpErrorCheck=$true;TimeoutSec=20}
 if($null-ne$body){$params.Body=[Text.Encoding]::UTF8.GetBytes(($body|ConvertTo-Json -Depth 10))}
 Invoke-WebRequest @params
}
function Check($name,$response,$status){if([int]$response.StatusCode-ne$status){throw "${name}: HTTP $($response.StatusCode), mong đợi $status. $($response.Content)"};$checks.Add(@{name=$name;status='PASS'})}
function Json($response){$response.Content|ConvertFrom-Json}
$suffix=[Guid]::NewGuid().ToString('N').Substring(0,10)
$email="snapshot-$suffix@example.com";$password=[Guid]::NewGuid().ToString('N')
Check 'Đăng ký khách kiểm thử' (Request POST '/api/auth/register' @{fullName='Khách kiểm thử snapshot';email=$email;phone='0901234567';password=$password}) 201
$login=Request POST '/api/auth/login' @{email=$email;password=$password};Check 'Đăng nhập khách kiểm thử' $login 200;$token=(Json $login).accessToken
$productsResponse=Request GET '/api/catalog/products';Check 'Đọc danh sách sản phẩm' $productsResponse 200
$products=Json $productsResponse
$product=$null;$variant=$null
foreach($candidate in $products.content){
 if(-not$candidate.active-or$candidate.categoryId-le 0){continue}
 foreach($item in $candidate.variants){
  if(-not$item.active){continue}
  $availability=Json (Request GET "/api/inventory/availability/$($item.id)")
  if($availability.available-gt 0){$product=$candidate;$variant=$item;break}
 }
 if($variant){break}
}
if(-not$variant){throw 'Không có biến thể đang bán và còn tồn để thử category snapshot.'}
$cart=Request PUT "/api/cart/items/$($variant.id)" @{quantity=1} $token;Check 'Lưu giỏ' $cart 200
$checkout=Request POST '/api/orders' @{cartRevision=(Json $cart).revision;recipient='Khách kiểm thử';phone='0901234567';address='12 Hà Nội';note='Kiểm thử snapshot'} $token "snapshot-$suffix"
Check 'Tạo đơn' $checkout 202
$id=(Json $checkout).id;$order=$null
for($i=0;$i-lt 30;$i++){Start-Sleep -Milliseconds 500;$detail=Request GET "/api/orders/$id" $null $token;Check 'Đọc đơn của mình' $detail 200;$order=Json $detail;if($order.state-in @('PLACED','FAILED')){break};$checks.RemoveAt($checks.Count-1)}
if($order.state-ne'PLACED'){throw "Đơn không hoàn tất bước giữ hàng: $($order.state)"}
if($order.items[0].categoryId-ne$product.categoryId){throw "Sai category snapshot: Order=$($order.items[0].categoryId), Catalog=$($product.categoryId)"}
$checks.Add(@{name='Category ID của dòng đơn khớp Catalog khi đặt';status='PASS'})
$cancel=Request POST "/api/orders/$id/state" @{state='CANCELLED'} $token;Check 'Hủy đơn và giải phóng tồn' $cancel 200
$result=@{at=[DateTime]::UtcNow.ToString('o');passed=$checks.Count;orderId=$id;categoryId=$order.items[0].categoryId;checks=$checks}
$result|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path (Split-Path $PSScriptRoot -Parent) 'docs/category-snapshot-smoke-result.json') -Encoding UTF8
Write-Host "PASS $($checks.Count) kiểm tra snapshot danh mục qua Gateway/MySQL."
