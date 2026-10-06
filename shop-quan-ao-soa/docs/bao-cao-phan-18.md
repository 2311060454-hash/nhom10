# Phần 18 — Doanh thu theo danh mục

## Mục tiêu và kết quả

ADMIN nay xem doanh thu hàng hóa theo danh mục tại `/admin/category-revenue`, lọc theo ngày hoàn tất và xuất CSV. Báo cáo tách phí vận chuyển để tổng hàng hóa cộng phí giao bằng doanh thu COD đã thu sau hoàn tiền.

## Thiết kế SOA và cách tính

Order Service cung cấp giá/số lượng đã chốt khi đặt, số lượng đã trả, khoản hoàn và phí giao trong endpoint báo cáo nội bộ. Catalog Service cung cấp ánh xạ sản phẩm đến danh mục và tên danh mục qua REST nội bộ. Reporting Service chỉ tổng hợp, không truy cập chéo schema. Chỉ đơn `COMPLETED` + `COD` + `PAID` được tính; đơn mô phỏng, hủy và trả toàn bộ không được tính. Với trả một phần, số lượng còn lại của từng sản phẩm làm trọng số phân bổ doanh thu hàng hóa sau giảm giá và hoàn tiền. Chênh lệch làm tròn được gán vào danh mục cuối để tổng không sai lệch. Phí vận chuyển được báo riêng. Tên danh mục được escape khi ghi CSV để tránh công thức bảng tính.

## Kiểm thử và giới hạn

JUnit kiểm tra ánh xạ Catalog, phân bổ giảm giá, hoàn một phần, phí giao, loại giao dịch mô phỏng, CSV và quyền ADMIN. Vitest kiểm tra bảng và bộ lọc. [Smoke qua Gateway/MySQL](category-revenue-smoke-result.json) đạt 9/9; trên dữ liệu hiện có, tổng danh mục cộng phí giao khớp **1.250.000 ₫** doanh thu COD của báo cáo thời gian. Kết quả kiểm thử ở thời điểm hoàn thành phần 18 là 109/109 JUnit và 38/38 Vitest. Sau đó Order Flyway V6 bổ sung category ID snapshot cho đơn mới, tránh phân loại lại khi sản phẩm đổi danh mục. Đơn cũ có `category_id` NULL vẫn dùng danh mục hiện tại; báo cáo giới hạn tổng hợp 10.000 đơn và chưa có phân cấp doanh thu theo danh mục cha.
