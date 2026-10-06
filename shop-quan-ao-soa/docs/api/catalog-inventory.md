# API Catalog và Inventory — phần 2

Gateway: `http://localhost:8080`. Catalog: 8082/schema `shop_quan_ao_catalog`; Inventory: 8083/schema `shop_quan_ao_inventory`. Swagger ở `http://localhost:8082/swagger-ui/index.html` và `http://localhost:8083/swagger-ui/index.html`.

## Catalog

| Method | URL | Dữ liệu vào | Kết quả | Quyền |
|---|---|---|---|---|
| GET | /api/catalog/products | q, categoryId, brandId, gender, size, color, min, max, sale, sort, page, limit | PageResult<ProductView> | Công khai |
| GET | /api/catalog/products/{id} | ID | ProductView chỉ biến thể đang bán, giá vốn null | Công khai; sản phẩm ngừng bán trả 404 |
| GET | /api/catalog/categories | Không | Danh mục đang dùng | Công khai |
| GET | /api/catalog/brands | Không | Thương hiệu đang dùng | Công khai |
| GET | /api/catalog/manage/products | Bộ lọc như public | Có sản phẩm ngừng bán/giá vốn | ADMIN |
| GET | /api/catalog/manage/products/{id} | ID | ProductView quản trị | ADMIN |
| POST | /api/catalog/manage/products | ProductInput | ProductView, 201 | ADMIN |
| PUT | /api/catalog/manage/products/{id} | ProductInput có version hiện tại | ProductView | ADMIN |
| DELETE | /api/catalog/manage/products/{id} | ID | 204, ngừng bán | ADMIN |
| GET/POST | /api/catalog/manage/categories | CategoryInput khi POST | Danh sách/bản ghi | ADMIN |
| PUT | /api/catalog/manage/categories/{id} | CategoryInput | Bản ghi | ADMIN |
| GET/POST | /api/catalog/manage/brands | BrandInput khi POST | Danh sách/bản ghi | ADMIN |
| PUT | /api/catalog/manage/brands/{id} | BrandInput | Bản ghi | ADMIN |
| POST | /api/catalog/manage/products/{id}/images | multipart field file | ImageView | ADMIN |
| DELETE | /api/catalog/manage/products/{id}/images/{imageId} | ID | 204 | ADMIN |
| GET | /api/catalog/images/{imageId} | ID ảnh UUID.png | image/png | Công khai |
| GET | /internal/catalog/variants/{id} | ID biến thể | Quote | X-Internal-Key, chỉ cổng service |

`ProductInput`: name, categoryId, brandId, description, material, style, gender (NAM/NU/UNISEX), active, featured, version (null khi tạo), variants[]. Mã sản phẩm do backend sinh, không nhập từ frontend.

`VariantInput`: id (null khi thêm), sku, size, color, costPrice, price, salePrice (null khi không giảm), active. Tiền dùng DECIMAL/BigDecimal. Khi sửa gửi toàn bộ biến thể, giữ ID cũ. Không xóa biến thể cũ; đặt active=false. SKU unique, tổ hợp product/size/color unique, giá giảm không vượt giá bán. Mở bán yêu cầu ít nhất một biến thể active. Version cũ trả 409.

`ProductView`: id, code, name, categoryId/name, brandId/name, description, material, style, gender, active, featured, version, createdAt, variants[], images[]. `ImageView`: id, url. `Quote`: variantId, productId, categoryId, sku, productName, size, color, price (giá hiệu lực), active. Quote không chứa giá vốn.

`CategoryInput`: name, parentId nullable, active. Cấm chu trình danh mục cha. Không ngừng danh mục đang có sản phẩm hoạt động hoặc danh mục con hoạt động. `BrandInput`: name, active; không ngừng thương hiệu có sản phẩm đang bán. Tên danh mục unique toàn shop trong bản này.

Lọc min/max tính trên cùng biến thể active với size/color đang lọc; sort nhận `newest`, `priceAsc`, `priceDesc`, `bestSelling`. Giá sắp xếp là giá thấp nhất của các biến thể active của sản phẩm. `bestSelling` nhận số lượng bán ròng từ Reporting qua REST nội bộ (đơn COD `COMPLETED` + `PAID`, đã trừ hàng hoàn một phần), sắp xếp toàn bộ sản phẩm phù hợp trước khi phân trang; sản phẩm chưa bán đứng sau và cùng số lượng thì sản phẩm mới hơn đứng trước. Reporting giới hạn đọc 10.000 đơn; Catalog trả 503 nếu có hơn 10.000 sản phẩm phù hợp để tránh phân trang sai. Khi Reporting hoặc Order không phản hồi, bộ lọc bán chạy trả 503. Phân trang backend, limit 1..100. `categoryId` lọc cả danh mục đã chọn và mọi cấp danh mục con; các bộ lọc khác vẫn áp dụng đồng thời ở database trước phân trang.

