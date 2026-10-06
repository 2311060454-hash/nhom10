param([string]$Maven = 'mvn.cmd')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (Test-Path -LiteralPath (Join-Path $projectRoot '.runtime/processes.json')) { throw 'Dừng backend bằng Stop-Local.ps1 trước khi build; Windows khóa jar đang chạy.' }
& $Maven -f (Join-Path $projectRoot 'backend/pom.xml') clean verify -B -ntp
if ($LASTEXITCODE -ne 0) { throw 'Backend build hoặc test thất bại.' }
Push-Location (Join-Path $projectRoot 'frontend')
try {
    & npm.cmd ci
    if ($LASTEXITCODE -ne 0) { throw 'npm ci thất bại.' }
    & npm.cmd test
    if ($LASTEXITCODE -ne 0) { throw 'Frontend test thất bại.' }
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build thất bại.' }
} finally { Pop-Location }
