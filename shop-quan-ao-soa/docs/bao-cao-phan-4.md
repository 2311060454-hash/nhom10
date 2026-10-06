# Báo cáo bổ sung phần 4 — Thanh toán và vận chuyển

## Chương 1: Tổng quan

Mục tiêu phần này là hoàn thiện luồng đơn hàng từ đóng gói đến giao, thu COD và hoàn tất; đồng thời có thanh toán mô phỏng rõ ràng phục vụ trình diễn local. Chức năng bổ sung gồm ghi mã vận đơn, hãng, người phụ trách và chi phí vận chuyển, giao thất bại, giao lại, nhận hàng hoàn và hoàn mô phỏng. Không có giao dịch ngân hàng thật.

## Chương 2: Cơ sở lý thuyết áp dụng

Payment/Shipping là một service nghiệp vụ độc lập, REST tại 8085, dữ liệu riêng shop_quan_ao_payment. Order điều phối yêu cầu bằng bảng command bền vững; Payment lưu kết quả từng command trong cùng transaction với biến động. Idempotency bảo vệ thao tác khi response bị mất. Các bản sao paymentState/shippingState ở Order được cập nhật sau phản hồi, thể hiện nhất quán cuối cùng chứ không phải một transaction xuyên database.

## Chương 3: Phân tích và thiết kế

Khách là chủ đơn được chọn kết quả mô phỏng trong hạn 15 phút. ADMIN/STAFF được xử lý vận chuyển và ghi nhận thu đủ COD sau giao thành công. Quyền được kiểm tra tại Order; API đọc Payment còn kiểm tra userId từ dữ liệu thuộc chính service. Endpoint ghi Payment là nội bộ có X-Internal-Key, Gateway không công khai.

Các sơ đồ, DTO, bảng API, trạng thái và quyền sở hữu dữ liệu nằm ở [Payment/Shipping API](api/payment-shipping.md). Payment sở hữu payments, shipments, fulfillment_events, payment_commands, refunds và payment_guard. Order bổ sung fulfillment_commands và pending_command_id. Foreign key chỉ nằm trong cùng schema.

Khi có command chưa xác định kết quả, Order từ chối thao tác mới có thể xung đột. Khi giao thất bại, chỉ xác nhận nhận lại hàng thực tế mới bắt đầu hoàn kho. Không coi phản hồi 202 là đã thanh toán hoặc giao thành công.

## Chương 4: Xây dựng và kiểm thử

Payment chạy độc lập bằng Spring Boot 3/JPA/Flyway. Start-Local chạy Auth → Catalog → Inventory → Payment/Shipping → Order → Gateway. Migration Order V2 không sửa tệp V1 đã chạy. MySQL đã thực thi migration và Hibernate validate thành công trong lần chạy thực tế ngày 24/09/2026.

Giao diện checkout có COD và simulation local. Chi tiết đơn hiển thị trạng thái từ API, các biểu mẫu vận chuyển và chứng từ thu COD theo vai trò; có xác nhận thao tác, trạng thái pending, lỗi và retry giữ key. Dữ liệu simulation luôn được gắn nhãn không thu/hoàn tiền thật.

Kết quả kiểm chứng hiện tại:

- 18 kiểm thử Order và 10 kiểm thử Payment/Shipping đạt với JUnit, Spring Boot Test, H2 và Mockito.
- 16 kiểm thử frontend Vitest đạt; Vite production build thành công.
- 94 kiểm tra Payment/Shipping qua Gateway/MySQL thật đạt; gồm COD hoàn tất, simulation thành công/thất bại, hoàn simulation đúng một lần, giao thất bại/giao lại/hàng hoàn, kiểm tra chủ sở hữu và tồn cuối.
- Chạy lại 29 kiểm tra giỏ hàng/đơn hàng phần 3 qua MySQL: đạt.
- Lỗi dependency và response không xác định được kiểm bằng mock ở backend; chưa thực hiện fault-injection mạng thật hoặc chứng minh mọi trường hợp mất điện/phân vùng mạng.
- Chưa có browser E2E toàn bộ checkout/fulfillment. Không gọi test frontend mock hoặc HTTP smoke là browser E2E.

Bằng chứng ở payment-shipping-smoke-result.json, orders-smoke-result.json và target/surefire-reports. Test tạo sản phẩm/khách riêng, giữ đơn và nhật ký thật trong database demo, ngừng bán sản phẩm test khi thành công. Hai đơn hoàn tất trong smoke sử dụng dữ liệu thử nghiệm; chứng từ COD có tiền tố TEST, không chứng minh đã thu tiền từ người thật.

Build target Java 17; các kiểm thử thực tế hiện chạy trên JVM 25.0.4 của máy. Chưa chạy riêng JVM 17.

## Chương 5: Kết luận và giới hạn

Đã nối Order, Inventory và Payment/Shipping thành luồng local đến hoàn tất. Phân biệt được PAID thủ công của COD với SIMULATED_PAID/SIMULATED_REFUNDED. Chưa tích hợp VNPay/MoMo hoặc hãng vận chuyển; chưa có hoàn COD thật, trả hàng sau hoàn tất, hoàn từng phần, mã giảm giá, đánh giá, báo cáo và thông báo. Assignee là tên ghi nhận, chưa ràng buộc tài khoản nhân viên.

Giải pháp khóa một dòng guard cho mỗi service làm giảm thông lượng; worker retry theo chu kỳ cố định chưa có dead-letter/circuit breaker. Cần thêm các cơ chế này, kiểm thử JVM 17 và fault-injection trước khi mô tả hệ thống là sẵn sàng triển khai thực tế ngoài môi trường đồ án.
