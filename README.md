HỆ THỐNG QUẢN LÝ SHOP QUẦN ÁO THEO KIẾN TRÚC HƯỚNG DỊCH VỤ (SOA)

> **Nhóm 10 — Chuyên ngành Công nghệ Phần mềm**  
> **Công nghệ:** Java 17, Spring Boot 3.5, ReactJS + Vite, Bootstrap 5, MySQL 8  
> **Kiến trúc:** Service-Oriented Architecture (SOA) — Database per Service  

---

## 📌 1. GIỚI THIỆU HỆ THỐNG

Hệ thống được thiết kế và phát triển hoàn chỉnh theo kiến trúc hướng dịch vụ (SOA) phục vụ cả **Bán hàng trực tuyến** (Storefront) và **Quản lý vận hành cửa hàng thời trang** (Admin Dashboard).

Hệ thống bao gồm **8 Microservices backend** độc lập giao tiếp qua REST API, 1 **API Gateway** định tuyến tập trung, **7 cơ sở dữ liệu MySQL** riêng biệt và giao diện người dùng **ReactJS (Vite)** responsive 100% bằng tiếng Việt và tiền tệ VNĐ.

---

## 🏛️ 2. KIẾN TRÚC VÀ CỔNG DỊCH VỤ (SOA SERVICES & PORTS)

| Dịch vụ (Service) | Cổng | Cơ sở dữ liệu (MySQL) | Chức năng chính |
| :--- | :---: | :--- | :--- |
| **API Gateway** | `8080` | *(Không lưu DB)* | Điểm tiếp nhận duy nhất, xác thực JWT, định tuyến, CORS, chống tấn công |
| **auth-user-service** | `8081` | `shop_quan_ao_auth` | Đăng ký, đăng nhập JWT, BCrypt, phân quyền ADMIN/STAFF/CUSTOMER, sổ địa chỉ, hồ sơ |
| **catalog-service** | `8082` | `shop_quan_ao_catalog` | Danh mục đa cấp, thương hiệu, sản phẩm, biến thể (size/màu/SKU), upload album ảnh, banner |
| **inventory-service** | `8083` | `shop_quan_ao_inventory` | Tồn kho thời gian thực, giữ hàng (Reservation), nhập/xuất/kiểm kê, cảnh báo sắp hết |
| **order-service** | `8084` | `shop_quan_ao_order` | Giỏ hàng, đặt hàng Idempotent, snapshot giá/danh mục, lịch sử trạng thái, yêu cầu đổi trả |
| **payment-shipping-service** | `8085` | `shop_quan_ao_payment` | Thanh toán COD / Sandbox, điều hành giao vận (mã vận đơn, hãng ship, đối soát hoàn tiền) |
| **promotion-review-service** | `8086` | `shop_quan_ao_promotion` | Mã giảm giá Coupon (% hoặc số tiền), ưu đãi sản phẩm, đánh giá sao, danh sách yêu thích |
| **reporting-notification-service** | `8087` | `shop_quan_ao_reporting` | Dashboard doanh thu COD thực tế, báo cáo danh mục, đơn hủy/trả, xuất CSV, ticket hỗ trợ |
| **Frontend (React + Vite)** | `5173` | *(Client Browser)* | Giao diện khách hàng (`StoreLayout`) & Bảng điều khiển quản trị (`AdminLayout` Sidebar) |

---

## 🚀 3. HƯỚNG DẪN KHỞI CHẠY HỆ THỐNG TRÊN WINDOWS (1 CLICK)

### Yêu cầu tiên quyết:
- **Java 17+** (OpenJDK hoặc Oracle JDK)
- **Node.js 18+** & **npm**
- **MySQL Server 8.x** đang chạy tại `localhost:3310`, user `root`, password `12345` (hoặc cấu hình trong file `.env`)

### Cách khởi chạy nhanh nhất:
1. Nhấp đúp chuột (Double Click) vào file:
   👉 **`CHAY-HE-THONG.cmd`** ngay tại thư mục gốc `nhom10`.
2. Hệ thống sẽ tự động:
   - Dọn dẹp tiến trình cũ nếu còn sót lại.
   - Bật lần lượt cả 8 Microservices Backend và kiểm tra trạng thái từng service (`UP`).
   - Khởi động giao diện Frontend React (Vite).
   - Tự động mở trình duyệt web tại địa chỉ: **`http://localhost:5173`**.

