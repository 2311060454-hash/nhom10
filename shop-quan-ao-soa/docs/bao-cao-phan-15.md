# Phần 15 — Tra cứu đơn hàng cho nhân viên

## Vấn đề và kết quả

Danh sách quản lý đơn trước đây chỉ lọc trạng thái. Phần này bổ sung tìm theo mã đơn UUID, tên hoặc số điện thoại người nhận, cùng khoảng ngày đặt. Các bộ lọc thực hiện tại Order Service trên MySQL trước khi phân trang. STAFF/ADMIN dùng form ở `/manage/orders`; API CUSTOMER vẫn chỉ trả đơn thuộc tài khoản của họ.

## Thiết kế

`GET /api/orders/manage` nhận `q`, `from`, `to`, `state`, `page`. `q` tối đa 120 ký tự. Ngày nhập `yyyy-MM-dd` chuyển từ `Asia/Ho_Chi_Minh` thành khoảng Instant nửa mở để bao gồm trọn ngày kết thúc. Ngày bắt đầu sau ngày kết thúc trả 400. Danh sách sắp theo `createdAt DESC, id DESC` để phân trang ổn định khi nhiều đơn cùng thời điểm. Tên và điện thoại là snapshot người nhận trong Order Service, không truy cập database Auth. Backend áp dụng quyền STAFF/ADMIN; frontend chỉ cung cấp thao tác thuận tiện.

## Kiểm thử

JUnit xác nhận tìm theo ba trường, khoảng ngày theo múi giờ địa phương, phạm vi đơn của khách và ngày đảo ngược. Vitest xác nhận form gửi bộ lọc đúng. [Smoke qua Gateway/MySQL](order-search-smoke-result.json) đạt 7/7, gồm STAFF thấy đơn và CUSTOMER bị 403. Toàn backend đạt 105/105 JUnit; frontend 35/35 Vitest và Vite build. Chưa có tìm theo tên tài khoản mua trong Auth Service hoặc tối ưu tìm kiếm toàn văn cho dữ liệu rất lớn.
