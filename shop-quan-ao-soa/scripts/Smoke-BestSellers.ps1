param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$checks=[System.Collections.Generic.List[object]]::new()
function Check($name,$condition){if(-not$condition){throw $name};$checks.Add(@{name=$name;status='PASS'})}
$ranking=Invoke-RestMethod 'http://127.0.0.1:8087/internal/reports/best-sellers' -Headers @{'X-Internal-Key'=$env:INTERNAL_API_KEY} -TimeoutSec 30
$first=Invoke-RestMethod "$BaseUrl/api/catalog/products?sort=bestSelling&limit=3" -TimeoutSec 30
$second=Invoke-RestMethod "$BaseUrl/api/catalog/products?sort=bestSelling&limit=3&page=1" -TimeoutSec 30
$all=Invoke-RestMethod "$BaseUrl/api/catalog/products?sort=bestSelling&limit=100" -TimeoutSec 30
Check 'Danh sách công khai có phân trang' ($first.content.Count-le 3-and$second.content.Count-le 3-and$first.totalElements-eq$all.totalElements)
Check 'Hai trang không lặp sản phẩm' (@($first.content|Where-Object {$second.content.id -contains $_.id}).Count-eq 0)
$expected=@($ranking|Where-Object {$all.content.id -contains $_.productId}|Sort-Object @{Expression='quantity';Descending=$true},@{Expression='productId';Descending=$true}|ForEach-Object productId)
$actual=@($all.content|Select-Object -First $expected.Count|ForEach-Object id)
Check 'Thứ tự khớp doanh số Order qua Reporting' (($expected -join ',')-eq($actual -join ','))
$withoutKey=Invoke-WebRequest 'http://127.0.0.1:8087/internal/reports/best-sellers' -SkipHttpErrorCheck -TimeoutSec 10
Check 'Endpoint thống kê nội bộ cần khóa service' ($withoutKey.StatusCode-in @(401,403))
$result=@{at=[DateTime]::UtcNow.ToString('o');passed=$checks.Count;productCount=$all.totalElements;rankingCount=@($ranking).Count;checks=$checks}
$result|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $projectRoot 'docs/best-sellers-smoke-result.json') -Encoding UTF8
Write-Host "PASS $($checks.Count) kiểm tra sản phẩm bán chạy qua Gateway/MySQL."
