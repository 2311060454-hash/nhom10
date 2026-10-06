# Phần 6 — báo cáo và thông báo đơn hàng

Ngày kiểm tra: 24/09/2026. Đã triển khai Reporting & Notification Service cổng 8087 và schema `shop_quan_ao_reporting`. Service tổng hợp báo cáo qua REST nội bộ từ Order/Auth/Catalog/Inventory, lưu thông báo trạng thái đơn trong database của mình. Quyền ADMIN được kiểm tra bằng Spring Security tại API báo cáo; thông báo bị ràng buộc bởi `userId` của JWT.

Dashboard hiển thị doanh thu COD đã thu theo hôm nay, tuần, tháng, năm và tổng; trạng thái đơn, số sản phẩm/khách, biến thể sắp hết, sản phẩm bán chạy và đơn gần đây. Trang báo cáo lọc khoảng ngày, nhóm ngày/tháng/quý/năm và xuất CSV. Doanh thu theo ngày hoàn tất đơn, không theo ngày tạo đơn. Đơn mô phỏng local có tổng riêng, không được cộng vào doanh thu tiền thật. Số liệu không được seed hoặc hardcode trên frontend.

Thông báo được tạo idempotent từ lịch sử đơn khi chủ tài khoản gọi API. Có trang danh sách, trạng thái đã đọc/chưa đọc, phân trang và link về đơn. Việc đồng bộ lặp không tạo thông báo trùng; tài khoản khác đọc ID thông báo trả 404. Chưa có push/email hoặc thông báo khuyến mãi.

Đã chạy `mvn clean verify` toàn backend, Vitest và Vite build; chạy `scripts/Smoke-ReportingNotification.ps1` qua Gateway/MySQL: **23 kiểm tra đạt**, tạo một đơn COD thật, xử lý giao hàng và thu tiền, đối chiếu doanh thu tăng đúng 130.000 VNĐ, thử CSV và quyền đọc thông báo. Bằng chứng: `docs/reporting-notification-smoke-result.json`. Các smoke Auth (23), Catalog/Inventory (36), Order (29), Payment/Shipping (94) và Promotion/Review (34) đều chạy lại và đạt sau tích hợp. Kiểm thử frontend dùng mock API; chưa có E2E browser toàn bộ trang. Biên dịch release 17, kiểm thử chạy JVM 25; chưa kiểm thử runtime Java 17.

Chưa triển khai: báo cáo doanh thu theo danh mục, xuất Excel, yêu cầu hỗ trợ, banner/nội dung cửa hàng, hoàn tiền sau đơn hoàn tất và các phần UI khác của đồ án gốc. Bộ tổng hợp có giới hạn 10.000 đơn trong một yêu cầu để phù hợp demo local; cần thiết kế kho báo cáo bất đồng bộ khi mở rộng.
