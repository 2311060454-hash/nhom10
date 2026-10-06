param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$login=Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method POST -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes((@{email='staff@shop.local';password=$env:SEED_PASSWORD}|ConvertTo-Json))) -TimeoutSec 20
$headers=@{Authorization="Bearer $($login.accessToken)"}
$sources=@(
 @{key='placed';path='/api/orders/manage?state=PLACED&page=0'},
 @{key='packing';path='/api/orders/manage?state=PACKING&page=0'},
 @{key='shipped';path='/api/orders/manage?state=SHIPPED&page=0'},
 @{key='returns';path='/api/orders/returns/manage?state=REQUESTED&page=0'},
 @{key='support';path='/api/support/manage?state=OPEN&page=0'},
 @{key='guest';path='/api/support/guest/manage?state=OPEN&page=0'},
 @{key='low';path='/api/inventory?low=true&page=0&size=1'}
)
$checks=[System.Collections.Generic.List[object]]::new()
foreach($source in $sources){
 $data=Invoke-RestMethod -Uri "$BaseUrl$($source.path)" -Headers $headers -TimeoutSec 20
 if($null-eq$data.totalElements-or$data.totalElements-lt 0){throw "Nguồn $($source.key) không trả totalElements hợp lệ"}
 $checks.Add(@{name=$source.key;status='PASS';totalElements=$data.totalElements})
}
$result=@{at=[DateTime]::UtcNow.ToString('o');passed=$checks.Count;checks=$checks}
$result|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $projectRoot 'docs/staff-dashboard-smoke-result.json') -Encoding UTF8
Write-Host "PASS $($checks.Count) nguồn dữ liệu dashboard STAFF qua Gateway/MySQL."
