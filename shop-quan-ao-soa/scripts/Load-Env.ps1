$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $projectRoot '.env'
if (-not (Test-Path -LiteralPath $envFile)) { throw 'Chạy scripts/Init-Local.ps1 để tạo .env trước.' }
Get-Content -LiteralPath $envFile -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') }
}
foreach ($required in @('DB_HOST','DB_PORT','DB_USER','DB_PASSWORD','JWT_SECRET','INTERNAL_API_KEY','SEED_PASSWORD')) {
    if (-not [Environment]::GetEnvironmentVariable($required,'Process')) { throw "Thiếu biến $required trong .env" }
}
