param([string]$DatabasePassword, [string]$SamplePassword)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $envFile) { throw '.env đã tồn tại. Chỉnh sửa trực tiếp để tránh ghi đè khóa hoặc mật khẩu.' }
if (-not $DatabasePassword) { $DatabasePassword = [System.Net.NetworkCredential]::new('', (Read-Host 'Mật khẩu MySQL local' -AsSecureString)).Password }
if (-not $SamplePassword) { $SamplePassword = [System.Net.NetworkCredential]::new('', (Read-Host 'Mật khẩu tài khoản mẫu (ít nhất 10 ký tự)' -AsSecureString)).Password }
if ($SamplePassword.Length -lt 10 -or [Text.Encoding]::UTF8.GetByteCount($SamplePassword) -gt 72) { throw 'Mật khẩu mẫu cần 10 ký tự trở lên và không quá 72 byte UTF-8.' }
if ($DatabasePassword.Contains("`n") -or $SamplePassword.Contains("`n")) { throw 'Mật khẩu không được chứa xuống dòng.' }
function New-LocalSecret {
    $bytes = New-Object byte[] 48
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes); return [Convert]::ToBase64String($bytes) } finally { $rng.Dispose() }
}
$lines = @('DB_HOST=localhost','DB_PORT=3310','DB_USER=root',"DB_PASSWORD=$DatabasePassword", "JWT_SECRET=$(New-LocalSecret)", "INTERNAL_API_KEY=$(New-LocalSecret)", "SEED_PASSWORD=$SamplePassword", 'CORS_ORIGIN=http://localhost:5173,http://127.0.0.1:5173')
[IO.File]::WriteAllLines($envFile,$lines,[Text.UTF8Encoding]::new($false))
Write-Host 'Đã tạo .env local, được gitignore. Không chia sẻ hoặc commit file này.'
