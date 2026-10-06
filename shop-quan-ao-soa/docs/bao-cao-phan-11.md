# Phần 11 — thông báo tiến trình trả hàng

## Vấn đề và phạm vi

Trước phần này, khách chỉ thấy tiến trình trả hàng ở chi tiết đơn. Notification Service chỉ nhập các mốc trạng thái đơn, nên một đơn trả một phần vẫn ở `COMPLETED` và không có thông báo riêng khi được duyệt hay hoàn tiền. Phần này bổ sung thông báo trong ứng dụng cho tiến trình trả hàng toàn bộ và một phần; chưa gửi email hoặc push.

## Thiết kế giao tiếp

Order Service tiếp tục sở hữu `return_status_history`. Endpoint nội bộ `GET /internal/orders/report` trả thêm `returnEvents` khi truy vấn theo `userId`; mỗi phần tử có return ID, event ID, phạm vi FULL/PARTIAL, trạng thái, số tiền yêu cầu hoàn và thời gian. Endpoint báo cáo tổng hợp không cần lịch sử này. Reporting & Notification Service chỉ đọc qua REST, kiểm tra `userId` của bản ghi và không truy cập database Order.

Khi khách mở `/notifications` hoặc gọi `/api/notifications/unread-count`, Notification Service nhập các mốc `REQUESTED`, `APPROVED`, `REJECTED`, `RECEIVED`, `REFUND_PENDING` và `REFUNDED` của yêu cầu một phần. Mã sự kiện `return:{returnId}:{eventId}` cùng unique constraint `notifications.event_key` ngăn thông báo lặp. Với trả toàn bộ, mốc `REFUNDED` ở yêu cầu không tạo thêm thông báo vì trạng thái đơn `RETURNED` đã có thông báo cuối. Khách có thể mở chi tiết đơn và đánh dấu đã đọc bằng API đang có.

Thông báo được đồng bộ khi người dùng truy cập API; nếu Order Service không phản hồi, API trả lỗi 503 và giữ nguyên thông báo đã lưu, không báo đã đồng bộ thành công. Nội dung không chứa địa chỉ hoặc dữ liệu thanh toán nhạy cảm. Các trạng thái hoàn tiền mô phỏng vẫn chỉ phản ánh giao dịch local, không phải chứng từ chuyển khoản.

## Kiểm thử

JUnit kiểm tra dữ liệu sự kiện nội bộ, nhập thông báo idempotent, quyền chủ sở hữu và việc tránh hai thông báo cuối khi trả toàn bộ. `scripts/Smoke-PartialReturns.ps1` kiểm tra `RETURN_APPROVED` và `RETURN_REFUNDED` qua Gateway/MySQL, bên cạnh các bước đặt hàng, hoàn kho và tính doanh thu.
