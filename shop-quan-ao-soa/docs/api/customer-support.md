# Customer Support API

Customer Support thuộc Reporting & Notification Service (`8087`), dữ liệu nằm trong schema `shop_quan_ao_reporting`. Frontend gọi qua Gateway `8080`. Ticket theo dõi yêu cầu JWT: khách chỉ đọc, trả lời và đóng ticket của chính mình; STAFF/ADMIN xem danh sách và xử lý tất cả ticket. Backend trả 404 cho ticket của người khác. Form liên hệ vãng lai dùng API riêng, không cần JWT; chỉ nhân viên và quản trị viên xem nội dung.

| HTTP | URL | Đầu vào → đầu ra | Quyền |
|---|---|---|---|
| POST | `/api/support` | `Idempotency-Key`, `{subject,message}` → `Detail`, HTTP 201 | CUSTOMER |
| GET | `/api/support?page=` | Trang ticket của mình → `PageResult<TicketView>` | Đã đăng nhập |
| GET | `/api/support/manage?state=&q=&page=` | Lọc trạng thái/chủ đề → trang ticket | STAFF, ADMIN |
| GET | `/api/support/{id}` | Ticket, tin nhắn, lịch sử trạng thái → `Detail` | Chủ ticket, STAFF, ADMIN |
| POST | `/api/support/{id}/messages` | `Idempotency-Key`, `{message}` → `Detail` | Chủ ticket, STAFF, ADMIN |
| PUT | `/api/support/{id}/state` | `{state: IN_PROGRESS|RESOLVED|CLOSED}` → `Detail` | STAFF/ADMIN; chủ ticket chỉ được đóng sau RESOLVED |

Ticket đi qua `OPEN → IN_PROGRESS → WAITING_CUSTOMER → IN_PROGRESS → RESOLVED → CLOSED`. Nhân viên phản hồi sẽ đưa về `WAITING_CUSTOMER`; khách phản hồi đưa về `IN_PROGRESS`, kể cả mở lại ticket `RESOLVED`. Nhân viên có thể tiếp nhận lại ticket đã giải quyết. Ticket `CLOSED` không nhận thêm tin nhắn. Mọi chuyển trạng thái ghi vào `support_status_history`; tin nhắn có vai trò tác giả và thời gian riêng.

`Idempotency-Key` dài 8–100 ký tự chữ/số/gạch. Gọi lại cùng khóa và nội dung trả đúng ticket hoặc tin nhắn cũ, khác nội dung trả 409. Một hàng guard được khóa trong transaction khi tạo/trả lời để ngăn ghi trùng đồng thời. Nội dung nhập được giới hạn và kiểm tra bằng Bean Validation, React hiển thị như text, không chèn HTML. Mật khẩu và thông tin cá nhân không nằm trong ticket mặc định.

Migration: `backend/reporting-notification-service/src/main/resources/db/migration/V2__customer_support.sql`. Các FK chỉ nối ticket, tin nhắn và lịch sử trong cùng schema; `user_id` là tham chiếu logic sang Auth. Smoke: `scripts/Smoke-Support.ps1`, kết quả `docs/support-smoke-result.json`.

## Liên hệ không cần tài khoản

| HTTP | URL | Đầu vào → đầu ra | Quyền |
|---|---|---|---|
| POST | `/api/support/guest` | `Idempotency-Key`, `{fullName,email,phone?,subject,message}` → `{id,createdAt}`, HTTP 201 | Công khai |
| GET | `/api/support/guest/manage?state=&q=&page=` | Trang 20 liên hệ, lọc `OPEN/IN_PROGRESS/RESOLVED`, tìm theo chủ đề/email | STAFF, ADMIN |
| GET | `/api/support/guest/manage/{id}` | Nội dung, thông tin liên lạc, trạng thái, ghi chú | STAFF, ADMIN |
| PUT | `/api/support/guest/manage/{id}` | `{state: IN_PROGRESS|RESOLVED,note}` → liên hệ đã cập nhật | STAFF, ADMIN |

Form ở `/contact`; hàng chờ ở `/manage/support/guest`. Biên nhận công khai chỉ có mã và thời gian, không có API tra cứu công khai bằng mã để tránh lộ thông tin cá nhân. Nhân viên liên hệ ngoài hệ thống rồi ghi chú nội bộ; ứng dụng không gửi email hay SMS tự động. Cần ghi chú để đánh dấu `RESOLVED`. Có thể mở lại thành `IN_PROGRESS`; gọi lặp đúng trạng thái và ghi chú trả cùng kết quả, ghi chú khác trả 409. Khóa gửi dài 8–100 ký tự; cùng email + khóa + nội dung trả cùng mã, khác nội dung trả 409. Mỗi email tối đa 3 liên hệ trong một giờ. Giới hạn theo email là biện pháp chống lạm dụng cơ bản, chưa có CAPTCHA hoặc chặn theo IP. Flyway `V4__guest_contacts.sql` tạo bảng trong schema Reporting; `actor_id` là tham chiếu logic sang Auth. Chạy `scripts/Smoke-GuestContact.ps1` để kiểm tra Gateway/MySQL.
