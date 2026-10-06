param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$checks=[Collections.Generic.List[object]]::new()
function Req($method,$path,$body=$null,$token=''){
 $headers=@{};if($token){$headers.Authorization="Bearer $token"}
 $args=@{Uri="$BaseUrl$path";Method=$method;Headers=$headers;ContentType='application/json; charset=utf-8';SkipHttpErrorCheck=$true;TimeoutSec=20}
 if($null-ne$body){$args.Body=[Text.Encoding]::UTF8.GetBytes(($body|ConvertTo-Json -Depth 8))}
 Invoke-WebRequest @args
}
function Json($response){$response.Content|ConvertFrom-Json}
function Check($name,$response,$expected){
 if([int]$response.StatusCode-ne$expected){throw "$name HTTP $($response.StatusCode): $($response.Content)"}
 $checks.Add(@{name=$name;status='PASS'})
}
function Assert($name,$condition){if(-not$condition){throw $name};$checks.Add(@{name=$name;status='PASS'})}
$admin=(Json (Req POST '/api/auth/login' @{email='admin@shop.local';password=$env:SEED_PASSWORD})).accessToken
$staff=(Json (Req POST '/api/auth/login' @{email='staff@shop.local';password=$env:SEED_PASSWORD})).accessToken
Assert 'Đăng nhập ADMIN và STAFF' ($admin-and$staff)
$contents=Req GET '/api/catalog/content';Check 'Đọc nội dung công khai' $contents 200
$old=(@((Json $contents)|Where-Object key -eq 'CONTACT'))[0].text
$text="Liên hệ kiểm thử $([Guid]::NewGuid().ToString('N').Substring(0,8))"
Check 'STAFF không sửa nội dung' (Req PUT '/api/catalog/manage/content/CONTACT' @{text=$text} $staff) 403
Check 'ADMIN sửa nội dung' (Req PUT '/api/catalog/manage/content/CONTACT' @{text=$text} $admin) 200
$saved=@((Json (Req GET '/api/catalog/content'))|Where-Object key -eq 'CONTACT')
Assert 'Nội dung lưu và đọc lại từ MySQL' ($saved[0].text -eq $text)
Check 'Không cho sửa key không hợp lệ' (Req PUT '/api/catalog/manage/content/INVALID' @{text='x'} $admin) 404
Check 'Khôi phục nội dung liên hệ ban đầu' (Req PUT '/api/catalog/manage/content/CONTACT' @{text=$old} $admin) 200
$from=(Get-Date).ToUniversalTime().AddMinutes(-1).ToString('o')
$until=(Get-Date).ToUniversalTime().AddDays(1).ToString('o')
$title="Banner kiểm thử $([Guid]::NewGuid().ToString('N').Substring(0,8))"
$body=@{title=$title;subtitle='Ảnh thật trên MySQL';linkPath='/';sortOrder=1;startsAt=$from;endsAt=$until;active=$true}
Check 'STAFF không tạo banner' (Req POST '/api/catalog/manage/banners' $body $staff) 403
$external=$body.Clone();$external.linkPath='https://example.com'
Check 'Chặn link ngoài trang' (Req POST '/api/catalog/manage/banners' $external $admin) 400
$made=Req POST '/api/catalog/manage/banners' $body $admin;Check 'Tạo banner' $made 201
$id=(Json $made).id
$visible=@((Json (Req GET '/api/catalog/banners'))|Where-Object id -eq $id)
Assert 'Banner có hiệu lực hiển thị công khai' ($visible.Count -eq 1)
Add-Type -AssemblyName System.Drawing
$bitmap=[Drawing.Bitmap]::new(8,8);$stream=[IO.MemoryStream]::new()
try{$bitmap.Save($stream,[Drawing.Imaging.ImageFormat]::Png);$bytes=$stream.ToArray()}finally{$bitmap.Dispose();$stream.Dispose()}
$client=[Net.Http.HttpClient]::new()
$client.DefaultRequestHeaders.Authorization=[Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer',$admin)
try{
 $multipart=[Net.Http.MultipartFormDataContent]::new()
 $image=[Net.Http.ByteArrayContent]::new($bytes);$image.Headers.ContentType=[Net.Http.Headers.MediaTypeHeaderValue]::new('image/png')
 $multipart.Add($image,'file','banner.png')
 $response=$client.PostAsync("$BaseUrl/api/catalog/manage/banners/$id/image",$multipart).GetAwaiter().GetResult()
 Assert 'Tải ảnh banner qua Gateway' ([int]$response.StatusCode-eq 200)
 $url=($response.Content.ReadAsStringAsync().GetAwaiter().GetResult()|ConvertFrom-Json).imageUrl
 $read=Req GET $url;Check 'Ảnh công khai đọc được' $read 200
 Assert 'Ảnh PNG thực' ($read.Headers['Content-Type']-like '*image/png*')
 $multipart.Dispose()
 $invalid=[Net.Http.MultipartFormDataContent]::new()
 $fake=[Net.Http.ByteArrayContent]::new([Text.Encoding]::UTF8.GetBytes('fake'));$fake.Headers.ContentType=[Net.Http.Headers.MediaTypeHeaderValue]::new('image/png')
 $invalid.Add($fake,'file','fake.png')
 $bad=$client.PostAsync("$BaseUrl/api/catalog/manage/banners/$id/image",$invalid).GetAwaiter().GetResult()
 Assert 'Từ chối ảnh giả' ([int]$bad.StatusCode-eq 400)
 $invalid.Dispose()
}finally{$client.Dispose()}
$body.active=$false
Check 'Ngừng hiển thị banner' (Req PUT "/api/catalog/manage/banners/$id" $body $admin) 200
$hidden=@((Json (Req GET '/api/catalog/banners'))|Where-Object id -eq $id)
Assert 'Banner tắt không còn công khai' ($hidden.Count -eq 0)
Check 'Banner vẫn có trong trang quản trị' (Req GET '/api/catalog/manage/banners' $null $admin) 200
@{timestamp=(Get-Date).ToUniversalTime().ToString('o');database='MySQL localhost:3310';tests=@($checks.ToArray());note='Banner kiểm thử được tắt; nội dung liên hệ được khôi phục.'}|ConvertTo-Json -Depth 8|Set-Content (Join-Path $projectRoot 'docs/store-content-smoke-result.json') -Encoding UTF8
Write-Host "$($checks.Count) kiểm tra banner/nội dung trên MySQL thành công."
