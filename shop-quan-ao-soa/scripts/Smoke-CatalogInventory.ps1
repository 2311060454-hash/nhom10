$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$results=[Collections.Generic.List[object]]::new()
function Call([string]$Method,[string]$Path,$Body=$null,[string]$Token='', [string]$Key='', [bool]$Internal=$false) {
    $headers=@{}
    if($Token){$headers.Authorization="Bearer $Token"}
    if($Key){$headers['Idempotency-Key']=$Key}
    $origin='http://127.0.0.1:8080'
    if($Internal){$origin='http://127.0.0.1:8083';$headers['X-Internal-Key']=$env:INTERNAL_API_KEY}
    $args=@{Uri="$origin$Path";Method=$Method;Headers=$headers;ContentType='application/json; charset=utf-8';SkipHttpErrorCheck=$true;TimeoutSec=20}
    if($null -ne $Body){$args.Body=[Text.Encoding]::UTF8.GetBytes(($Body|ConvertTo-Json -Depth 12))}
    $r=Invoke-WebRequest @args
    $json=$null
    if($r.Content -and $r.Headers['Content-Type'] -like '*application/json*'){$json=$r.Content|ConvertFrom-Json}
    return [pscustomobject]@{Code=[int]$r.StatusCode;Data=$json;Raw=$r}
}
function Check([string]$Name,$Response,[int]$Expected){if($Response.Code -ne $Expected){throw "$Name : expected=$Expected actual=$($Response.Code) $($Response.Data.message)"};$results.Add(@{name=$Name;status='PASS';http=$Expected})}
function Assert([string]$Name,[bool]$Condition){if(-not $Condition){throw $Name};$results.Add(@{name=$Name;status='PASS'})}
$login=Call POST '/api/auth/login' @{email='admin@shop.local';password=$env:SEED_PASSWORD};Check 'Admin login' $login 200;$admin=$login.Data.accessToken
$catalog=Call GET '/api/catalog/products?sort=priceAsc';Check 'Danh sách và sắp xếp MySQL' $catalog 200
Assert 'Có dữ liệu mẫu sản phẩm' ($catalog.Data.totalElements -ge 6)
$categories=(Call GET '/api/catalog/categories').Data;$brands=(Call GET '/api/catalog/brands').Data
$suffix=[Guid]::NewGuid().ToString('N').Substring(0,12)
$input=@{name="Sản phẩm kiểm thử $suffix";categoryId=$categories[0].id;brandId=$brands[0].id;description='Dữ liệu test local';material='Cotton';style='Suông';gender='UNISEX';active=$true;featured=$false;version=$null;variants=@(@{id=$null;sku="TEST-$suffix-S";size='S';color='Đen';costPrice=100000;price=250000;salePrice=200000;active=$true},@{id=$null;sku="TEST-$suffix-M";size='M';color='Trắng';costPrice=100000;price=250000;salePrice=$null;active=$true})}
$created=Call POST '/api/catalog/manage/products' $input $admin;Check 'Tạo sản phẩm hai biến thể' $created 201;$product=$created.Data;$variant=[long]$product.variants[0].id
$public=Call GET "/api/catalog/products/$($product.id)";Check 'Chi tiết công khai' $public 200;Assert 'Không lộ giá vốn' ($null -eq $public.Data.variants[0].costPrice)
$filter=Call GET "/api/catalog/products?q=$suffix&size=S&color=%C4%90en&sale=true";Check 'Lọc size màu khuyến mãi' $filter 200;Assert 'Lọc đúng sản phẩm' ($filter.Data.totalElements -eq 1)
Check 'Chặn quản trị không có JWT' (Call POST '/api/catalog/manage/products' $input) 401
$payload=@{variantId=$variant;type='RECEIPT';quantity=1;reason='Kiểm thử tồn cuối'};$key=[Guid]::NewGuid().ToString('N')
Check 'Nhập một đơn vị' (Call POST '/api/inventory/adjustments' $payload $admin $key) 200
$replayed=Call POST '/api/inventory/adjustments' $payload $admin $key;Check 'Retry nhập kho idempotent' $replayed 200;Assert 'Không cộng kho hai lần' ($replayed.Data.onHand -eq 1)
Check 'Chặn xuất vượt tồn' (Call POST '/api/inventory/adjustments' @{variantId=$variant;type='ISSUE';quantity=2;reason='Test'} $admin ([Guid]::NewGuid().ToString('N'))) 409
$orders=@([Guid]::NewGuid().ToString(),[Guid]::NewGuid().ToString());$requests=@();$tasks=@();$client=[Net.Http.HttpClient]::new()
try {
    foreach($order in $orders){$request=[Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::Post,"http://127.0.0.1:8083/internal/inventory/reservations/$order");$request.Headers.Add('X-Internal-Key',$env:INTERNAL_API_KEY);$request.Content=[Net.Http.StringContent]::new((@{items=@(@{variantId=$variant;quantity=1})}|ConvertTo-Json -Depth 5),[Text.Encoding]::UTF8,'application/json');$requests+=$request;$tasks+=$client.SendAsync($request)}
    [Threading.Tasks.Task]::WaitAll([Threading.Tasks.Task[]]$tasks)
    $codes=@($tasks|ForEach-Object{[int]$_.Result.StatusCode}|Sort-Object)
    Assert 'MySQL: hai khách tranh một đơn vị chỉ một thành công' (($codes -join ',') -eq '200,409')
    $winner=if([int]$tasks[0].Result.StatusCode -eq 200){$orders[0]}else{$orders[1]}
} finally {foreach($request in $requests){$request.Dispose()};$client.Dispose()}
Assert 'Tồn khả dụng bằng 0 sau giữ' ((Call GET "/api/inventory/availability/$variant").Data.available -eq 0)
Check 'Giải phóng giữ kho' (Call POST "/internal/inventory/reservations/$winner/release" $null '' '' $true) 200
Check 'Giải phóng lặp an toàn' (Call POST "/internal/inventory/reservations/$winner/release" $null '' '' $true) 200
Assert 'Hoàn đúng một đơn vị' ((Call GET "/api/inventory/availability/$variant").Data.available -eq 1)
$order=[Guid]::NewGuid().ToString();$hold=@{items=@(@{variantId=$variant;quantity=1})}
Check 'Giữ kho lần mới' (Call POST "/internal/inventory/reservations/$order" $hold '' '' $true) 200
Check 'Commit kho' (Call POST "/internal/inventory/reservations/$order/commit" $null '' '' $true) 200
Check 'Commit lặp' (Call POST "/internal/inventory/reservations/$order/commit" $null '' '' $true) 200
Check 'Hoàn hàng đã xuất' (Call POST "/internal/inventory/reservations/$order/restock" $null '' '' $true) 200
Check 'Hoàn hàng lặp' (Call POST "/internal/inventory/reservations/$order/restock" $null '' '' $true) 200
Assert 'Kho sau hoàn vẫn bằng 1' ((Call GET "/api/inventory/availability/$variant").Data.available -eq 1)
$password=[Guid]::NewGuid().ToString('N');$email="staff-$suffix@example.com"
$staff=Call POST '/api/admin/users/staff' @{fullName='Nhân viên kiểm thử';email=$email;phone='0900000000';password=$password;inventoryWrite=$false} $admin;Check 'Tạo STAFF chưa có quyền kho' $staff 201
$staffToken=(Call POST '/api/auth/login' @{email=$email;password=$password}).Data.accessToken
Check 'STAFF thiếu quyền bị từ chối ghi kho' (Call POST '/api/inventory/adjustments' $payload $staffToken ([Guid]::NewGuid().ToString('N'))) 403
Check 'STAFF không sửa sản phẩm' (Call POST '/api/catalog/manage/products' $input $staffToken) 403
Check 'Khóa STAFF kiểm thử' (Call PUT "/api/admin/users/$($staff.Data.id)/access" @{role='STAFF';active=$false;inventoryWrite=$false} $admin) 200
$input.version=$product.version
for($i=0;$i -lt 2;$i++){$input.variants[$i].id=$product.variants[$i].id}
$input.name="Đã sửa $suffix";$updated=Call PUT "/api/catalog/manage/products/$($product.id)" $input $admin;Check 'Sửa sản phẩm' $updated 200
Check 'Chặn ghi đè phiên bản cũ' (Call PUT "/api/catalog/manage/products/$($product.id)" $input $admin) 409
Add-Type -AssemblyName System.Drawing
$bitmap=[Drawing.Bitmap]::new(8,8);$stream=[IO.MemoryStream]::new()
try{$bitmap.Save($stream,[Drawing.Imaging.ImageFormat]::Png);$bytes=$stream.ToArray()}finally{$bitmap.Dispose();$stream.Dispose()}
$client=[Net.Http.HttpClient]::new();$multipart=[Net.Http.MultipartFormDataContent]::new();$content=[Net.Http.ByteArrayContent]::new($bytes);$content.Headers.ContentType=[Net.Http.Headers.MediaTypeHeaderValue]::new('image/png');$multipart.Add($content,'file','test.png');$client.DefaultRequestHeaders.Authorization=[Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer',$admin)
try{$response=$client.PostAsync("http://127.0.0.1:8080/api/catalog/manage/products/$($product.id)/images",$multipart).GetAwaiter().GetResult();Assert 'Upload ảnh qua Gateway' ([int]$response.StatusCode -eq 200);$image=$response.Content.ReadAsStringAsync().GetAwaiter().GetResult()|ConvertFrom-Json;Assert 'Đọc ảnh đã lưu' ((Invoke-WebRequest "http://127.0.0.1:8080$($image.url)").StatusCode -eq 200)}finally{$multipart.Dispose();$client.Dispose()}
Check 'Ngừng bán sản phẩm test' (Call DELETE "/api/catalog/manage/products/$($product.id)" $null $admin) 204
Check 'Sản phẩm ngừng bán không xuất hiện công khai' (Call GET "/api/catalog/products/$($product.id)") 404
Check 'Lịch sử kho thực tế' (Call GET "/api/inventory/history?variantId=$variant" $null $admin) 200
Check 'Logout quản trị' (Call POST '/api/auth/logout' $null $admin) 200
@{timestamp=(Get-Date).ToUniversalTime().ToString('o');database='MySQL localhost:3310';tests=@($results.ToArray());note='Tạo sản phẩm test, ngừng bán ở cuối; STAFF test bị khóa. Bảo toàn lịch sử kho.'}|ConvertTo-Json -Depth 8|Set-Content (Join-Path $projectRoot 'docs/catalog-inventory-smoke-result.json') -Encoding UTF8
Write-Host "$($results.Count) kiểm tra Catalog/Inventory trên MySQL thành công."