### Cách dừng toàn bộ hệ thống:
- Nhấp đúp chuột vào file: 👉 **`DUNG-HE-THONG.cmd`**.

---

## 🔑 4. DANH SÁCH TÀI KHOẢN MẪU & MẬT KHẨU

> **Mật khẩu chung cho tất cả tài khoản:** `Password@123`

| Vai trò | Email đăng nhập | Quyền hạn và phạm vi hoạt động |
| :--- | :--- | :--- |
| **Quản trị viên (ADMIN)** | `admin@shop.local` | Toàn quyền quản trị hệ thống: Dashboard doanh thu, Quản lý sản phẩm & biến thể, Album ảnh, Danh mục, Thương hiệu, Kho hàng, Đơn hàng, Giao vận, Khách hàng & Tổng chi tiêu, Nhân viên, Coupon, Đánh giá, Báo cáo CSV |
| **Nhân viên (STAFF)** | `staff@shop.local` | Bàn làm việc nhân viên (`/staff`), Xử lý và duyệt đơn hàng (`/manage/orders`), Phân công và điều phối giao vận (`/manage/shipping`), Nhập xuất kho theo quyền (`/inventory`) |
| **Khách hàng (CUSTOMER)** | `customer@shop.local` | Mua sắm, bộ lọc nâng cao, nút "Mua ngay", Giỏ hàng, Đặt hàng COD/Sandbox, Quản lý đơn hàng của tôi, Yêu cầu trả hàng, Đánh giá, Yêu thích, Gửi hỗ trợ |

*(Khách hàng mới có thể tự do đăng ký tài khoản trực tiếp qua nút **"Tạo tài khoản"** trên web)*.

---

## 📱 5. DANH SÁCH CÁC TRANG & CHỨC NĂNG CHÍNH

### Dành cho Khách hàng:
- **Trang chủ (`/`):** Banner khuyến mãi, Bộ lọc nhanh (Nam, Nữ, Unisex, Nổi bật ⭐, Giảm giá 🔥), phân trang, lọc giá, size, màu, thương hiệu.
- **Chi tiết sản phẩm (`/products/:id`):** Chọn kích cỡ, màu sắc, kiểm tra tồn kho thời gian thực, nút **"Lưu vào giỏ"** và **"Mua ngay"** (tự chuyển checkout), đánh giá sao, sản phẩm cùng danh mục.
- **Giỏ hàng (`/cart`):** Tăng/giảm số lượng (+ / −), áp mã giảm giá, tính phí vận chuyển, tạm tính và tổng thanh toán.
- **Đặt hàng (`/checkout`):** Chọn địa chỉ đã lưu hoặc nhập mới, ghi chú giao hàng, chọn COD hoặc Mô phỏng trực tuyến, bảo vệ Idempotency chống tạo trùng đơn.
- **Xác nhận đặt hàng (`/orders/:id?placed=true`):** Thông báo đặt hàng thành công và theo dõi tiến trình đơn hàng.
- **Đơn hàng của tôi (`/orders`):** Lịch sử đơn hàng, xem chi tiết, hủy đơn khi hợp lệ, gửi yêu cầu đổi trả / hoàn tiền.
- **Trang tiện ích cá nhân:** Hồ sơ (`/account`), Sổ địa chỉ (`/addresses`), Yêu thích (`/favorites`), Đánh giá của tôi (`/my-reviews`), Thông báo (`/notifications`), Ticket hỗ trợ (`/support`), Liên hệ (`/contact`).

