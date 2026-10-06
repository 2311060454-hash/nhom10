> Cập nhật phần 6: cả 8 service đã có mã nguồn và chạy local; Reporting/Notification lưu thông báo, tổng hợp báo cáo qua REST. Các ghi chú “chưa triển khai” của phần 2 bên dưới là lịch sử.

# Thiết kế hệ thống shop quần áo SOA

Đây là thiết kế đích. Trạng thái triển khai và kiểm thử được ghi riêng trong `docs/verification.md`; sơ đồ không phải bằng chứng chức năng đã chạy.

Cập nhật phần 2: Catalog và Inventory đã triển khai. Inventory local hiện khóa một dòng guard cho toàn bộ miền kho trước thao tác ghi, thay vì khóa từng variant như thiết kế tối ưu đích; cách này an toàn và đơn giản nhưng hạn chế thông lượng. Chi tiết hợp đồng thực tế ở `docs/api/catalog-inventory.md`. Order/saga vẫn thuộc thiết kế, chưa hiện thực.

## Ranh giới và cổng cố định

| Module | Cổng | Database | Quyền sở hữu |
|---|---:|---|---|
| api-gateway | 8080 | Không | Xác thực đầu vào, CORS, định tuyến, timeout |
| auth-user-service | 8081 | shop_quan_ao_auth | users, roles, user_roles, addresses, password_reset_tokens, auth_sessions, audit_logs |
| catalog-service | 8082 | shop_quan_ao_catalog | categories, brands, products, product_images, product_variants, banners, store_settings |
| inventory-service | 8083 | shop_quan_ao_inventory | inventory, inventory_transactions, reservations, reservation_items |
| order-service | 8084 | shop_quan_ao_order | carts, cart_items, orders, order_items, order_status_history, return_requests, saga_tasks |
| payment-shipping-service | 8085 | shop_quan_ao_payment | payments, shipments, refunds |
| promotion-review-service | 8086 | shop_quan_ao_promotion | coupons, coupon_usages, promotions, reviews, wishlists |
| reporting-notification-service | 8087 | shop_quan_ao_reporting | notifications (customer_support và reporting_events còn thuộc thiết kế) |

Frontend React/Vite chạy 5173. Mọi database dùng MySQL 8 trên localhost:3310, utf8mb4. ID liên dịch vụ là tham chiếu logic, không tạo khóa ngoại xuyên database. Trong một database sử dụng FK, unique, index và check constraint. Tiền dùng DECIMAL(15,2), Java BigDecimal. Thời gian lưu UTC; giao diện hiển thị Asia/Ho_Chi_Minh.

## Vì sao SOA

Xác thực, danh mục, tồn kho và đơn hàng có quy tắc và vòng đời khác nhau. Mỗi dịch vụ triển khai và sở hữu dữ liệu độc lập; chỉ gọi REST để dùng khả năng của dịch vụ khác. Đổi lại phải xử lý timeout, giao dịch phân tán, tính nhất quán cuối cùng và truy vết. Gateway không chứa nghiệp vụ tính tiền. Thư viện common chỉ chứa bảo mật, giao tiếp và hợp đồng lỗi, không chứa repository liên miền.

## Chức năng theo vai trò

| Vai trò | Chức năng dự kiến | Hạn chế tại backend |
|---|---|---|
| CUSTOMER | Hồ sơ, địa chỉ, tìm/lọc sản phẩm, giỏ hàng, đặt/hủy đơn, sandbox/COD, yêu thích, đánh giá sau mua, hỗ trợ, thông báo | Chỉ truy cập tài nguyên của mình; đăng ký luôn CUSTOMER |
| STAFF | Xử lý đơn, giao hàng, tra cứu sản phẩm/khách cần thiết, nhập xuất kho nếu có quyền INVENTORY_WRITE | Không quản lý quyền, nhân viên, cấu hình hoặc khuyến mãi |
| ADMIN | Tất cả nghiệp vụ quản lý, phân quyền, nội dung, báo cáo | Không bỏ qua bất biến tồn kho/trạng thái; ghi audit |

## Bảo mật và giao tiếp

