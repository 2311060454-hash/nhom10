# Database và dữ liệu mẫu

1. Tạo kết nối Workbench đến **MySQL Server** `localhost:3310`, user `root`, dùng mật khẩu phát triển cá nhân đã cung cấp.
2. Mở `00-create-databases.sql`, chạy toàn bộ để tạo 7 database utf8mb4. Script không xóa hoặc sửa dữ liệu sẵn có.
3. Chạy Auth bằng profile `local`. Flyway tự chạy `backend/auth-user-service/src/main/resources/db/migration/V1__auth.sql`, ghi lịch sử vào `flyway_schema_history`.
4. JPA sử dụng `ddl-auto: validate` để phát hiện sai schema, không tự sửa/xóa bảng.
5. `DevSeed` tạo ba tài khoản nếu email chưa có. Mật khẩu lấy từ `SEED_PASSWORD` và được BCrypt trước khi INSERT. Không có mật khẩu rõ hoặc hash chung công khai trong SQL.

Không chạy thủ công V1 rồi chạy Flyway trên cùng database: sẽ thiếu metadata lịch sử. Luồng hỗ trợ là Workbench tạo database, Flyway chạy migration khi ứng dụng khởi động. Có thể mở V1 trong Workbench để đọc thiết kế và dùng SELECT để kiểm tra sau khi chạy.

```sql
SELECT @@port, VERSION();
SHOW DATABASES LIKE 'shop_quan_ao_%';
USE shop_quan_ao_auth;
SELECT installed_rank, version, description, success FROM flyway_schema_history;
SELECT id, email, full_name, active FROM users;
SELECT user_id, role_name FROM user_roles;
SELECT id, user_id, recipient, detail FROM addresses;
SELECT action, COUNT(*) FROM audit_logs GROUP BY action;
```

Database Auth sở hữu đầy đủ bảng users, roles, user_roles, addresses, auth_sessions, password_reset_tokens và audit_logs. Mật khẩu reset được lưu SHA-256; bản rõ chỉ có trong thư local ngoài repository. Phiên lưu thời hạn và revoked. `user_roles` có FK đến users và roles. Địa chỉ và phiên có FK đến users. Audit giữ ID lịch sử mà không ép cascade xóa.

Catalog và Inventory đã có V1 trong `backend/catalog-service/src/main/resources/db/migration/` và `backend/inventory-service/src/main/resources/db/migration/`. Chạy theo thứ tự Auth → Catalog → Inventory → Payment/Shipping → Promotion/Review → Order → Reporting/Notification → Gateway. Mỗi service chỉ kết nối schema riêng; Inventory xác minh biến thể và seed bằng REST Catalog.

Catalog local seed: 6 sản phẩm/48 biến thể, 2 danh mục và 1 thương hiệu. Inventory local seed: 30 đơn vị mỗi biến thể mẫu khi kho rỗng. SQL không chứa mật khẩu hoặc khóa API. Các bảng Catalog có FK nội bộ categories/brands/products/variants/images; Inventory có FK nội bộ stock/transactions/reservations/items. variant_id bên kho chỉ là tham chiếu logic, không có FK xuyên DB.

Order có V1__orders.sql, V2__fulfillment.sql và V3__coupon_snapshot.sql; Payment có V1__payment_shipping.sql; Promotion có V1__promotion_review.sql với coupons, coupon_usages, promotions, reviews và wishlists. Reporting có V1__reporting_notifications.sql lưu notifications và V2__customer_support.sql lưu ticket/tin nhắn/lịch sử hỗ trợ; báo cáo được tổng hợp qua REST, không sao chép bảng đơn sang schema này. Chạy Smoke-Orders.ps1 và Smoke-PaymentShipping.ps1 để tạo đơn kiểm thử thật cùng lịch sử; các script không giả tạo đơn hoàn tất bằng INSERT. Không sửa migration đã chạy hoặc xóa flyway_schema_history.



Dùng `scripts/Smoke-PromotionReview.ps1` sau khi khởi động để kiểm tra coupon, giá theo sản phẩm, đánh giá và wishlist trên MySQL. Mở Workbench: `USE shop_quan_ao_promotion; SHOW TABLES; SELECT * FROM flyway_schema_history;` rồi xem bảng nghiệp vụ mà không chỉnh sửa thủ công.

Bảng `notifications` thuộc Reporting và chỉ giữ ID đơn/tham chiếu cùng nội dung trạng thái; không tạo FK xuyên sang Order. Trong Workbench: `USE shop_quan_ao_reporting; SHOW TABLES; SELECT * FROM flyway_schema_history; SELECT id,user_id,order_id,state,read_at FROM notifications ORDER BY id DESC LIMIT 20;`

Kiểm tra V2 trong Workbench: `USE shop_quan_ao_reporting; SELECT version,description,success FROM flyway_schema_history ORDER BY installed_rank; SELECT id,user_id,subject,state FROM customer_support ORDER BY id DESC LIMIT 20;`. Không xóa dữ liệu hỗ trợ để chạy lại seed hoặc smoke.