### Dành cho Quản trị viên & Nhân viên (`AdminLayout`):
- **Dashboard Quản trị (`/admin/dashboard`):** Doanh thu COD thực tế, hôm nay, tuần này, tháng này, năm nay, số đơn chờ/giao/hoàn tất/hủy, sản phẩm bán chạy, đơn gần đây.
- **Quản lý sản phẩm (`/admin/products`):** Thêm mới (`/admin/products/new`), sửa, tải ảnh máy tính (JPEG/PNG) có xem trước, quản lý SKU/size/màu/giá.
- **Quản lý danh mục (`/admin/categories`):** Cây danh mục cha/con, trạng thái hoạt động.
- **Quản lý thương hiệu (`/admin/brands`):** Danh sách thương hiệu thời trang.
- **Quản lý kho hàng (`/inventory`):** Tồn thực tế, Đang giữ, Khả dụng, Nhập kho (`RECEIPT`), Xuất kho (`ISSUE`), Kiểm kê (`COUNT`), Mức tối thiểu (`MINIMUM`), xuất CSV tồn kho.
- **Quản lý đơn hàng (`/manage/orders`):** Tìm kiếm đơn theo mã/khách/sđt/ngày, duyệt đơn, chuẩn bị hàng.
- **Quản lý giao vận (`/manage/shipping`):** Xem địa chỉ, gán đơn vị vận chuyển, mã vận đơn, người phụ trách, chi phí hãng ship, xác nhận giao thành công / thất bại / thu COD.
- **Yêu cầu đổi trả (`/manage/returns`):** Duyệt yêu cầu hoàn tiền/hoàn hàng, đối soát kho và tiền hoàn.
- **Quản lý người dùng (`/manage/users`):** Xem hồ sơ chi tiết khách hàng, **tổng chi tiêu tích lũy**, lịch sử mua hàng, khóa/mở tài khoản nhanh, cấp quyền nhân viên kho.
- **Marketing & Nội dung:** Mã giảm giá (`/admin/coupons`), Khuyến mãi (`/admin/promotions`), Duyệt đánh giá (`/admin/reviews`), Banner (`/admin/banners`), Nội dung shop (`/admin/store-content`).
- **Báo cáo & Thống kê:** Báo cáo thời gian (`/admin/reports`), Doanh thu theo danh mục (`/admin/category-revenue`), Báo cáo đơn hủy/trả (`/admin/order-exceptions`).

---

## 🧪 6. KẾT QUẢ KIỂM THỬ HỆ THỐNG (TEST EVIDENCE)

- **Backend (JUnit 5 & Spring Boot Test):** **111/111** bài kiểm thử tự động đạt `BUILD SUCCESS` (0 lỗi, 0 thất bại).
- **Frontend (Vitest & React Testing Library):** **44/44** bài kiểm thử tự động đạt `PASS`.
- **Production Build:** `npm run build` hoàn thành xuất sắc với 0 lỗi.
- Chi tiết bằng chứng kiểm thử xem tại: [docs/verification.md](shop-quan-ao-soa/docs/verification.md).

---

## 📑 7. TÀI LIỆU BÁO CÁO ĐỒ ÁN HOÀN CHỈNH
Nội dung tài liệu đồ án tốt nghiệp chuẩn 5 chương (Tổng quan, Cơ sở lý thuyết, Phân tích thiết kế hệ thống kèm các sơ đồ kiến trúc/Use Case/Sequence/Activity/ERD, Xây dựng triển khai và Kết luận) được lưu trữ đầy đủ tại:
👉 **[docs/BAO-CAO-DO-AN-HOAN-CHINH.md](shop-quan-ao-soa/docs/BAO-CAO-DO-AN-HOAN-CHINH.md)**

---

## 🛠️ 8. HƯỚNG DẪN XỬ LÝ SỰ CỐ THƯỜNG GẶP

1. **Lỗi `Access denied for user 'root'@'localhost'`:**
   - Mở file `.env` tại `shop-quan-ao-soa/.env` và cập nhật mật khẩu MySQL của máy bạn tại dòng `DB_PASSWORD=...`.
2. **Lỗi `Port 3310 không kết nối được`:**
   - Mở dịch vụ MySQL trong Services của Windows và kiểm tra cổng dịch vụ (mặc định cấu hình đồ án là 3310).
3. **Lỗi `Running scripts is disabled on this system` (PowerShell ExecutionPolicy):**
   - Không cần thay đổi cài đặt bảo mật hệ thống; chỉ cần sử dụng file thực thi nhanh **`CHAY-HE-THONG.cmd`** (đã tích hợp cờ `-ExecutionPolicy Bypass`).
4. **Lỗi `Port 8080/5173 đã được sử dụng`:**
   - Chạy file **`DUNG-HE-THONG.cmd`** để giải phóng các tiến trình Java và Node.js đang chạy ngầm trước khi mở lại.
