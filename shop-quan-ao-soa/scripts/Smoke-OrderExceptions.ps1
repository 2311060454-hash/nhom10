param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
function Login($email){(Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method POST -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes((@{email=$email;password=$env:SEED_PASSWORD}|ConvertTo-Json)))).accessToken}
function Req($path,$token=''){$h=@{};if($token){$h.Authorization="Bearer $token"};Invoke-WebRequest -Uri "$BaseUrl$path" -Headers $h -SkipHttpErrorCheck -TimeoutSec 30}
$checks=[System.Collections.Generic.List[object]]::new()
function Check($name,$valid){if(-not$valid){throw $name};$checks.Add(@{name=$name;status='PASS'})}
$admin=Login 'admin@shop.local';$staff=Login 'staff@shop.local';$customer=Login 'customer@shop.local'
$today=[DateTime]::UtcNow.AddHours(7).ToString('yyyy-MM-dd');$query="from=2020-01-01&to=$today"
$reportResponse=Req "/api/reports/exceptions?$query" $admin
Check 'ADMIN xem báo cáo' ($reportResponse.StatusCode-eq 200)
$report=$reportResponse.Content|ConvertFrom-Json
Check 'Tổng sự kiện bằng số dòng chi tiết' ($report.rows.Count-eq($report.cancelledOrders+$report.fullReturns+$report.partialReturns))
Check 'Tiền hoàn COD và mô phỏng không âm' ($report.codRefunds-ge 0-and$report.simulatedRefunds-ge 0)
$csv=Req "/api/reports/exceptions.csv?$query" $admin
Check 'ADMIN xuất CSV' ($csv.StatusCode-eq 200-and$csv.Headers['Content-Disposition']-like '*don-huy-hoan.csv*')
$lines=@($csv.Content -split "`r?`n"|Where-Object {$_ -ne ''})
Check 'CSV khớp số dòng báo cáo' ($lines.Count-1-eq$report.rows.Count)
Check 'CSV có các cột số tiền hoàn' ($lines[0] -like '*ma_don,loai,phuong_thuc,gia_tri_don_vnd,tien_hoan_vnd,thoi_diem_utc*')
Check 'STAFF không xem báo cáo quản trị' ((Req "/api/reports/exceptions?$query" $staff).StatusCode-eq 403)
Check 'CUSTOMER không xem báo cáo' ((Req "/api/reports/exceptions?$query" $customer).StatusCode-eq 403)
Check 'Khoảng ngày đảo ngược bị chặn' ((Req '/api/reports/exceptions?from=2026-09-25&to=2026-09-24' $admin).StatusCode-eq 400)
$result=@{at=[DateTime]::UtcNow.ToString('o');passed=$checks.Count;checks=$checks;cancelled=$report.cancelledOrders;fullReturns=$report.fullReturns;partialReturns=$report.partialReturns}
$result|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $projectRoot 'docs/order-exceptions-smoke-result.json') -Encoding UTF8
Write-Host "PASS $($checks.Count) kiểm tra báo cáo đơn hủy/hoàn qua Gateway/MySQL."
