$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$pidFile = Join-Path $projectRoot '.runtime/processes.json'
if (-not (Test-Path -LiteralPath $pidFile)) { Write-Host 'Không có phiên chạy do script tạo.'; return }
foreach ($entry in (Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json)) {
    $process = Get-Process -Id $entry.Id -ErrorAction SilentlyContinue
    if ($process) {
        $expectedStart = ([DateTime]$entry.StartedAt).ToUniversalTime()
        if ($process.ProcessName -ne 'java' -or [Math]::Abs(($process.StartTime.ToUniversalTime() - $expectedStart).TotalSeconds) -ge 1) {
            throw "PID $($entry.Id) không khớp phiên đã tạo. Giữ nguyên process và file trạng thái để kiểm tra."
        }
        & taskkill.exe /PID $entry.Id /F | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Không dừng được PID $($entry.Id). Giữ lại file trạng thái." }
        Write-Host "Đã dừng $($entry.Name)"
    }
}
Remove-Item -LiteralPath $pidFile
