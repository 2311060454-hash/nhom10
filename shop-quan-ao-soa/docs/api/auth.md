# API đã triển khai: Auth & User, Gateway

Base URL frontend: `http://localhost:8080`. Service Auth: `http://127.0.0.1:8081`. JSON UTF-8. Swagger UI tại `http://localhost:8081/swagger-ui/index.html`; OpenAPI JSON tại `/v3/api-docs`.

Bearer JWT: `Authorization: Bearer <accessToken>`. Token 30 phút, phiên được kiểm tra tại DB. Đăng xuất, thay đổi quyền, khóa tài khoản và đổi mật khẩu thu hồi phiên. Nội dung response không có BCrypt hash.

| Method | Path | Input | Output | Quyền |
|---|---|---|---|---|
| POST | /api/auth/register | fullName, email, phone, password | UserView; 201 | Công khai, luôn CUSTOMER |
| POST | /api/auth/login | email, password | accessToken, tokenType, expiresIn, user | Công khai |
| POST | /api/auth/logout | Không | message | Đã đăng nhập |
| GET | /api/auth/me | Không | UserView | Chính mình |
| PUT | /api/auth/me | fullName, phone, marketingConsent | UserView | Chính mình |
| POST | /api/auth/change-password | currentPassword, newPassword | message | Chính mình |
| POST | /api/auth/forgot-password | email | message chung, không trả token | Công khai |
| POST | /api/auth/reset-password | token, password | message | Token reset hợp lệ |
| GET | /api/addresses | Không | AddressView[] | Chính mình |
| POST | /api/addresses | recipient, phone, detail, defaultAddress | AddressView; 201 | Chính mình |
| PUT | /api/addresses/{id} | Như POST | AddressView | Chủ địa chỉ |
| DELETE | /api/addresses/{id} | Không | 204 | Chủ địa chỉ |
| GET | /api/admin/users | q, role, page=0, size=20 (1..100) | PageResult<UserView> | ADMIN |
| POST | /api/admin/users/staff | fullName, email, phone, password, inventoryWrite | UserView; 201 | ADMIN |
| PUT | /api/admin/users/{id}/access | role, active, inventoryWrite | UserView | ADMIN; không sửa ADMIN hiện có |
| GET | /api/admin/audit | page=0 | PageResult<AuditView> | ADMIN |
| GET | /internal/sessions/{id}?userId={id} | X-Internal-Key | valid | Chỉ service, cổng 8081; không qua Gateway |
| GET | /internal/users/{id}/marketing-preference | X-Internal-Key | `enabled, since` | Chỉ service, cổng 8081; không qua Gateway |

`UserView`: id, fullName, email, phone, roles[], active, inventoryWrite, marketingConsent, createdAt.

`marketingConsent` mặc định `false`. Lần bật đầu tiên lưu `marketing_consent_at`; lưu hồ sơ khi vẫn bật không đổi thời điểm. Tắt đặt thời điểm về `NULL`; bật lại tạo thời điểm mới. Endpoint nội bộ chỉ trả `enabled=true` cho CUSTOMER còn hoạt động và đã đồng ý. Migration Auth V2 cập nhật tài khoản đã đồng ý trước đó tại thời điểm nâng cấp, không gửi lại ưu đãi cũ.

`AddressView`: id, recipient, phone, detail, defaultAddress. Không trả userId để giao diện dùng làm định danh chủ sở hữu.

`PageResult`: content[], totalElements, totalPages, number, size, last. `AuditView`: id, actorId, action, targetId, createdAt.

Quy tắc: mật khẩu 10–64 ký tự và tối đa 72 byte UTF-8 (giới hạn BCrypt), tên tối đa 120, email tối đa 190, điện thoại 9–20 ký tự hợp lệ, địa chỉ tối đa 500; mỗi người tối đa 20 địa chỉ. Email được trim/lowercase và có UNIQUE ở database. Trường không được định nghĩa trong DTO bị từ chối, vì vậy gửi `role: ADMIN` vào đăng ký sẽ trả 400.

Lỗi: `{ "code": 403, "message": "Bạn không có quyền thực hiện thao tác này", "timestamp": "2026-09-23T00:00:00Z" }`. 400 dữ liệu sai, 401 chưa/không còn đăng nhập, 403 thiếu quyền, 404 tài nguyên không tồn tại hoặc không thuộc chủ sở hữu, 409 dữ liệu trùng/xung đột, 429 quá nhiều yêu cầu, 503 dịch vụ phụ thuộc không phản hồi. Không gửi stack trace hoặc lỗi SQL ra client.

Gateway chuyển tiếp các route đã khai báo trong `GatewayController`; những route module chưa triển khai hiện trả 503. Chỉ giao diện Auth được công bố trong phần này. Không gọi trực tiếp endpoint nội bộ từ frontend. Gateway bỏ mọi header không nằm trong allowlist; không chuyển tiếp `X-Internal-Key` từ người dùng.

Giới hạn hiện tại: gateway tùy biến dựa trên Spring Web và Java HttpClient, chưa dùng service discovery; địa chỉ các service local cố định. Không retry ghi ở Gateway. Auth introspection timeout 5 giây, proxy timeout 8 giây. Retry/saga nghiệp vụ thuộc module Order, chưa triển khai.
