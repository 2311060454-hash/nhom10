param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
function Req($method,$path,$token=''){
 $headers=@{};if($token){$headers.Authorization="Bearer $token"}
 Invoke-WebRequest -Uri "$BaseUrl$path" -Method $method -Headers $headers -SkipHttpErrorCheck -TimeoutSec 20
}
function Json($r){$r.Content|ConvertFrom-Json}
$checks=[System.Collections.Generic.List[object]]::new()
function Check($name,$valid){if(-not$valid){throw $name};$checks.Add(@{name=$name;status='PASS'})}
$staff=(Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method POST -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes((@{email='staff@shop.local';password=$env:SEED_PASSWORD}|ConvertTo-Json)))).accessToken
$customer=(Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method POST -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes((@{email='customer@shop.local';password=$env:SEED_PASSWORD}|ConvertTo-Json)))).accessToken
$all=Json (Req GET '/api/orders/manage?page=0' $staff)
Check 'STAFF lấy danh sách từ MySQL' ($all.totalElements-ge 1-and$all.content.Count-ge 1)
$order=$all.content[0]
$date=([DateTimeOffset]::Parse($order.createdAt)).ToOffset([TimeSpan]::FromHours(7)).ToString('yyyy-MM-dd')
$idResult=Json (Req GET "/api/orders/manage?q=$($order.id)&page=0" $staff)
Check 'Tìm theo mã đơn' (@($idResult.content|Where-Object id -eq $order.id).Count-eq 1)
$phoneResult=Json (Req GET "/api/orders/manage?q=$([Uri]::EscapeDataString($order.phone))&page=0" $staff)
Check 'Tìm theo số điện thoại' (@($phoneResult.content|Where-Object id -eq $order.id).Count-eq 1)
$recipientResult=Json (Req GET "/api/orders/manage?q=$([Uri]::EscapeDataString($order.recipient))&page=0" $staff)
Check 'Tìm theo tên người nhận' (@($recipientResult.content|Where-Object id -eq $order.id).Count-eq 1)
$dateResult=Json (Req GET "/api/orders/manage?from=$date&to=$date&page=0" $staff)
Check 'Ngày đặt theo giờ Việt Nam gồm đơn thử' (@($dateResult.content|Where-Object id -eq $order.id).Count-eq 1)
Check 'Khoảng ngày đảo ngược bị chặn' ((Req GET '/api/orders/manage?from=2026-09-24&to=2026-09-23' $staff).StatusCode-eq 400)
Check 'CUSTOMER không tra cứu đơn toàn cửa hàng' ((Req GET '/api/orders/manage?q=test' $customer).StatusCode-eq 403)
$result=@{at=[DateTime]::UtcNow.ToString('o');passed=$checks.Count;checks=$checks;sampleOrderId=$order.id}
$result|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $projectRoot 'docs/order-search-smoke-result.json') -Encoding UTF8
Write-Host "PASS $($checks.Count) kiểm tra tìm đơn qua Gateway/MySQL."