Ảnh: JPEG/PNG, tối đa 5MB đầu vào, 6 triệu điểm ảnh và 10 ảnh/sản phẩm. Xác minh nội dung bằng ImageIO, giải mã và mã hóa lại PNG để không giữ nội dung chèn thêm. Đầu ra tối đa 8MB, tên UUID do server sinh, không sử dụng tên tệp người dùng làm đường dẫn. File ở uploads/catalog, metadata ở DB. Rollback xóa file vừa ghi. Gateway tắt multipart parsing để chuyển tiếp nguyên vẹn body.

## Inventory

| Method | URL | Dữ liệu vào | Kết quả | Quyền |
|---|---|---|---|---|
| GET | /api/inventory/availability/{variantId} | ID | variantId, available | Công khai |
| GET | /api/inventory | variantId tùy chọn, low, page, size | PageResult<StockView> | ADMIN/STAFF |
| GET | /api/inventory/history | variantId tùy chọn, page | PageResult<TransactionView> | ADMIN/STAFF |
| POST | /api/inventory/adjustments | Adjustment + Idempotency-Key | StockView | ADMIN hoặc STAFF có INVENTORY_WRITE |
| POST | /internal/inventory/reservations/{orderId} | Reserve | ReservationView | X-Internal-Key |
| GET | /internal/inventory/reservations/{orderId} | UUID đơn | ReservationView | X-Internal-Key |
| POST | /internal/inventory/reservations/{orderId}/commit | Không | ReservationView | X-Internal-Key |
| POST | /internal/inventory/reservations/{orderId}/release | Không | ReservationView | X-Internal-Key |
| POST | /internal/inventory/reservations/{orderId}/restock | Không | ReservationView | X-Internal-Key |

`Adjustment`: variantId, type (RECEIPT/ISSUE/COUNT/MINIMUM), quantity, reason. COUNT là số đếm thực tế, không phải delta. Idempotency-Key 8..100 ký tự chữ/số/_/-. Cùng key và payload trả snapshot kết quả cũ; khác payload trả 409. Người thực hiện lấy từ JWT, không từ body. Variant phải tồn tại theo API Catalog; không đọc DB Catalog. Giao tiếp Catalog diễn ra trước transaction kho, timeout 2s/lần, tối đa 2 lần GET với backoff 100ms khi lỗi I/O.

`StockView`: variantId, onHand, reserved, available, minimumStock. Khách chỉ được thấy variantId/available. Chưa có dòng kho thì available=0. Sắp hết hàng khi available<=minimumStock.

`Reserve`: items[] có variantId, quantity; tối đa 100 dòng, không trùng variant. `ReservationView`: orderId, state, expiresAt, items[]. Order ID 36 ký tự dạng UUID. Hạn giữ 15 phút; tác vụ mỗi 30 giây xử lý tối đa 100 lượt hết hạn, phần chưa xử lý được giữ đến vòng tiếp theo.

## Bất biến và giới hạn

- Trong transaction, khóa bi quan một dòng `inventory_guard` trước mọi thao tác ghi. Đây là lựa chọn đơn giản cho bản local: tất cả ghi kho được tuần tự hóa, tránh cạnh tranh giữa giữ/commit/release và ghi lặp. Đổi lại thông lượng thấp hơn khóa từng biến thể; không tuyên bố phù hợp tải lớn.
- Sau khi khóa mới đọc Stock, Reservation và idempotency. Tồn khả dụng = on_hand-reserved. CHECK ở MySQL ngăn on_hand<reserved hoặc tồn âm.
- Giữ nhiều dòng là một transaction: một dòng thiếu sẽ rollback toàn bộ.
- HELD → COMMITTED trừ onHand và reserved; HELD → RELEASED/EXPIRED chỉ giảm reserved; COMMITTED → RESTOCKED cộng onHand. Gọi lặp trạng thái đích không thay kho thêm lần nữa.
- Commit lượt đã hết hạn bị từ chối; worker sẽ giải phóng. Release không dùng cho lượt đã xuất. Restock hiện hoàn toàn bộ reservation, chưa hỗ trợ trả hàng một phần.
- Mọi biến động ghi audit kho gồm delta, reservedDelta, actorId, lý do và thời gian. Actor 0 dành cho seed/tác vụ nội bộ.
- API nội bộ không được Gateway công khai. Order sau này phải kiểm tra Quote active, snapshot giá và điều phối saga. Việc đã có API giữ kho **không có nghĩa luồng đặt hàng đã triển khai**.
- Ảnh mẫu chưa được seed; UI có trạng thái thiếu ảnh. Admin có thể tải ảnh thật. Chưa có giỏ hàng, nút mua, thanh toán, banner hoặc báo cáo bán chạy.
