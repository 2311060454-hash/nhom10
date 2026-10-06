# Từ điển dữ liệu đích

Tài liệu này tổng hợp bảng đã có và bảng còn ở mức thiết kế; migration Flyway của từng service là nguồn chính xác cho schema hiện chạy. Định danh liên service chỉ là BIGINT/UUID, không tạo FK xuyên database.

Mọi entity nghiệp vụ có created_at, updated_at DATETIME(6) khi có thể sửa; bảng sự kiện bất biến chỉ có created_at. ID mặc định BIGINT PK AUTO_INCREMENT. Tiền DECIMAL(15,2), phần trăm DECIMAL(5,2), thời gian UTC; quantity INT CHECK >=0. Trường bắt buộc dùng NOT NULL. Các chuỗi hiển thị dùng utf8mb4.

| Service / Bảng | Trường chính ngoài id | Ràng buộc/index cần có |
|---|---|---|
| Auth.users | email, full_name, phone, password_hash, active, marketing_consent, marketing_consent_at, created_at, updated_at | UNIQUE(email); `marketing_consent_at` NULL khi tắt; Auth Flyway V2 |
| Catalog.categories | name VARCHAR(120), parent_id nullable, active | UNIQUE(name,parent_id), FK cha trong bảng; kiểm tra chu trình ở service |
| Catalog.brands | name VARCHAR(120), active | UNIQUE(name) |
| Catalog.products | code VARCHAR(50), name VARCHAR(200), category_id, brand_id, description TEXT, material, style, gender, active, featured | UNIQUE(code), FK category/brand, INDEX(name,active), INDEX(category_id,active) |
| Catalog.product_images | product_id, storage_key VARCHAR(100), media_type, byte_size, sort_order | FK product, UNIQUE(storage_key); validate bytes ảnh |
| Catalog.product_variants | product_id, sku VARCHAR(80), size VARCHAR(20), color VARCHAR(50), cost_price, price, sale_price nullable, active | UNIQUE(sku), UNIQUE(product_id,size,color), FK product; CHECK giá không âm, sale_price <= price |
| Catalog.banners | title, subtitle, link_path, image_id, sort_order, starts_at, ends_at, active, created_at, updated_at | UNIQUE(image_id), INDEX(active,starts_at,ends_at,sort_order); link_path chỉ `/` hoặc `/products/{id}`; Flyway V2 |
| Catalog.store_content | content_key VARCHAR(40), content_text TEXT, updated_at | PK(content_key); sáu khóa nội dung cố định; Flyway V2 |
| Inventory.inventory | variant_id, on_hand, reserved, minimum_stock, version | UNIQUE(variant_id), CHECK(on_hand>=reserved AND reserved>=0) |
| Inventory.inventory_transactions | inventory_id, operation_key, delta, type, reason, actor_id, order_id nullable | FK inventory; UNIQUE(operation_key); INDEX(inventory_id,created_at) |
| Inventory.reservations | order_id UUID, request_hash, state, expires_at | UNIQUE(order_id), INDEX(state,expires_at) |
| Inventory.reservation_items | reservation_id, inventory_id, quantity | FK reservation/inventory; UNIQUE(reservation_id,inventory_id); CHECK(quantity>0) |
| Inventory.partial_return_restocks | return_id UUID, order_id UUID, request_hash, created_at | PK(return_id), UNIQUE(order_id), FK reservation nội bộ; Flyway V2; mỗi đơn hoàn một phần tối đa một lần |
| Order.carts | user_id, version | UNIQUE(user_id) |
| Order.cart_items | cart_id, variant_id, quantity | FK cart; UNIQUE(cart_id,variant_id); CHECK(quantity>0) |
| Order.orders | code, user_id, idempotency_key, request_hash, state, recipient, phone, address_snapshot, note, subtotal, discount, shipping_fee, total, payment_method | UNIQUE(code), UNIQUE(user_id,idempotency_key), INDEX(user_id,created_at), INDEX(state,created_at) |
| Order.order_items | order_id, product_id, variant_id, sku_snapshot, name_snapshot, size_snapshot, color_snapshot, image_snapshot, unit_price, quantity, line_total | FK order; CHECK(quantity>0); snapshot bất biến |
| Order.order_status_history | order_id, from_state, to_state, actor_id, reason | FK order; INDEX(order_id,created_at) |
| Order.return_requests | id UUID, order_id, user_id, request_key, reason, mode, refund_amount, state, decision_note, refund_reference, actor_id, created_at, updated_at, last_error | UNIQUE(order_id), FK order nội bộ, INDEX(state,updated_at); Flyway V4/V5 |
| Order.return_items | return_id, variant_id, quantity | FK return_requests nội bộ, UNIQUE(return_id,variant_id), CHECK(quantity>0); Flyway V5 |
| Order.return_status_history | return_id, state, actor_id, note, created_at | FK return_requests nội bộ, INDEX(return_id,id); Flyway V4 |
| Order.saga_tasks | order_id, step, request_hash, state, attempts, next_retry_at, last_error_code | FK order; UNIQUE(order_id,step); INDEX(state,next_retry_at) |
| Payment.payments | order_id, method, state, amount, provider_reference nullable, operation_key | UNIQUE(order_id), UNIQUE(operation_key), UNIQUE(provider_reference) khi có |
| Payment.shipments | order_id, carrier, tracking_code nullable, fee, state, assigned_staff_id nullable | UNIQUE(order_id), INDEX(tracking_code) |
| Payment.refunds | order_id, amount, state, reason, reference, created_at, updated_at | FK payments nội bộ, UNIQUE(order_id); state SIMULATED_REFUNDED/PENDING_MANUAL/REFUNDED; Flyway V2 |
| Promotion.coupons | code, type, discount_value, minimum_total, maximum_discount, total_limit, per_customer_limit, starts_at, ends_at, active, created_at, updated_at | UNIQUE(code); CHECK tiền và thời gian; INDEX(active,starts_at,ends_at) |
| Promotion.coupon_usages | order_id PK, coupon_id, user_id, state, discount, original_subtotal, expires_at, created_at, updated_at | FK nội bộ coupon; INDEX(coupon_id,user_id,state), INDEX(state,expires_at); order_id duy nhất để chống gọi lặp |
| Promotion.promotions | name, target_type PRODUCT/CATEGORY, target_id, type, discount_value, maximum_discount, starts_at, ends_at, active, created_at, updated_at | INDEX(target_type,target_id,active,starts_at,ends_at); ID target tham chiếu logic qua REST Catalog |
| Promotion.reviews | product_id, user_id, stars, comment, state, created_at, updated_at | UNIQUE(user_id,product_id); CHECK stars 1–5; INDEX(product_id,state,created_at); điều kiện mua qua REST Order |
| Promotion.wishlists | user_id, product_id | UNIQUE(user_id,product_id) |
| Reporting.notifications | user_id, event_key, order_id nullable, link_path nullable, state, title, body, created_at, read_at nullable | UNIQUE(event_key), INDEX(user_id,created_at,id), INDEX(user_id,read_at); Reporting Flyway V3 |
| Reporting.customer_support | user_id, request_key, request_hash, subject, state, created_at, updated_at | UNIQUE(user_id,request_key), INDEX(user_id,created_at), INDEX(state,updated_at) |
| Reporting.support_messages | ticket_id, author_id, author_role, request_key, request_hash, message_text, created_at | FK ticket nội bộ; UNIQUE(ticket_id,request_key), INDEX(ticket_id,created_at,id) |
| Reporting.support_status_history | ticket_id, state, actor_id, created_at | FK ticket nội bộ, INDEX(ticket_id,created_at,id) |
| Reporting.reporting_events (thiết kế, chưa triển khai) | event_key, aggregate_id, type, occurred_at, payload JSON | UNIQUE(event_key), INDEX(type,occurred_at) |

Snapshot đơn hàng không phụ thuộc tên/giá hiện tại trong Catalog. Doanh thu phải phân biệt COD thực nhận và sandbox. Chỉ một bên điều phối bù kho: Order saga. Inventory ghi operation_key cho mọi thao tác đảo để chống cộng kho hai lần.
