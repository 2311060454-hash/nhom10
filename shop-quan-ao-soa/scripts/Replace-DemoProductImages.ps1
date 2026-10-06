param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')

$items = @(
    @{ Id = 30; Name = 'Quần short đũi mùa hè'; File = '30-shorts-linen.png' }
    @{ Id = 22; Name = 'Áo hoodie nỉ bông Oversize'; File = '22-hoodie-oversize.png' }
    @{ Id = 20; Name = 'Đầm suông linen dáng dài'; File = '20-linen-dress.png' }
    @{ Id = 17; Name = 'Áo len dệt kim Thu Đông'; File = '17-knit-sweater.png' }
    @{ Id = 14; Name = 'Quần tây Smart Casual'; File = '14-smart-trousers.png' }
    @{ Id = 13; Name = 'Áo khoác Blazer Minimalist'; File = '13-minimal-blazer.png' }
    @{ Id = 6; Name = 'Áo kiểu cổ tròn'; File = '6-round-neck-top.png' }
    @{ Id = 5; Name = 'Chân váy midi mềm mại'; File = '5-midi-skirt.png' }
    @{ Id = 4; Name = 'Áo polo Classic'; File = '4-classic-polo.png' }
    @{ Id = 3; Name = 'Quần dáng suông Everyday'; File = '3-wide-trousers.png' }
    @{ Id = 2; Name = 'Sơ mi linen thanh lịch'; File = '2-linen-shirt.png' }
    @{ Id = 1; Name = 'Áo thun cotton Essential'; File = '1-cotton-tee.png' }
)

$body = @{ email = 'admin@shop.local'; password = $env:SEED_PASSWORD } | ConvertTo-Json
$session = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($body))
$headers = @{ Authorization = "Bearer $($session.accessToken)" }
$photoRoot = Join-Path $projectRoot 'assets/product-photography'

foreach ($item in $items) {
    $path = Join-Path $photoRoot $item.File
    if (-not (Test-Path -LiteralPath $path)) { throw "Thiếu ảnh: $path" }
    $product = Invoke-RestMethod -Uri "$BaseUrl/api/catalog/products/$($item.Id)"
    if ($product.name -ne $item.Name) { throw "ID $($item.Id) không còn là $($item.Name); dừng để tránh sửa nhầm ảnh." }

    $oldImages = @($product.images)
    $client = [Net.Http.HttpClient]::new()
    $client.DefaultRequestHeaders.Authorization = [Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $session.accessToken)
    $stream = [IO.File]::OpenRead($path)
    $multipart = [Net.Http.MultipartFormDataContent]::new()
    $part = [Net.Http.StreamContent]::new($stream)
    $part.Headers.ContentType = [Net.Http.Headers.MediaTypeHeaderValue]::Parse('image/png')
    $multipart.Add($part, 'file', $item.File)
    try {
        $response = $client.PostAsync("$BaseUrl/api/catalog/manage/products/$($item.Id)/images", $multipart).GetAwaiter().GetResult()
        $json = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) { throw "Tải ảnh $($item.Name) thất bại: HTTP $([int]$response.StatusCode) $json" }
        $uploaded = $json | ConvertFrom-Json
    } finally {
        $multipart.Dispose()
        $stream.Dispose()
        $client.Dispose()
    }
    $check = Invoke-WebRequest -Uri "$BaseUrl$($uploaded.url)" -Method Get
    if ($check.StatusCode -ne 200 -or $check.Headers['Content-Type'] -notlike 'image/png*') { throw "Ảnh mới của $($item.Name) không đọc được." }

    foreach ($old in $oldImages) {
        Invoke-RestMethod -Uri "$BaseUrl/api/catalog/manage/products/$($item.Id)/images/$($old.id)" -Method Delete -Headers $headers | Out-Null
    }
    $updated = Invoke-RestMethod -Uri "$BaseUrl/api/catalog/products/$($item.Id)"
    if ($updated.images.Count -ne 1 -or $updated.images[0].id -ne $uploaded.id) { throw "Album của $($item.Name) chưa được thay đúng." }
    Write-Host "Đã thay ảnh: $($item.Name)"
}
Write-Host "Hoàn tất $($items.Count) sản phẩm. Đây là ảnh demo tạo bằng AI, không phải ảnh của hàng tồn thực tế."
