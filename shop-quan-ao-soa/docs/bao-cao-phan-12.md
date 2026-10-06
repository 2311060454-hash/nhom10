# Phần 12 — thông báo khuyến mãi theo sự đồng ý của khách

## Mục tiêu

Hồ sơ đã có lựa chọn đồng ý nhận thông tin khuyến mãi nhưng trước phần này chưa tạo thông báo tương ứng. Phần này nối lựa chọn đó với các chương trình còn hiệu lực. Mặc định tài khoản mới không đồng ý; khách có thể bật hoặc tắt trong hồ sơ.

## Thiết kế SOA

Auth Service sở hữu `marketing_consent` và `marketing_consent_at`. Tắt đồng ý xóa thời điểm; bật lại ghi thời điểm mới. Endpoint nội bộ chỉ xác nhận cho CUSTOMER đang hoạt động. Promotion Service sở hữu chương trình và cung cấp trang các chương trình đang hoạt động, chưa hết hạn, được tạo/cập nhật/bắt đầu sau thời điểm đồng ý. Reporting & Notification Service chỉ gọi REST, không đọc database của Auth hay Promotion.

Khi khách mở trang thông báo hoặc gọi số chưa đọc, Notification Service nhập chương trình phù hợp. Mã `promotion:{userId}:{promotionId}` cùng unique constraint ngăn trùng khi đồng bộ lại. Bản ghi liên kết đến sản phẩm hoặc danh mục; `order_id` để trống và `link_path` được lưu trong database Reporting. Khách khác không thể đọc hoặc đánh dấu đã đọc thông báo này. Sau khi tắt đồng ý, thông báo đã nhận vẫn ở lịch sử nhưng chương trình mới không được nhập. Chỉ thông báo trong ứng dụng được triển khai, chưa gửi email/push.

## Giới hạn

Đồng bộ diễn ra khi khách gọi API, chưa chủ động đẩy thông báo vào thời điểm chương trình bắt đầu. Mỗi chương trình chỉ tạo một thông báo cho một khách dù được chỉnh sửa nhiều lần. Nếu service nguồn không phản hồi, API thông báo trả 503; không xem đó là đã đồng bộ thành công. Chương trình hết hạn có thể vẫn hiện trong lịch sử, kèm thời gian kết thúc để khách phân biệt.
