$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$pidFile=Join-Path $projectRoot '.runtime/frontend-process.json'
if(-not(Test-Path -LiteralPath $pidFile)){Write-Host 'Không có Vite dev server do script ghi nhận.';return}
$entry=Get-Content -LiteralPath $pidFile -Raw|ConvertFrom-Json
$process=Get-Process -Id $entry.Id -ErrorAction SilentlyContinue
if($process){
 $expected=([DateTime]$entry.StartedAt).ToUniversalTime()
 if($process.ProcessName -ne 'node'-or[Math]::Abs(($process.StartTime.ToUniversalTime()-$expected).TotalSeconds)-ge 1){
  throw 'PID frontend không khớp process ban đầu. Giữ nguyên để kiểm tra.'
 }
 & taskkill.exe /PID $entry.Id /F | Out-Null
 if($LASTEXITCODE-ne 0){throw 'Không dừng được Vite dev server.'}
 Write-Host 'Đã dừng Vite dev server.'
}
Remove-Item -LiteralPath $pidFile
