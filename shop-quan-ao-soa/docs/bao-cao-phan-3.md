# Báo cáo bổ sung phần 3: Giỏ hàng và đơn COD

## Chương 1 — Mục tiêu và phạm vi

Phần này nối Catalog và Inventory thành luồng đặt đơn thực tế. Khách có giỏ bền vững theo tài khoản, nhập/chọn địa chỉ, xem tiền, gửi đơn COD, tra cứu và hủy đơn khi được phép. ADMIN/STAFF xem danh sách phân trang, lọc trạng thái, xác nhận và đóng gói. Phạm vi kết thúc tại đóng gói; giao hàng, thu tiền, hoàn tiền và đánh giá mua hàng chưa triển khai.

## Chương 2 — Cơ sở kỹ thuật áp dụng

Order chạy độc lập tại 8084 với Spring Boot 3, JPA và MySQL shop_quan_ao_order. Gateway đã có route /api/cart và /api/orders. JWT và kiểm tra phiên Auth được dùng tại cả service. BigDecimal/DECIMAL dùng cho tiền; không tin giá hoặc chủ sở hữu do frontend gửi. Saga lưu trạng thái vào database để có thể tiếp tục sau khi tiến trình khởi động lại. Đây là cơ chế nhất quán cuối cùng, không phải transaction ACID xuyên service.

## Chương 3 — Thiết kế

Hợp đồng, ERD và sơ đồ tuần tự thực tế nằm ở [API Cart/Order](api/cart-order.md). Snapshot đơn tách khỏi dữ liệu sản phẩm hiện tại. Đơn bị lỗi hoặc hủy phải qua trạng thái bù trừ. Giỏ được xóa trong transaction tạo đơn; nếu đặt thất bại, người dùng xem snapshot và chọn mua lại, chưa có nút khôi phục toàn bộ giỏ tự động. Order không đọc bảng kho hoặc tài khoản trực tiếp.

## Chương 4 — Triển khai và xác minh

Mã nguồn nằm ở backend/order-service và frontend/src/OrderPages.jsx. Migration V1__orders.sql tạo 7 bảng kể cả order_guard. Start-Local.ps1 chạy Auth → Catalog → Inventory → Order → Gateway. Frontend có /cart, /checkout, /orders, /orders/:id, /manage/orders. Tất cả thao tác trên các trang này gọi API, không có nút thanh toán online giả.

Kiểm thử tự động Order dùng Spring Boot Test, JUnit, Mockito và H2; phụ thuộc REST được mock để kiểm tra timeout, cạnh tranh key, snapshot tiền, quyền và bù trừ. Smoke-Orders.ps1 gọi Gateway thật, sử dụng Auth/Catalog/Inventory/Order với MySQL 3310. Bằng chứng và ngày chạy nằm trong orders-smoke-result.json. Test frontend dùng Vitest/Testing Library, không thay thế browser E2E. Chưa tự động hóa toàn bộ checkout bằng trình duyệt.

Build đặt compiler release 17; runtime thực tế trên máy hiện là JDK 25.0.4. Chưa xác minh bằng JVM 17 riêng. Không dùng kết quả H2 làm bằng chứng migration MySQL; migration đã được thực thi khi service thật khởi động và smoke ghi/đọc dữ liệu thành công.

## Chương 5 — Kết quả và việc còn lại

Đã triển khai đặt đơn COD, snapshot, kiểm tra kho, chống trùng theo key, phân quyền và hủy hoàn kho. Chưa có Payment & Shipping Service, coupon, hoàn trả, lưu giao dịch thanh toán, doanh thu, review hoặc notification. Chưa có đơn mẫu hoàn thành vì hệ thống chưa có luồng hoàn thành hợp lệ. Các đơn smoke là dữ liệu kiểm thử thật và được giữ cùng lịch sử; sản phẩm smoke được ngừng bán.

Khóa toàn bộ thao tác ghi Order là giới hạn tải của bản local. Cần bổ sung fault-injection mạng thực, Java 17 runtime test và điều phối giao hàng/thanh toán trước khi tuyên bố hoàn thiện toàn bộ đề tài.
