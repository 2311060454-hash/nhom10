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
$admin=(ReadJson (Req POST '/api/auth/login' @{email='admin@shop.local';password=$env:SEED_PASSWORD})).accessToken
$staff=(ReadJson (Req POST '/api/auth/login' @{email='staff@shop.local';password=$env:SEED_PASSWORD})).accessToken
Assert 'ADMIN và STAFF đăng nhập' ($admin-and$staff)
$customers=@();foreach($n in 1..2){$email="support-$n-$suffix@example.com";$password=[Guid]::NewGuid().ToString('N');Check "Đăng ký khách $n" (Req POST '/api/auth/register' @{fullName="Khách hỗ trợ $n";email=$email;phone='0901234567';password=$password}) 201;$customers+=,(ReadJson (Req POST '/api/auth/login' @{email=$email;password=$password})).accessToken}
$first=$customers[0];$other=$customers[1]
Check 'Khách không xem danh sách quản lý' (Req GET '/api/support/manage' $null $first) 403
Check 'STAFF không tạo ticket khách' (Req POST '/api/support' @{subject='Kiểm tra quyền';message='Nội dung kiểm tra phân quyền'} $staff 'staff-support-1') 403
$key="support-$suffix";$body=@{subject='Đơn giao chậm';message='Tôi cần kiểm tra tình trạng giao hàng của đơn gần đây'}
$made=Req POST '/api/support' $body $first $key;Check 'Khách tạo ticket' $made 201;$ticket=ReadJson $made;$id=$ticket.ticket.id
$retry=Req POST '/api/support' $body $first $key;Check 'Gửi lại cùng key' $retry 201
Assert 'Không tạo ticket trùng' ((ReadJson $retry).ticket.id-eq$id)
Check 'Cùng key khác nội dung bị chặn' (Req POST '/api/support' @{subject='Đơn giao chậm';message='Nội dung yêu cầu khác để kiểm thử'} $first $key) 409
Check 'Người khác không đọc ticket' (Req GET "/api/support/$id" $null $other) 404
Assert 'Người khác không thấy ticket trong danh sách' ((ReadJson (Req GET '/api/support' $null $other)).content.Count-eq 0)
$managed=ReadJson (Req GET '/api/support/manage?state=OPEN' $null $staff)
Assert 'STAFF thấy ticket mới' (@($managed.content|Where-Object id -eq $id).Count-eq 1)
Check 'STAFF tiếp nhận' (Req PUT "/api/support/$id/state" @{state='IN_PROGRESS'} $staff) 200
$replyKey="reply-$suffix";$reply=@{message='Chúng tôi đang kiểm tra vận đơn và sẽ cập nhật sớm'}
$answered=Req POST "/api/support/$id/messages" $reply $staff $replyKey;Check 'STAFF trả lời' $answered 200
Assert 'Trạng thái chờ khách' ((ReadJson $answered).ticket.state-eq'WAITING_CUSTOMER')
Check 'Retry phản hồi cùng key' (Req POST "/api/support/$id/messages" $reply $staff $replyKey) 200
$detail=ReadJson (Req GET "/api/support/$id" $null $first)
Assert 'Phản hồi chỉ ghi một lần' ($detail.messages.Count-eq 2-and$detail.messages[1].authorRole-eq'STAFF')
Check 'Khách khác không phản hồi ticket' (Req POST "/api/support/$id/messages" @{message='Tôi không phải chủ ticket'} $other "other-$suffix") 404
Check 'Khách phản hồi bổ sung' (Req POST "/api/support/$id/messages" @{message='Cảm ơn, xin cho biết ngày giao hàng dự kiến'} $first "customer-$suffix") 200
Check 'Khách không tự đánh dấu đã xử lý' (Req PUT "/api/support/$id/state" @{state='RESOLVED'} $first) 403
Check 'STAFF đánh dấu đã xử lý' (Req PUT "/api/support/$id/state" @{state='RESOLVED'} $staff) 200
Check 'Khách đóng ticket đã xử lý' (Req PUT "/api/support/$id/state" @{state='CLOSED'} $first) 200
Check 'Đóng lại idempotent' (Req PUT "/api/support/$id/state" @{state='CLOSED'} $first) 200
Check 'Không trả lời ticket đóng' (Req POST "/api/support/$id/messages" @{message='Tôi nhắn thêm sau khi đóng ticket'} $first "closed-$suffix") 409
$detail=ReadJson (Req GET "/api/support/$id" $null $admin)
Assert 'Lịch sử và trao đổi lưu trong database' ($detail.ticket.state-eq'CLOSED'-and$detail.messages.Count-eq 3-and$detail.history.Count-eq 6)
$result=@{at=[DateTime]::UtcNow.ToString('o');checks=$checks;passed=$checks.Count;ticketId=$id}
$result|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $projectRoot 'docs/support-smoke-result.json') -Encoding UTF8
Write-Host "PASS $($checks.Count) kiểm tra Customer Support trên MySQL qua Gateway."
