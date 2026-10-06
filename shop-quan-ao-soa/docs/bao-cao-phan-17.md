# Phần 17 — Báo cáo đơn hủy và hoàn trả

## Mục tiêu và kết quả

Báo cáo doanh thu trước đây loại đơn hủy và xử lý tiền hoàn đúng khi tính doanh thu, nhưng chưa có màn hình phân tích riêng các sự kiện này. ADMIN nay lọc ngày hủy/hoàn tiền ở `/admin/order-exceptions`, xem số đơn hủy, trả toàn bộ, trả một phần, tiền hoàn COD và giá trị hoàn mô phỏng; có danh sách sự kiện và CSV.

## Thiết kế SOA và quy tắc tính

Order Service tiếp tục sở hữu đơn, yêu cầu trả và lịch sử trạng thái. Endpoint nội bộ báo cáo gửi các sự kiện Return cho Reporting Service qua REST; không truy cập chéo database. Reporting lấy ngày chuyển `CANCELLED` hoặc ngày Return chuyển `REFUNDED`, quy về `Asia/Ho_Chi_Minh` trước khi lọc. Trả một phần vẫn giữ đơn ở `COMPLETED` nên được nhận diện qua lịch sử Return, không dựa riêng vào trạng thái đơn. Yêu cầu đang chờ, bị từ chối hoặc chưa xác nhận hoàn tiền không được cộng. Số tiền hoàn COD được trình bày riêng; giao dịch `SIMULATED` không được mô tả là tiền thật đã chuyển. Đơn hủy ghi số tiền hoàn bằng 0 trong báo cáo này.

## Kiểm thử và giới hạn

JUnit xác nhận ba loại sự kiện, số tiền hoàn tách theo phương thức, ngày đảo ngược và quyền ADMIN. Vitest kiểm tra giao diện và bộ lọc. [Smoke qua Gateway/MySQL](order-exceptions-smoke-result.json) đạt 9/9 và kiểm tra CSV khớp số dòng báo cáo; dữ liệu thử hiện có gồm 17 đơn hủy, 10 trả toàn bộ, 2 trả một phần trong khoảng từ 2020 đến ngày chạy. Toàn backend đạt 107/107 JUnit, frontend 37/37 Vitest và Vite build. Mỗi lần tổng hợp hiện quét tối đa 10.000 đơn; vượt ngưỡng trả 503. Chưa có thống kê theo lý do hủy/trả hoặc phân tích theo danh mục sản phẩm.
