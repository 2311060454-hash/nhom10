param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
function Login($email){(Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method POST -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes((@{email=$email;password=$env:SEED_PASSWORD}|ConvertTo-Json)))).accessToken}
function Req($path,$token=''){$h=@{};if($token){$h.Authorization="Bearer $token"};Invoke-WebRequest -Uri "$BaseUrl$path" -Headers $h -SkipHttpErrorCheck -TimeoutSec 20}
$checks=[System.Collections.Generic.List[object]]::new()
function Check($name,$valid){if(-not$valid){throw $name};$checks.Add(@{name=$name;status='PASS'})}
$staff=Login 'staff@shop.local';$customer=Login 'customer@shop.local'
$count=(Invoke-RestMethod -Uri "$BaseUrl/api/inventory?size=1" -Headers @{Authorization="Bearer $staff"}).totalElements
$all=Req '/api/inventory/export.csv' $staff
Check 'STAFF xuất CSV' ($all.StatusCode-eq 200-and$all.Headers['Content-Disposition']-like '*bao-cao-ton-kho.csv*')
$lines=@($all.Content -split "`r?`n"|Where-Object {$_ -ne ''})
Check 'Số dòng bằng số biến thể trong MySQL' ($lines.Count-1-eq$count)
Check 'CSV có cột tồn kho và khả dụng' ($lines[0] -like '*Mã biến thể,Tồn thực tế,Đang giữ,Khả dụng,Mức tối thiểu,Sắp hết hàng*')
$first=(Invoke-RestMethod -Uri "$BaseUrl/api/inventory?size=1" -Headers @{Authorization="Bearer $staff"}).content[0].variantId
$one=Req "/api/inventory/export.csv?variantId=$first" $staff
Check 'Lọc theo ID biến thể chỉ xuất một dòng' ((@($one.Content -split "`r?`n"|Where-Object {$_ -ne ''})).Count-eq 2)
$lowCount=(Invoke-RestMethod -Uri "$BaseUrl/api/inventory?low=true&size=1" -Headers @{Authorization="Bearer $staff"}).totalElements
$low=Req '/api/inventory/export.csv?low=true' $staff
Check 'Bộ lọc sắp hết hàng khớp API' ((@($low.Content -split "`r?`n"|Where-Object {$_ -ne ''})).Count-1-eq$lowCount)
Check 'Khách chưa đăng nhập bị chặn' ((Req '/api/inventory/export.csv').StatusCode-eq 401)
Check 'CUSTOMER không xuất báo cáo kho' ((Req '/api/inventory/export.csv' $customer).StatusCode-eq 403)
$result=@{at=[DateTime]::UtcNow.ToString('o');passed=$checks.Count;checks=$checks;rowCount=$count}
$result|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $projectRoot 'docs/inventory-export-smoke-result.json') -Encoding UTF8
Write-Host "PASS $($checks.Count) kiểm tra CSV tồn kho qua Gateway/MySQL."
