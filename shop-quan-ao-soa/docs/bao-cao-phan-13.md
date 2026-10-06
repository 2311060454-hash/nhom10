# Phần 13 — Liên hệ của khách chưa đăng nhập

## Vấn đề và kết quả

Trước phần này, khách phải đăng nhập để tạo ticket hỗ trợ. Trang liên hệ chỉ hiển thị thông tin cửa hàng. Nay khách vãng lai gửi họ tên, email, số điện thoại tùy chọn, chủ đề và nội dung từ `/contact`. Reporting Service lưu liên hệ trong MySQL và trả mã tiếp nhận. Nhân viên hoặc quản trị viên xem hàng chờ riêng tại `/manage/support/guest`, ghi chú cách đã liên hệ và cập nhật trạng thái.

## Ranh giới và an toàn dữ liệu

Reporting Service sở hữu `guest_contacts` trong `shop_quan_ao_reporting`; `actor_id` tham chiếu logic đến Auth, không có khóa ngoại xuyên service. Gateway chuyển `/api/support/**` đến Reporting. POST công khai không yêu cầu JWT; GET/PUT hàng chờ được Spring Security giới hạn STAFF/ADMIN. Biên nhận chỉ chứa `id` và `createdAt`; không có API đọc liên hệ công khai bằng mã. Nội dung được React hiển thị dưới dạng văn bản, không chèn HTML. `Idempotency-Key` theo email và hash nội dung, cùng khóa khác nội dung trả 409. Giao dịch khóa hàng `support_guard` để tuần tự hóa tạo liên hệ và ngăn gửi lặp đồng thời. Mỗi email tối đa 3 yêu cầu trong một giờ.

## Quy trình

`OPEN → IN_PROGRESS → RESOLVED`. Nhân viên có thể giải quyết trực tiếp từ OPEN hoặc mở lại RESOLVED về IN_PROGRESS. RESOLVED yêu cầu ghi chú xử lý. Gọi lặp cùng trạng thái/ghi chú giữ nguyên kết quả; thay ghi chú trong lần gọi lặp trả 409. Nhân viên liên hệ qua kênh ngoài hệ thống; không có email hay SMS tự động.

## Triển khai và kiểm thử

Migration: `backend/reporting-notification-service/src/main/resources/db/migration/V4__guest_contacts.sql`. API/DTO, giao diện và cách gọi nằm trong [Customer Support API](api/customer-support.md). JUnit kiểm tra gửi lặp, giới hạn tốc độ, phân quyền, biên nhận không lộ thông tin và luồng trạng thái. Vitest kiểm tra form và hàng chờ; smoke đi qua Gateway/MySQL thật. Bản cập nhật đạt 104/104 JUnit, 32/32 Vitest, Vite build và [19/19 smoke](guest-contact-smoke-result.json). Đã chạy các service trên Windows và kiểm tra Reporting/Gateway sẵn sàng. Chưa kiểm thử trực quan từng kích thước màn hình hoặc chạy runtime bằng JDK 17 trên máy này.
