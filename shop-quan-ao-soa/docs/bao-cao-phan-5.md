# Phần 5 — khuyến mãi, mã giảm giá, đánh giá và yêu thích

Ngày kiểm tra: 24/09/2026. Đây là phần triển khai tiếp sau Order và Payment/Shipping, không phải tuyên bố toàn bộ đồ án đã hoàn tất.

Promotion & Review Service chạy cổng 8086 và sở hữu database `shop_quan_ao_promotion`. Các bảng `coupons`, `coupon_usages`, `promotions`, `reviews`, `wishlists` cùng khóa guard và lịch sử Flyway nằm riêng khỏi Catalog/Order. Service dùng REST để kiểm tra sản phẩm, danh mục và điều kiện khách đã mua; không nối FK xuyên schema.

Quản trị viên tạo/sửa, bật/tắt coupon phần trăm hoặc số tiền, giới hạn đơn tối thiểu, tổng lượt, lượt theo khách, thời gian và mức giảm tối đa. Chương trình giảm theo sản phẩm hoặc danh mục chọn mức lợi nhất; Order lấy giá có hiệu lực khi tính giỏ và đóng băng đơn giá trong `order_items`. Coupon được báo giá, giữ theo `orderId`, chốt sau khi kho giữ thành công, giải phóng khi đơn hủy/thất bại; các lệnh gọi lặp không tăng lượt dùng. Thanh toán và giao hàng giữ trạng thái riêng theo phần 4.

Khách lưu sản phẩm yêu thích; chỉ tài khoản đó xem/xóa mục của mình. Đánh giá chỉ được gửi sau đơn `COMPLETED` có sản phẩm tương ứng. Đánh giá mới hoặc chỉnh sửa ở trạng thái PENDING; chỉ ADMIN duyệt/ẩn. Chỉ APPROVED xuất hiện trên trang chi tiết. Frontend có `/favorites`, `/my-reviews`, `/admin/coupons`, `/admin/promotions`, `/admin/reviews`, giá khuyến mãi và form đánh giá tại trang chi tiết, ô coupon tại checkout.

Kiểm thử `mvn clean verify`: 70 test backend qua 7 module, gồm 10 test mới của Promotion & Review và 21 test Order; `npm test`: 17 test frontend; Vite production build thành công. `scripts/Smoke-PromotionReview.ps1` đạt 34 kiểm tra HTTP qua Gateway/MySQL, gồm giới hạn coupon, giải phóng lượt khi hủy, hoàn tất COD, duyệt đánh giá, quyền STAFF/CUSTOMER và cô lập wishlist. File bằng chứng `docs/promotion-review-smoke-result.json`. Các smoke Auth, Order và Payment/Shipping cũng chạy lại sau tích hợp. Máy kiểm thử dùng JVM 25 với biên dịch `--release 17`; chưa xác minh chạy bằng runtime Java 17.

Giới hạn còn lại: Reporting/Notification Service cổng 8087 chưa xây dựng; banner, liên hệ hỗ trợ, báo cáo, yêu cầu trả/hoàn sau đơn hoàn tất và một số màn hình admin chưa triển khai. Trang lưới Catalog chưa gộp chương trình khuyến mãi theo thời gian vào bộ lọc/sắp xếp; giá chi tiết và giỏ/checkout đã tính từ API. Kiểm thử giao diện tự động dùng mock API, còn kết nối database thật được chứng minh bằng smoke HTTP, chưa có E2E browser toàn bộ các trang.
