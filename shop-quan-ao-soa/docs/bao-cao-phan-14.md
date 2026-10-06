# Phần 14 — Tổng quan công việc nhân viên

## Mục tiêu và thiết kế

Trước phần này, STAFF có các trang xử lý đơn, trả hàng, hỗ trợ và kho nhưng chưa có trang tổng quan riêng. Trang `/staff` tập hợp bảy hàng chờ: đơn chờ xác nhận, đang đóng gói, đang giao, yêu cầu trả hàng chờ duyệt, ticket hỗ trợ mới, liên hệ vãng lai mới và biến thể sắp hết hàng. Mỗi ô dẫn tới trang nghiệp vụ hiện có.

Trang gọi các API REST qua Gateway bằng JWT của STAFF. Số liệu là `totalElements` từ truy vấn phân trang tại service sở hữu dữ liệu, không cộng các dòng chỉ có trong trang đầu và không dùng giá trị giả. Không tạo database hoặc endpoint mới. API Order sở hữu ba số đơn và yêu cầu trả; Reporting sở hữu hai số hỗ trợ; Inventory sở hữu số biến thể sắp hết hàng. Nếu một service lỗi, ô đó báo lỗi riêng và các ô còn lại vẫn hiển thị. Nút làm mới truy vấn lại toàn bộ. Quyền ghi kho lấy từ hồ sơ đăng nhập; các thao tác kho vẫn được backend kiểm tra quyền.

## Kiểm thử và giới hạn

Hai Vitest kiểm tra gọi đủ bảy nguồn, số liệu/liên kết, làm mới, quyền kho và lỗi từng service. Frontend đạt 34/34 Vitest và Vite production build. [Smoke qua Gateway/MySQL](staff-dashboard-smoke-result.json) đọc thành công cả bảy nguồn với tài khoản STAFF. Phần này không sửa backend; lần `mvn clean verify` gần nhất ở phần 13 đạt 104/104 JUnit. Chưa đo hiệu năng truy vấn dưới tải lớn; dashboard chỉ hiển thị số liệu tại thời điểm tải hoặc nhấn làm mới, chưa cập nhật thời gian thực.
