param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')

$migration = Get-Content -LiteralPath (Join-Path $projectRoot 'backend/catalog-service/src/main/resources/db/migration/V3__store_content_defaults.sql') -Raw -Encoding UTF8
$pattern = "(?m)^UPDATE store_content SET content_text = '([^']*)', updated_at = UTC_TIMESTAMP\(6\)\r?\nWHERE content_key = '([A-Z_]+)' AND TRIM\(content_text\) = '';"
$matches = [regex]::Matches($migration, $pattern)
if ($matches.Count -ne 5) { throw 'Không đọc được đủ 5 nội dung mẫu từ migration.' }

$loginBody = @{ email = 'admin@shop.local'; password = $env:SEED_PASSWORD } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($loginBody)) -TimeoutSec 15
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$current = Invoke-RestMethod -Uri "$BaseUrl/api/catalog/content" -Method Get -TimeoutSec 15
foreach ($entry in $matches) {
    $key = $entry.Groups[2].Value
    $row = @($current | Where-Object key -eq $key)[0]
    if ($null -ne $row -and -not [string]::IsNullOrWhiteSpace($row.text)) {
        Write-Host "Giữ nguyên $key (đã có nội dung)."
        continue
    }
    $body = @{ text = $entry.Groups[1].Value.Replace('\n', "`n") } | ConvertTo-Json -Depth 3
    Invoke-RestMethod -Uri "$BaseUrl/api/catalog/manage/content/$key" -Method Put -Headers $headers -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($body)) -TimeoutSec 15 | Out-Null
    Write-Host "Đã thêm nội dung $key."
}
