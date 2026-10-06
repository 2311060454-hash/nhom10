# Sơ đồ thiết kế (Mermaid)

## Kiến trúc và giao tiếp
```mermaid
flowchart TB
  UI[React: khách hàng / nhân viên / quản trị] --> GW[API Gateway :8080]
  GW --> Auth[Auth :8081]
  GW --> Catalog[Catalog :8082]
  GW --> Inventory[Inventory :8083]
  GW --> Order[Order :8084]
  GW --> Payment[Payment & Shipping :8085]
  GW --> Promo[Promotion & Review :8086]
  GW --> Report[Reporting & Notification :8087]
  Order -->|giá và snapshot| Catalog
  Order -->|reserve / commit / release| Inventory
  Order -->|giữ / xác nhận / bỏ mã| Promo
  Order -->|thanh toán / vận đơn| Payment
  Promo -->|xác minh đã mua| Order
  Report -->|REST tổng hợp| Order
  Auth --> DB1[(shop_quan_ao_auth)]
  Catalog --> DB2[(shop_quan_ao_catalog)]
  Inventory --> DB3[(shop_quan_ao_inventory)]
  Order --> DB4[(shop_quan_ao_order)]
  Payment --> DB5[(shop_quan_ao_payment)]
  Promo --> DB6[(shop_quan_ao_promotion)]
  Report --> DB7[(shop_quan_ao_reporting)]
```

## Use case tổng quát
```mermaid
flowchart LR
  C[Khách hàng] --> A(Đăng ký / đăng nhập / hồ sơ)
  C --> B(Tìm kiếm / giỏ hàng / đặt hàng)
  C --> D(Theo dõi / hủy / đổi trả)
  C --> E(Đánh giá / yêu thích / hỗ trợ)
  S[Nhân viên] --> F(Xác nhận / đóng gói / giao hàng)
  S --> G(Tra cứu / nhập xuất kho theo quyền)
  M[Quản trị] --> F
  M --> G
  M --> H(Quản lý sản phẩm / khuyến mãi / nội dung)
  M --> I(Quản lý tài khoản / phân quyền)
  M --> J(Báo cáo / thống kê)
```

## Sequence đặt hàng
```mermaid
sequenceDiagram
  actor C as Khách hàng
  participant G as Gateway
  participant O as Order
  participant P as Catalog
  participant I as Inventory
  participant V as Promotion
  participant T as Payment
  C->>G: POST /orders + JWT + Idempotency-Key
  G->>O: Yêu cầu đã xác thực
  O->>P: Lấy giá và biến thể
  O->>V: Tính giá khuyến mãi và báo giá coupon
  O->>O: Lưu snapshot, tổng tiền, PROCESSING và unique key
  O-->>C: 202 mã đơn để theo dõi
  O->>V: Giữ coupon(orderId) nếu có
  O->>I: Reserve(orderId, items)
  I->>I: Khóa dòng, kiểm tra, giữ kho
  I-->>O: Reservation
  O->>T: Khởi tạo COD hoặc mô phỏng local
  alt Thất bại chắc chắn
    O->>I: Release(orderId), idempotent
    O->>V: Bỏ lượt giữ mã
    O->>O: FAILED/CANCELLED
  else Thành công
    O->>I: Commit(orderId)
    O->>V: Chốt lượt coupon(orderId) nếu có
    O->>O: PLACED hoặc AWAITING_PAYMENT
  else Timeout kết quả chưa biết
    O->>O: Giữ tác vụ cần đối soát
    O->>I: GET trạng thái theo orderId rồi retry có kiểm soát
  end
```

## Activity
```mermaid
flowchart TD
  A[Nhận yêu cầu] --> B{Khóa đã có?}
  B -->|Có cùng payload| C[Trả kết quả đã lưu]
  B -->|Không| D[Đọc giỏ, giá, địa chỉ]
  D --> E{Hợp lệ và còn hàng?}
  E -->|Không| F[Báo lỗi]
  E -->|Có| G[Giữ kho và mã giảm]
  G --> H{Thanh toán}
  H -->|COD / sandbox thành công| I[Lưu đơn]
  H -->|Thất bại| J[Giải phóng giữ kho]
  I --> K[Xử lý giao hàng]
  K --> L[Hoàn tất hoặc xử lý trả hàng]
```

## ERD theo ranh giới
Các liên kết dưới đây chỉ là FK khi hai bảng cùng service. ID liên service không có FK.
```mermaid
erDiagram
  USERS ||--o{ USER_ROLES : has
  ROLES ||--o{ USER_ROLES : grants
  USERS ||--o{ ADDRESSES : owns
  USERS ||--o{ AUTH_SESSIONS : owns
  USERS ||--o{ PASSWORD_RESET_TOKENS : owns
  CATEGORIES ||--o{ PRODUCTS : contains
  BRANDS ||--o{ PRODUCTS : brands
  PRODUCTS ||--|{ PRODUCT_VARIANTS : variants
  PRODUCTS ||--o{ PRODUCT_IMAGES : images
  INVENTORY ||--o{ INVENTORY_TRANSACTIONS : changes
  RESERVATIONS ||--|{ RESERVATION_ITEMS : holds
  CARTS ||--o{ CART_ITEMS : contains
  ORDERS ||--|{ ORDER_ITEMS : snapshots
  ORDERS ||--|{ ORDER_STATUS_HISTORY : history
  ORDERS ||--o{ RETURN_REQUESTS : requests
  PAYMENTS ||--o{ REFUNDS : refunds
  COUPONS ||--o{ COUPON_USAGES : usages
```

## Class diagram Auth
```mermaid
classDiagram
  AuthController --> AuthService
  AuthService --> UserRepository
  AuthService --> SessionRepository
  AuthService --> PasswordEncoder
  AuthService --> JwtService
  UserRepository --> User
  User "1" --> "*" Address
  User "1" --> "*" AuthSession
  GatewayController --> SessionIntrospection
```
