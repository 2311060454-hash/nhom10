param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
function Login($email){(Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method POST -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes((@{email=$email;password=$env:SEED_PASSWORD}|ConvertTo-Json)))).accessToken}
function Req($path,$token=''){$h=@{};if($token){$h.Authorization="Bearer $token"};Invoke-WebRequest -Uri "$BaseUrl$path" -Headers $h -SkipHttpErrorCheck -TimeoutSec 30}
$checks=[System.Collections.Generic.List[object]]::new()
function Check($name,$valid){if(-not$valid){throw $name};$checks.Add(@{name=$name;status='PASS'})}
$admin=Login 'admin@shop.local';$staff=Login 'staff@shop.local';$customer=Login 'customer@shop.local'
$today=[DateTime]::UtcNow.AddHours(7).ToString('yyyy-MM-dd');$range="from=2020-01-01&to=$today"
$response=Req "/api/reports/categories?$range" $admin
Check 'ADMIN xem doanh thu danh mục' ($response.StatusCode-eq 200)
$report=$response.Content|ConvertFrom-Json
$revenue=(Req "/api/reports/revenue?$range&group=YEAR" $admin).Content|ConvertFrom-Json
Check 'Tổng danh mục và vận chuyển bằng doanh thu COD đã thu' ([decimal]$report.totalRevenue-eq[decimal]$revenue.realizedRevenue)
$sum=[decimal]0;foreach($row in $report.categories){$sum+=[decimal]$row.revenue}
Check 'Các danh mục cộng đúng doanh thu hàng hóa' ($sum-eq[decimal]$report.merchandiseRevenue)
Check 'Phí giao được tách khỏi hàng hóa' ([decimal]$report.merchandiseRevenue+[decimal]$report.shippingRevenue-eq[decimal]$report.totalRevenue)
$csv=Req "/api/reports/categories.csv?$range" $admin
Check 'ADMIN xuất CSV' ($csv.StatusCode-eq 200-and$csv.Headers['Content-Disposition']-like '*doanh-thu-danh-muc.csv*')
$lines=@($csv.Content -split "`r?`n"|Where-Object {$_ -ne ''})
Check 'CSV đủ danh mục' ($lines.Count-1-eq$report.categories.Count)
Check 'STAFF không xem doanh thu quản trị' ((Req "/api/reports/categories?$range" $staff).StatusCode-eq 403)
Check 'CUSTOMER không xem doanh thu quản trị' ((Req "/api/reports/categories?$range" $customer).StatusCode-eq 403)
Check 'Ngày đảo ngược trả 400' ((Req '/api/reports/categories?from=2026-09-25&to=2026-09-24' $admin).StatusCode-eq 400)
$result=@{at=[DateTime]::UtcNow.ToString('o');passed=$checks.Count;checks=$checks;categoryCount=$report.categories.Count;realizedRevenue=$report.totalRevenue}
$result|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $projectRoot 'docs/category-revenue-smoke-result.json') -Encoding UTF8
Write-Host "PASS $($checks.Count) kiểm tra doanh thu danh mục qua Gateway/MySQL."
