# Trả hàng sau khi đơn hoàn tất

Phạm vi hiện triển khai: trả toàn bộ hoặc **một phần đơn** trong 14 ngày kể từ trạng thái `COMPLETED`; chưa đổi sản phẩm hoặc gửi nhiều lần trả cho cùng một đơn. Order Service (8084) sở hữu `return_requests`, `return_items`, `return_status_history` và `orders.returned_amount` trong `shop_quan_ao_order` (Flyway V4/V5). Payment/Shipping Service (8085) sở hữu `payments`, `shipments`, `refunds` (Flyway V2). Inventory Service sở hữu `partial_return_restocks` (Flyway V2). Các service chỉ trao đổi qua REST, không có FK xuyên database.

| Method | URL qua Gateway | Dữ liệu / kết quả | Quyền |
| --- | --- | --- | --- |
| GET | `/api/orders/{id}/return` | ReturnView và lịch sử | Chủ đơn, STAFF, ADMIN |
| GET | `/api/orders/returns/manage?state=REQUESTED&page=0` | Danh sách phân trang 20 yêu cầu, lọc trạng thái | STAFF, ADMIN |
| POST | `/api/orders/{id}/return` | Header `Idempotency-Key`, body `{reason}` để trả toàn bộ hoặc `{reason,items:[{variantId,quantity}]}` để trả một phần; HTTP 201 | CUSTOMER là chủ đơn |
| POST | `/api/orders/{id}/return/decision` | `{action: APPROVE\|REJECT,note}` | STAFF, ADMIN |
| POST | `/api/orders/{id}/return/receive` | `{note}` — xác nhận đã nhận hàng thực tế | STAFF, ADMIN |
| POST | `/api/orders/{id}/return/confirm-refund` | `{reference}` — mã chứng từ hoàn COD sau khi hoàn tiền thật | ADMIN |

Yêu cầu chỉ tạo khi đơn `COMPLETED`, thanh toán `PAID` hoặc `SIMULATED_PAID`, không có lệnh giao hàng đang đối soát. Người khác nhận 404; STAFF không thể tạo yêu cầu thay khách. Mỗi đơn có tối đa một yêu cầu; cùng key, lý do và danh sách hàng trả kết quả cũ, dữ liệu khác trả 409. Trả một phần phải chọn biến thể thuộc đơn, số lượng không vượt số đã mua và còn ít nhất một món giữ lại. Trạng thái `REQUESTED → APPROVED → RECEIVED → REFUND_PENDING → REFUND_CONFIRMING → REFUNDED` cho COD, hoặc `REQUESTED → APPROVED → RECEIVED → REFUNDED` cho mô phỏng. `REQUESTED → REJECTED` là nhánh từ chối. Lịch sử ghi người thao tác, ghi chú và thời gian.

Worker Order lấy `order_guard` trước khi đối soát, dùng mã lệnh cố định ở Payment. Payment dùng guard, bảng `payment_commands` và `refunds.order_id UNIQUE` để chống hoàn lặp. Khi nhận hàng, Payment tạo refund: `PENDING_MANUAL` cho COD hoặc `SIMULATED_REFUNDED` cho mô phỏng; sau đó Inventory restock đúng một lần. Trả toàn bộ dùng reservation của đơn và chuyển Order sang `RETURNED`. Trả một phần dùng return ID riêng tại Inventory, giữ đơn `COMPLETED`, thanh toán `PAID` hoặc `SIMULATED_PAID` và vận chuyển `DELIVERED` cho phần hàng khách giữ. Nếu timeout, yêu cầu vẫn ở trạng thái đối soát, worker thử lại với cùng mã lệnh. COD chờ ADMIN nhập chứng từ sau khi thực sự hoàn tiền; mô phỏng không chuyển tiền thật.

Báo cáo doanh thu tính COD `COMPLETED` và `PAID`, trừ `orders.returned_amount` sau khi hoàn một phần được xác nhận. Số lượng bán chạy trừ số món đã trả một phần. COD chờ hoàn thực tế vẫn được tính đủ cho đến khi ghi chứng từ; trả toàn bộ chuyển `RETURNED` và bị loại. Refund toàn bộ gồm phí giao hàng; refund một phần lấy giá snapshot của dòng trả, phân bổ giảm giá đơn theo tỷ lệ giá hàng và không gồm phí giao hàng. Việc bấm xác nhận hoàn COD là thao tác ghi nhận thủ công, không tích hợp ngân hàng; chỉ thực hiện sau khi đã chuyển tiền cho khách. Báo cáo gắn phần tiền hoàn với kỳ hoàn tất của đơn và phản ánh số thuần hiện tại, chưa có báo cáo dòng tiền theo ngày hoàn thực tế.

Reporting & Notification Service nhận lịch sử Order và lịch sử trả hàng qua REST khi khách mở `/notifications` hoặc gọi `/api/notifications/unread-count`. Khách nhận thông báo khi yêu cầu được tạo, duyệt/từ chối, cửa hàng nhận hàng, chờ hoàn COD và khi hoàn một phần xong. Trả toàn bộ tiếp tục dùng thông báo đơn `RETURNED` khi hoàn tất để tránh hai thông báo cuối trùng nội dung. Khóa sự kiện theo return ID và event ID ngăn nhập trùng; chỉ chủ tài khoản xem hoặc đánh dấu đã đọc. Đồng bộ theo yêu cầu, chưa có email/push.

Swagger: [Order 8084](http://localhost:8084/swagger-ui/index.html), [Payment 8085](http://localhost:8085/swagger-ui/index.html). Khách thao tác tại `/orders/{id}`; STAFF/ADMIN mở hàng đợi `/manage/returns` rồi vào chi tiết đơn để xử lý.
