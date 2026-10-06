param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$checks=[System.Collections.Generic.List[object]]::new()
function Req($method,$path,$body=$null,$token='',$key=''){
 $headers=@{};if($token){$headers.Authorization="Bearer $token"};if($key){$headers['Idempotency-Key']=$key}
 $args=@{Uri="$BaseUrl$path";Method=$method;Headers=$headers;ContentType='application/json; charset=utf-8';SkipHttpErrorCheck=$true;TimeoutSec=20}
 if($null-ne$body){$args.Body=[Text.Encoding]::UTF8.GetBytes(($body|ConvertTo-Json -Depth 10))};Invoke-WebRequest @args
}
function ReadJson($r){$r.Content|ConvertFrom-Json}
function Check($name,$r,$status){if([int]$r.StatusCode-ne$status){throw "$name HTTP $($r.StatusCode): $($r.Content)"};$checks.Add(@{name=$name;status='PASS'})}
function Assert($name,$valid){if(-not$valid){throw $name};$checks.Add(@{name=$name;status='PASS'})}
$suffix=[Guid]::NewGuid().ToString('N').Substring(0,10)
$body=@{fullName='Nguyễn Minh Anh';email="guest-$suffix@example.com";phone='0901234567';subject='Hỏi về đổi size';message='Tôi muốn hỏi cách đổi size sản phẩm đã mua'}
$key="guest-$suffix"
$made=Req POST '/api/support/guest' $body '' $key;Check 'Khách chưa đăng nhập gửi liên hệ' $made 201
$receipt=ReadJson $made;$id=$receipt.id
Assert 'Biên nhận chỉ có mã, không lộ thông tin cá nhân' ($id-and-not($receipt.PSObject.Properties.Name-contains'email')-and-not($receipt.PSObject.Properties.Name-contains'message'))
$retry=Req POST '/api/support/guest' $body '' $key;Check 'Gửi lại cùng key' $retry 201
Assert 'Không tạo yêu cầu trùng' ((ReadJson $retry).id-eq$id)
$changed=$body.Clone();$changed.subject='Hỏi về giao hàng'
Check 'Cùng key khác nội dung bị chặn' (Req POST '/api/support/guest' $changed '' $key) 409
Check 'Không có trang xem chi tiết công khai' (Req GET "/api/support/guest/manage/$id") 401
$staff=(ReadJson (Req POST '/api/auth/login' @{email='staff@shop.local';password=$env:SEED_PASSWORD})).accessToken
Assert 'STAFF đăng nhập' $staff
$customerEmail="guest-customer-$suffix@example.com";$customerPassword=[Guid]::NewGuid().ToString('N')
Check 'Đăng ký khách kiểm tra quyền' (Req POST '/api/auth/register' @{fullName='Khách kiểm thử';email=$customerEmail;phone='0901234567';password=$customerPassword}) 201
$customer=(ReadJson (Req POST '/api/auth/login' @{email=$customerEmail;password=$customerPassword})).accessToken
Check 'CUSTOMER không xem hàng chờ' (Req GET '/api/support/guest/manage' $null $customer) 403
Check 'CUSTOMER không sửa liên hệ' (Req PUT "/api/support/guest/manage/$id" @{state='RESOLVED';note='Thử sửa'} $customer) 403
$list=ReadJson (Req GET '/api/support/guest/manage?state=OPEN' $null $staff)
Assert 'STAFF thấy liên hệ mới' (@($list.content|Where-Object id -eq $id).Count-eq 1)
$detail=ReadJson (Req GET "/api/support/guest/manage/$id" $null $staff)
Assert 'STAFF xem được nội dung và liên lạc' ($detail.email-eq$body.email-and$detail.message-eq$body.message)
Check 'Không giải quyết nếu thiếu ghi chú' (Req PUT "/api/support/guest/manage/$id" @{state='RESOLVED';note=''} $staff) 400
$working=Req PUT "/api/support/guest/manage/$id" @{state='IN_PROGRESS';note='Đang kiểm tra'} $staff;Check 'STAFF tiếp nhận' $working 200
Assert 'Trạng thái đang xử lý được lưu' ((ReadJson $working).state-eq'IN_PROGRESS')
$resolved=Req PUT "/api/support/guest/manage/$id" @{state='RESOLVED';note='Đã liên hệ qua số điện thoại'} $staff;Check 'STAFF giải quyết' $resolved 200
Assert 'Ghi chú và người xử lý được lưu' ((ReadJson $resolved).staffNote-eq'Đã liên hệ qua số điện thoại'-and(ReadJson $resolved).actorId)
Check 'Thao tác lặp giữ nguyên kết quả' (Req PUT "/api/support/guest/manage/$id" @{state='RESOLVED';note='Đã liên hệ qua số điện thoại'} $staff) 200
Check 'Ghi chú khác khi retry bị chặn' (Req PUT "/api/support/guest/manage/$id" @{state='RESOLVED';note='Ghi chú khác'} $staff) 409
$result=@{at=[DateTime]::UtcNow.ToString('o');checks=$checks;passed=$checks.Count;contactId=$id}
$result|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $projectRoot 'docs/guest-contact-smoke-result.json') -Encoding UTF8
Write-Host "PASS $($checks.Count) kiểm tra Guest Contact trên MySQL qua Gateway."
