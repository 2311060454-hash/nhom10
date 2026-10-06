# Phần 7 — yêu cầu hỗ trợ khách hàng

Ngày kiểm tra: 24/09/2026. Đã bổ sung Customer Support vào Reporting & Notification Service (8087) với migration Flyway V2 trên `shop_quan_ao_reporting`. `customer_support` lưu ticket, `support_messages` lưu trao đổi, `support_status_history` lưu mọi chuyển trạng thái. FK chỉ nằm trong schema này; ID khách hàng là tham chiếu logic sang Auth qua JWT.

CUSTOMER tạo và xem ticket của mình tại `/support`, gửi thêm thông tin tại `/support/:id` và đóng yêu cầu sau khi được giải quyết. STAFF/ADMIN xem danh sách lọc theo trạng thái/chủ đề tại `/manage/support`, tiếp nhận, phản hồi, giải quyết và đóng. Nội dung được React hiển thị như text. Backend trả 404 nếu một khách cố xem/trả lời ticket của khách khác; phân quyền không chỉ nằm ở nút giao diện.

Tạo ticket và phản hồi dùng `Idempotency-Key` với hash nội dung. Cùng khóa/nội dung trả kết quả cũ; cùng khóa/nội dung khác trả 409. Khóa guard trong transaction tuần tự hóa ghi để hai yêu cầu đồng thời không tạo trùng. `CLOSED` không cho trả lời hay chuyển lại trạng thái. Mỗi tin nhắn ghi vai trò tác giả và thời điểm, mỗi chuyển trạng thái ghi người thực hiện.

Kiểm thử: 5 bài JUnit mới cho Support, gồm hai request đồng thời cùng khóa; Vitest có 2 bài mới cho form tạo ticket và thao tác STAFF. `scripts/Smoke-Support.ps1` đạt **25 kiểm tra** qua Gateway/MySQL; `scripts/Smoke-ReportingNotification.ps1` đạt lại **23 kiểm tra** sau migration V2. Backend toàn dự án `mvn test` **80/80** sau khi bổ sung bài concurrency; `mvn clean verify` đóng gói thành công trước khi thêm riêng bài kiểm thử này. Bằng chứng smoke ở `docs/support-smoke-result.json`.

Giới hạn còn lại: chưa có đính kèm tệp, SLA, phân công nhân viên cụ thể, email/push khi có trả lời, liên kết ticket với order, banner/nội dung cửa hàng và trả hàng sau đơn hoàn tất. Hỗ trợ hiện yêu cầu khách đăng nhập; chưa có form liên hệ công khai cho khách vãng lai.
