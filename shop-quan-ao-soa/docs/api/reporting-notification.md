# Reporting & Notification Service (8087)

Service sở hữu `shop_quan_ao_reporting` và bảng `notifications`. Báo cáo tính từ snapshot Order qua `GET /internal/orders/report` (100 đơn/trang, tối đa 10.000 đơn mỗi yêu cầu), kết hợp số lượng khách từ Auth, sản phẩm từ Catalog và tồn thấp từ Inventory qua REST nội bộ. Không truy cập trực tiếp schema khác. API công khai chỉ đi qua Gateway `8080`; truy cập `/internal/**` cần khóa service.

| HTTP | URL | Đầu vào → đầu ra | Quyền |
|---|---|---|---|
| GET | `/api/reports/dashboard` | Số đơn theo trạng thái; doanh thu COD hôm nay/tuần/tháng/năm/tổng; giá trị mô phỏng; sản phẩm bán chạy, đơn gần đây; tổng khách/sản phẩm/tồn thấp → `Dashboard` | ADMIN |
| GET | `/api/reports/revenue?from=YYYY-MM-DD&to=YYYY-MM-DD&group=DAY` | Khoảng ngày và `DAY/MONTH/QUARTER/YEAR` → tổng và bucket | ADMIN |
| GET | `/api/reports/revenue.csv?from=&to=&group=` | Cùng bộ lọc → CSV UTF-8 BOM | ADMIN |
| GET | `/api/reports/exceptions?from=YYYY-MM-DD&to=YYYY-MM-DD` | Số đơn hủy, trả toàn bộ/một phần, tiền hoàn COD/mô phỏng và danh sách sự kiện | ADMIN |
| GET | `/api/reports/exceptions.csv?from=&to=` | Cùng bộ lọc → CSV UTF-8 BOM | ADMIN |
| GET | `/api/reports/categories?from=YYYY-MM-DD&to=YYYY-MM-DD` | Doanh thu hàng hóa COD theo danh mục snapshot của đơn mới, phí vận chuyển riêng | ADMIN |
| GET | `/api/reports/categories.csv?from=&to=` | Cùng bộ lọc → CSV UTF-8 BOM | ADMIN |
| GET | `/internal/reports/best-sellers` | Danh sách productId, tên snapshot, số lượng bán ròng COD và giá trị dòng hàng, phục vụ sắp xếp Catalog | Khóa service nội bộ |
| GET | `/api/notifications?page=` | Đồng bộ trạng thái đơn, trả hàng và khuyến mãi đã đồng ý; trả trang 20 thông báo | Đã đăng nhập |
| GET | `/api/notifications/unread-count` | Đồng bộ rồi trả số chưa đọc | Đã đăng nhập |
| PUT | `/api/notifications/{id}/read` | Đánh dấu một thông báo đã đọc, gọi lặp không đổi kết quả | Chủ thông báo |

Chỉ đơn `COMPLETED`, phương thức `COD`, trạng thái thanh toán `PAID` được cộng vào **doanh thu đã thu**; khoản hoàn một phần đã xác nhận được trừ qua `returnedAmount`. Tổng giá trị đơn đã gồm giảm giá và phí vận chuyển. Đơn `SIMULATED_PAID` hoàn tất được trình bày riêng là **giá trị mô phỏng**, không tính là tiền thật. Đơn trả toàn bộ `RETURNED`, đơn hủy, thất bại hoặc chưa hoàn tất không tính doanh thu. Kỳ doanh thu dùng thời điểm chuyển `COMPLETED` theo múi giờ `Asia/Ho_Chi_Minh`; số đơn tổng quan đếm trạng thái hiện tại.

Báo cáo đơn hủy/hoàn dùng ngày chuyển `CANCELLED` hoặc ngày sự kiện hoàn tiền `REFUNDED` theo `Asia/Ho_Chi_Minh`, không dùng ngày tạo đơn. Trả một phần vẫn là đơn `COMPLETED`, được nhận diện qua lịch sử Return của Order Service. Một dòng cho mỗi đơn hủy hoặc lần hoàn tiền đã ghi nhận; yêu cầu trả đang chờ, bị từ chối hoặc chưa xác nhận hoàn tiền không được tính. `codRefunds` là số tiền COD đã được ADMIN ghi hoàn, `simulatedRefunds` chỉ là giá trị hoàn mô phỏng, không phải tiền chuyển khoản. Đơn hủy có `refundAmount=0` trong báo cáo này. Dữ liệu giới hạn bởi 10.000 đơn của endpoint nội bộ; vượt giới hạn trả 503 thay vì cắt âm thầm.

Báo cáo danh mục chỉ dùng đơn `COMPLETED` + `COD` + `PAID`, lọc theo ngày hoàn tất. Order gửi giá, số lượng và `categoryId` snapshot trong từng `orderItem`, số đã trả, tiền hoàn và phí vận chuyển. Reporting ưu tiên `categoryId` đã lưu khi đặt hàng; Catalog cung cấp tên danh mục và ánh xạ sản phẩm hiện tại qua REST nội bộ. Reporting chia doanh thu hàng hóa sau giảm giá và hoàn một phần cho các danh mục theo tỷ trọng giá trị hàng còn lại, làm tròn VNĐ đến hai chữ số thập phân và dồn chênh lệch làm tròn vào danh mục cuối. Phí giao không gán cho danh mục và hiển thị riêng; `merchandiseRevenue + shippingRevenue = totalRevenue` bằng báo cáo doanh thu COD cùng kỳ. Số đơn ở mỗi danh mục là số đơn có ít nhất một sản phẩm còn lại thuộc danh mục đó. Nếu danh mục không còn trong Catalog, tên là `Chưa phân loại` nhưng ID snapshot vẫn được giữ. **Giới hạn lịch sử:** migration V6 để `category_id` NULL cho đơn cũ; chỉ các dòng này dùng danh mục hiện tại của sản phẩm hoặc nhóm `Chưa phân loại` nếu sản phẩm không còn. Tên danh mục từ Catalog được escape khi xuất CSV để tránh công thức bảng tính.

Thông báo được lấy từ lịch sử trạng thái Order, các mốc xử lý trả hàng và chương trình khuyến mãi qua REST khi người dùng mở trang hoặc gọi API; một `event_key` duy nhất và khóa transaction tránh ghi trùng. Trả một phần có thông báo `RETURN_REFUNDED`; trả toàn bộ dùng thông báo đơn `RETURNED` để tránh trùng mốc cuối. Khuyến mãi chỉ nhập cho CUSTOMER đang bật đồng ý, từ chương trình còn hiệu lực được tạo/cập nhật/bắt đầu sau thời điểm đồng ý; tắt đồng ý chặn thông báo mới. Khuyến mãi liên kết tới sản phẩm hoặc danh mục, không giả lập order ID. Đây là đồng bộ theo yêu cầu, chưa có email/push chủ động. Khi Order, Auth hoặc Promotion không phản hồi, endpoint trả 503; thông báo đã lưu vẫn còn để đọc khi service hoạt động lại. Bộ tổng hợp hiện giới hạn 10.000 đơn và 10.000 chương trình cho mỗi lần đồng bộ.

Migration: Reporting V1 tạo thông báo, V3 cho phép thông báo khuyến mãi không có order ID và lưu `link_path`; Auth V2 lưu thời điểm đồng ý. Swagger UI: `http://localhost:8087/swagger-ui/index.html`. Smoke: `scripts/Smoke-ReportingNotification.ps1` và `scripts/Smoke-MarketingNotifications.ps1`.
