param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$results = [System.Collections.Generic.List[object]]::new()
function Request([string]$Method,[string]$Path,$Data=$null,[string]$Token='') {
    $headers=@{}
    if ($Token) { $headers.Authorization="Bearer $Token" }
    $parameters=@{Uri="$BaseUrl$Path";Method=$Method;Headers=$headers;ContentType='application/json; charset=utf-8';TimeoutSec=15;SkipHttpErrorCheck=$true}
    if ($null -ne $Data) { $parameters.Body=[Text.Encoding]::UTF8.GetBytes(($Data | ConvertTo-Json -Depth 8)) }
    return Invoke-WebRequest @parameters
}
function Expect([string]$Name,$Response,[int]$Code) {
    if ([int]$Response.StatusCode -ne $Code) { throw "$Name : mong đợi $Code, nhận $($Response.StatusCode)" }
    $results.Add(@{name=$Name;status='PASS';http=$Code})
}
$suffix=[Guid]::NewGuid().ToString('N').Substring(0,10)
$email="test-$suffix@example.com"
$password=[Guid]::NewGuid().ToString('N')
$response=Request POST '/api/auth/register' @{fullName='Khách kiểm thử';email=$email;phone='0901234567';password=$password}
Expect 'Đăng ký lưu database' $response 201
$newUser=$response.Content | ConvertFrom-Json
if ($newUser.roles[0] -ne 'CUSTOMER' -or $newUser.PSObject.Properties.Name -contains 'passwordHash') { throw 'DTO đăng ký không an toàn' }
$response=Request POST '/api/auth/register' @{fullName='Trùng email';email=$email;phone='0901234567';password=$password}
Expect 'Chặn email trùng' $response 409
$response=Request POST '/api/auth/login' @{email=$email;password=$password}
Expect 'Đăng nhập qua Gateway' $response 200
$token=($response.Content | ConvertFrom-Json).accessToken
Expect 'Lấy hồ sơ' (Request GET '/api/auth/me' $null $token) 200
Expect 'Khách không có quyền quản trị' (Request GET '/api/admin/users' $null $token) 403
$address=@{recipient='Người nhận';phone='0901234567';detail='12 Nguyễn Trãi, Hà Nội';defaultAddress=$true}
$response=Request POST '/api/addresses' $address $token
Expect 'Thêm địa chỉ' $response 201
$addressId=($response.Content | ConvertFrom-Json).id
$response=Request POST '/api/auth/login' @{email='customer@shop.local';password=$env:SEED_PASSWORD}
Expect 'Đăng nhập khách mẫu' $response 200
$otherToken=($response.Content | ConvertFrom-Json).accessToken
Expect 'Chặn truy cập địa chỉ người khác' (Request PUT "/api/addresses/$addressId" $address $otherToken) 404
Expect 'Xóa địa chỉ của mình' (Request DELETE "/api/addresses/$addressId" $null $token) 204
Expect 'Đăng xuất' (Request POST '/api/auth/logout' $null $token) 200
Expect 'JWT bị thu hồi' (Request GET '/api/auth/me' $null $token) 401
$response=Request POST '/api/auth/login' @{email='staff@shop.local';password=$env:SEED_PASSWORD}
Expect 'Đăng nhập nhân viên' $response 200
$staffToken=($response.Content | ConvertFrom-Json).accessToken
Expect 'Nhân viên không được quản lý quyền' (Request GET '/api/admin/users' $null $staffToken) 403
$response=Request POST '/api/auth/login' @{email='admin@shop.local';password=$env:SEED_PASSWORD}
Expect 'Đăng nhập quản trị' $response 200
$adminToken=($response.Content | ConvertFrom-Json).accessToken
Expect 'Quản trị xem tài khoản' (Request GET '/api/admin/users?size=5' $null $adminToken) 200
Expect 'Kiểm tra phân trang không hợp lệ' (Request GET '/api/admin/users?size=999' $null $adminToken) 400
Expect 'Nhật ký có dữ liệu' (Request GET '/api/admin/audit' $null $adminToken) 200
Expect 'Khóa tài khoản kiểm thử' (Request PUT "/api/admin/users/$($newUser.id)/access" @{role='CUSTOMER';active=$false;inventoryWrite=$false} $adminToken) 200
Expect 'API báo cáo không tồn tại trả 404' (Request GET '/api/reports/not-found' $null $adminToken) 404
Expect 'Không công khai API nội bộ' (Request GET '/internal/sessions/missing?userId=1') 401
Expect 'Đăng xuất khách mẫu' (Request POST '/api/auth/logout' $null $otherToken) 200
Expect 'Đăng xuất nhân viên' (Request POST '/api/auth/logout' $null $staffToken) 200
Expect 'Đăng xuất quản trị' (Request POST '/api/auth/logout' $null $adminToken) 200
$report=@{timestamp=(Get-Date).ToUniversalTime().ToString('o');baseUrl=$BaseUrl;database='MySQL localhost:3310/shop_quan_ao_auth';tests=@($results.ToArray());note='Tạo một tài khoản test, khóa ở cuối. Không xóa dữ liệu người dùng.'}
$output=Join-Path $projectRoot 'docs/auth-smoke-result.json'
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $output -Encoding UTF8
Write-Host "$($results.Count) kiểm tra HTTP thành công. Báo cáo: $output"


