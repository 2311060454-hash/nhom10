# API Promotion & Review (cổng 8086)

Frontend gọi qua Gateway `http://localhost:8080`; service sở hữu duy nhất schema `shop_quan_ao_promotion`. JWT dùng chung secret và phiên được Auth xác minh. `/internal/**` chỉ dành cho các service mang `X-Internal-Key`, Gateway không chuyển tiếp đường dẫn nội bộ.

| Phương thức | URL | Đầu vào → đầu ra | Quyền |
|---|---|---|---|
| GET | `/api/coupons/quote?code=&subtotal=` | Mã và tiền hàng → `code, discount, remainingTotal` | Đã đăng nhập |
| GET/POST | `/api/coupons/manage` | Trang mã / `CouponInput` → trang / `CouponView` | ADMIN |
| PUT | `/api/coupons/manage/{id}` | `CouponInput` → `CouponView` | ADMIN |
| GET | `/api/promotions/active?productId=&categoryId=` | IDs → chương trình còn hiệu lực | Công khai |
| GET | `/api/promotions/price?productId=&categoryId=&price=` | Giá gốc/giá bán → `price, originalPrice, promotionId` | Công khai; chỉ tham khảo |
| GET/POST | `/api/promotions/manage` | Trang / `PromotionInput` → trang / `PromotionView` | ADMIN |
| PUT | `/api/promotions/manage/{id}` | `PromotionInput` → `PromotionView` | ADMIN |
| GET | `/api/reviews/products/{id}?page=` | ID sản phẩm → trang đánh giá APPROVED | Công khai |
| GET | `/api/reviews/mine?page=` | Trang đánh giá của mình | Đã đăng nhập |
| POST | `/api/reviews` | `productId, stars, comment` → đánh giá PENDING | CUSTOMER đã có đơn COMPLETED chứa sản phẩm |
| DELETE | `/api/reviews/{id}` | Ẩn đánh giá của chính mình → 204 | Chủ đánh giá |
| GET | `/api/reviews/manage?productId=&stars=&state=&page=` | Bộ lọc → trang đánh giá | ADMIN |
| PUT | `/api/reviews/manage/{id}` | `{state: APPROVED|HIDDEN}` → đánh giá | ADMIN |
| GET | `/api/wishlists` | Danh sách yêu thích của tài khoản | Đã đăng nhập |
| PUT/DELETE | `/api/wishlists/{productId}` | Thêm/xóa idempotent → mục/204 | Đã đăng nhập |

Nội bộ Order gọi `GET /internal/coupons/quote`, `POST /internal/coupons/reservations/{orderId}`, `POST .../commit`, `POST .../release` và `POST /internal/promotions/price`. Một dòng guard được khóa trong transaction khi giữ coupon để không vượt tổng lượt hoặc lượt mỗi khách. `orderId` là khóa idempotency cho lượt giữ. Hủy hoặc đặt hàng thất bại giải phóng đúng lượt; worker cũng hết hạn lượt giữ sau 15 phút. Order gọi giá khuyến mãi từ service này, lưu giá từng dòng và discount vào snapshot trước khi giữ kho. Giá được kiểm lại ở backend, không tin giá gửi từ frontend.

Notification Service gọi `GET /internal/promotions/marketing?since={ISO-8601}&page=0&size=100` để lấy các chương trình đang hoạt động, đã bắt đầu, chưa hết hạn và được tạo/cập nhật/bắt đầu kể từ lúc khách đồng ý. Kết quả `PageResult<PromotionView>` có thời gian tạo/sửa để xác định mốc thông báo; tối đa 10.000 chương trình cho một lần đồng bộ. Endpoint chỉ dành cho service, không đưa qua Gateway.

Review gọi REST Order nội bộ để kiểm tra `COMPLETED`, không đọc bảng đơn hàng. Nội dung do khách nhập chỉ hiển thị như text trong React, không chèn HTML. Giá từ endpoint công khai có thể truyền ID/giá bất kỳ để xem trước; chỉ giá Order nhận từ Catalog rồi tính lại qua REST Promotion mới được sử dụng để thanh toán.

Schema/migration: `backend/promotion-review-service/src/main/resources/db/migration/V1__promotion_review.sql`; Order thêm cột snapshot bằng `backend/order-service/src/main/resources/db/migration/V3__coupon_snapshot.sql`. Không có FK xuyên database. OpenAPI: `http://localhost:8086/swagger-ui/index.html`.