JWT thời hạn ngắn có subject user ID, role và session ID. Dịch vụ kiểm tra chữ ký; gateway kiểm tra session qua Auth để khóa tài khoản, đổi quyền và đăng xuất có hiệu lực ngay. Các cổng service bind loopback khi chạy local. API nội bộ yêu cầu khóa riêng và không được gateway công khai. Không tin role/user ID từ request body hoặc header do người dùng tự đặt. DTO trả hồ sơ không chứa hash mật khẩu hoặc token reset.

API REST trả lỗi thống nhất `{code,message,timestamp}` và mã 400/401/403/404/409/503. Request có giới hạn kích thước. Timeout giao tiếp 2 giây kết nối, 5 giây phản hồi. Chỉ retry thao tác đọc hoặc thao tác đã có idempotency; tối đa 3 lần có backoff. Không tự retry POST thanh toán không có khóa.

## Bất biến đặt hàng

1. Khách gửi Idempotency-Key; UNIQUE(user_id,idempotency_key). Cùng khóa khác payload trả 409.
2. Order đọc giỏ của đúng chủ sở hữu, lấy giá/biến thể đang bán từ Catalog; không tin giá frontend. Lưu snapshot tên, SKU, size, màu, giá.
3. Inventory khóa các dòng theo variant ID tăng dần, kiểm tra `on_hand-reserved >= quantity`, tạo reservation UNIQUE(order_id), tăng reserved trong một transaction.
4. Coupon được giữ trong transaction có khóa, kiểm tra thời gian, hạn mức tổng/theo khách. Retry cùng order không dùng mã thêm lần nữa.
5. Order lưu trạng thái và tác vụ saga bền vững trước các side effect. Thanh toán sandbox ghi rõ SIMULATED, không coi là giao dịch ngân hàng thật.
6. COD xác nhận hoặc sandbox thành công: commit reservation đúng một lần; thất bại/hủy: release đúng một lần. Nếu timeout chưa biết kết quả, truy vấn trạng thái theo order ID trước khi bù.
7. Worker retry tác vụ chưa xong và reservation hết hạn; khóa reservation bảo đảm commit/release không cùng thành công. Không giữ transaction SQL mở trong lúc chờ REST.
8. Hoàn hàng chỉ cộng kho khi đã nhận hàng thực tế; unique return ID ngăn cộng hai lần. Hoàn tiền được ghi tách khỏi trạng thái vận chuyển.

Order: PENDING_PAYMENT → PENDING → CONFIRMED → PACKING → SHIPPING → COMPLETED. PENDING_PAYMENT/PENDING/CONFIRMED có thể → CANCELLED; PACKING chỉ nhân viên được hủy theo chính sách. COMPLETED → RETURN_REQUESTED → RETURNED; yêu cầu có thể bị từ chối. Trạng thái thanh toán UNPAID/SIMULATED_PAID/PAID_COD/FAILED/REFUNDED; trạng thái giao hàng WAITING/SHIPPING/DELIVERED/FAILED độc lập.

Doanh thu = tổng tiền hàng sau giảm của đơn COMPLETED trừ tiền hàng đã hoàn; không tính phí vận chuyển, đơn hủy hay khoản sandbox như dòng tiền thật. Báo cáo phải phân biệt doanh số mô phỏng và tiền COD đã ghi nhận.

## Cấu trúc thư mục đích

```text
shop-quan-ao-soa/
  backend/
    common/
    api-gateway/
    auth-user-service/
    catalog-service/
    inventory-service/
    order-service/
    payment-shipping-service/
    promotion-review-service/
    reporting-notification-service/
  frontend/
  database/migrations/
  database/seed/
  docs/architecture/
  docs/diagrams/
  docs/api/
  scripts/
  .env.example
  README.md
```

Phiên bản: Java release 17, Spring Boot 3.5.16, Maven 3.9.x, MySQL 8. React/Vite được khóa bằng package-lock khi xây dựng frontend. Tương thích Java được đối chiếu tại https://docs.spring.io/spring-boot/3.5/system-requirements.html ; OpenAPI tại https://springdoc.org/v2/ .
