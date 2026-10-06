param([string]$Java = 'java.exe')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$runtime = Join-Path $projectRoot '.runtime'
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
$pidFile = Join-Path $runtime 'processes.json'
if (Test-Path -LiteralPath $pidFile) { throw 'Đã có thông tin phiên chạy. Chạy Stop-Local.ps1 trước.' }
$started = @()
try {
    foreach ($service in @(@{Name='auth-user-service';Port=8081},@{Name='catalog-service';Port=8082},@{Name='inventory-service';Port=8083},@{Name='payment-shipping-service';Port=8085},@{Name='promotion-review-service';Port=8086},@{Name='order-service';Port=8084},@{Name='reporting-notification-service';Port=8087},@{Name='api-gateway';Port=8080})) {
        if (Get-NetTCPConnection -LocalPort $service.Port -State Listen -ErrorAction SilentlyContinue) { throw "Cổng $($service.Port) đã được sử dụng." }
        $jar = Join-Path $projectRoot "backend/$($service.Name)/target/$($service.Name)-1.0.0.jar"
        if (-not (Test-Path -LiteralPath $jar)) { throw "Chưa có $jar. Chạy Build.ps1 trước." }
        $arguments = @('-Xms64m','-Xmx256m','-jar',('"'+$jar+'"'),'--spring.profiles.active=local')
        $process = Start-Process -FilePath $Java -ArgumentList $arguments -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtime "$($service.Name).out.log") -RedirectStandardError (Join-Path $runtime "$($service.Name).err.log")
        $started += @{Id=$process.Id;Name=$service.Name;StartedAt=$process.StartTime.ToUniversalTime().ToString('o')}
        $ready=$false
        for ($i=0; $i -lt 60; $i++) {
            if ($process.HasExited) { throw "$($service.Name) đã dừng. Xem .runtime/*.log" }
            try { $health=Invoke-RestMethod "http://127.0.0.1:$($service.Port)/actuator/health" -TimeoutSec 2; if ($health.status -eq 'UP') { $ready=$true; break } } catch { }
            Start-Sleep -Seconds 1
        }
        if (-not $ready) { throw "$($service.Name) chưa sẵn sàng sau 60 giây." }
        Write-Host "$($service.Name) sẵn sàng tại cổng $($service.Port)"
    }
} finally {
    if ($started.Count -gt 0) { ConvertTo-Json -InputObject @($started) | Set-Content -LiteralPath $pidFile -Encoding UTF8 }
}



