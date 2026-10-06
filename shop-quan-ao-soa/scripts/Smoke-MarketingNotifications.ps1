param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$checks=[Collections.Generic.List[object]]::new()
function Req($method,$path,$body=$null,$token=''){
 $headers=@{};if($token){$headers.Authorization="Bearer $token"}
 $args=@{Uri="$BaseUrl$path";Method=$method;Headers=$headers;ContentType='application/json; charset=utf-8';SkipHttpErrorCheck=$true;TimeoutSec=20}
 if($null-ne$body){$args.Body=[Text.Encoding]::UTF8.GetBytes(($body|ConvertTo-Json -Depth 10))}
 Invoke-WebRequest @args
}
function Json($response){$response.Content|ConvertFrom-Json}
function Check($name,$response,$status){if([int]$response.StatusCode-ne$status){throw "$name HTTP $($response.StatusCode): $($response.Content)"};$checks.Add(@{name=$name;status='PASS'})}
function Assert($name,$condition){if(-not$condition){throw $name};$checks.Add(@{name=$name;status='PASS'})}
function Register($suffix){$password=[Guid]::NewGuid().ToString('N');$email="marketing-$suffix@example.com";Check "Đăng ký $suffix" (Req POST '/api/auth/register' @{fullName="Khách $suffix";email=$email;phone='0901234567';password=$password}) 201;return (Json (Req POST '/api/auth/login' @{email=$email;password=$password})).accessToken}
function Notices($token){return Json (Req GET '/api/notifications' $null $token)}
$suffix=[Guid]::NewGuid().ToString('N').Substring(0,10)
$admin=(Json (Req POST '/api/auth/login' @{email='admin@shop.local';password=$env:SEED_PASSWORD})).accessToken
$optIn=Register "$suffix-on";$optOut=Register "$suffix-off"
$products=Json (Req GET '/api/catalog/products?limit=1');if(-not$products.content.Count){throw 'Cần một sản phẩm mẫu đang bán'}
$productId=$products.content[0].id;$categoryId=$products.content[0].categoryId
Assert 'Mặc định không đồng ý' (-not(Json (Req GET '/api/auth/me' $null $optIn)).marketingConsent)
Check 'Bật đồng ý nhận khuyến mãi' (Req PUT '/api/auth/me' @{fullName='Khách nhận ưu đãi';phone='0901234567';marketingConsent=$true} $optIn) 200
Assert 'Auth lưu lựa chọn bật' ((Json (Req GET '/api/auth/me' $null $optIn)).marketingConsent)
$created=[Collections.Generic.List[object]]::new()
function Campaign($name,$targetType,$targetId){
 $body=@{name=$name;targetType=$targetType;targetId=$targetId;type='PERCENT';value=10;startsAt=(Get-Date).ToUniversalTime().AddSeconds(-30).ToString('o');endsAt=(Get-Date).ToUniversalTime().AddHours(1).ToString('o');active=$true}
 $result=Req POST '/api/promotions/manage' $body $admin;Check "Tạo $name" $result 200
 $campaign=Json $result;$created.Add(@{id=$campaign.id;body=$body});return $campaign.id
}
try{
 $productCampaign=Campaign "Ưu đãi sản phẩm $suffix" 'PRODUCT' $productId
 $first=Notices $optIn
 Assert 'Người đồng ý thấy ưu đãi sản phẩm' (@($first.content|Where-Object { $_.state-eq'PROMOTION'-and$_.linkPath-eq"/products/$productId" }).Count-eq 1)
 Assert 'Người chưa đồng ý không nhận ưu đãi' (@((Notices $optOut).content|Where-Object state -eq 'PROMOTION').Count-eq 0)
 Assert 'Đồng bộ lặp không tạo thông báo mới' (@((Notices $optIn).content|Where-Object state -eq 'PROMOTION').Count-eq 1)
 $categoryCampaign=Campaign "Ưu đãi danh mục $suffix" 'CATEGORY' $categoryId
 Assert 'Ưu đãi danh mục dẫn tới bộ lọc' (@((Notices $optIn).content|Where-Object { $_.state-eq'PROMOTION'-and$_.linkPath-eq"/?categoryId=$categoryId" }).Count-eq 1)
 Check 'Tắt đồng ý nhận khuyến mãi' (Req PUT '/api/auth/me' @{fullName='Khách nhận ưu đãi';phone='0901234567';marketingConsent=$false} $optIn) 200
 Assert 'Auth lưu lựa chọn tắt' (-not(Json (Req GET '/api/auth/me' $null $optIn)).marketingConsent)
 $laterCampaign=Campaign "Ưu đãi sau khi tắt $suffix" 'PRODUCT' $productId
 Assert 'Không nhận chương trình mới sau khi tắt' (@((Notices $optIn).content|Where-Object state -eq 'PROMOTION').Count-eq 2)
 Assert 'Khách mặc định tắt vẫn không nhận' (@((Notices $optOut).content|Where-Object state -eq 'PROMOTION').Count-eq 0)
} finally {
 foreach($campaign in $created){$body=$campaign.body;$body.active=$false;$null=Req PUT "/api/promotions/manage/$($campaign.id)" $body $admin}
}
@{timestamp=(Get-Date).ToUniversalTime().ToString('o');database='MySQL localhost:3310';tests=@($checks.ToArray());note='Ba chương trình thử nghiệm đã tắt; tài khoản thử nghiệm vẫn tồn tại để đối chiếu.'}|ConvertTo-Json -Depth 8|Set-Content (Join-Path $projectRoot 'docs/marketing-notifications-smoke-result.json') -Encoding UTF8
Write-Host "$($checks.Count) kiểm tra thông báo khuyến mãi qua MySQL thành công."
