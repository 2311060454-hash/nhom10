# Phần 16 — Báo cáo tồn kho CSV

## Mục tiêu và kết quả

Trang kho trước đây xem tồn theo từng trang nhưng chưa xuất được báo cáo. STAFF/ADMIN nay tải CSV theo ID biến thể hoặc tình trạng sắp hết hàng. File chứa toàn bộ dòng phù hợp, gồm tồn thực tế, số đang giữ, khả dụng, mức tối thiểu và cờ cảnh báo.

## Thiết kế

Inventory Service đọc bảng `inventory` thuộc schema của mình, không truy cập database của service khác. `GET /api/inventory/export.csv` được Spring Security giới hạn STAFF/ADMIN. Truy vấn sắp theo ID và kiểm tra `totalElements`; vượt 10.000 dòng trả 413 thay vì âm thầm cắt bớt. CSV chỉ chứa số và nhãn cố định, không đưa dữ liệu do người dùng nhập vào ô bảng tính. BOM UTF-8 hỗ trợ mở tiếng Việt trên Windows. UI gọi API qua Gateway và tạo file tải xuống, dùng bộ lọc đã áp dụng.

## Kiểm thử và giới hạn

JUnit xác nhận số tồn thực, lọc sắp hết hàng và quyền 401/403. Vitest xác nhận yêu cầu xuất dùng đúng bộ lọc. [Smoke qua Gateway/MySQL](inventory-export-smoke-result.json) đạt 7/7, gồm số dòng CSV bằng tổng biến thể từ API và giới hạn quyền. Toàn backend đạt 106/106 JUnit, frontend 36/36 Vitest và Vite production build. CSV chỉ có mã biến thể vì tên sản phẩm thuộc Catalog Service; người dùng có thể tra mã biến thể ở trang sản phẩm/kho. Chưa có báo cáo nhập xuất theo khoảng ngày dạng file.
