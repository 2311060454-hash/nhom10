# Banner và nội dung cửa hàng

Catalog Service (8082) sở hữu hai bảng `banners` và `store_content` trong `shop_quan_ao_catalog`; Gateway định tuyến nguyên path `/api/catalog/**`. Flyway `V2__store_content.sql` tạo bảng và sáu khóa nội dung rỗng. ADMIN nhập nội dung thật trong giao diện; hệ thống không tự công bố địa chỉ, chính sách hoặc chương trình giảm giá không có dữ liệu.

| Method | URL | Đầu vào / đầu ra | Quyền |
| --- | --- | --- | --- |
| GET | `/api/catalog/banners` | Danh sách banner đang bật, `startsAt <= now < endsAt`, sắp theo `sortOrder,id` | Công khai |
| GET | `/api/catalog/banners/images/{imageId}` | Ảnh PNG đã xác thực và tái mã hóa | Công khai |
| GET | `/api/catalog/content` | Mảng `{key,text,updatedAt}` | Công khai |
| GET | `/api/catalog/manage/banners?page=0` | PageResult, 20 bản ghi/trang | ADMIN |
| POST | `/api/catalog/manage/banners` | BannerInput → BannerView, HTTP 201 | ADMIN |
| PUT | `/api/catalog/manage/banners/{id}` | BannerInput → BannerView; `active=false` ngừng hiển thị | ADMIN |
| POST | `/api/catalog/manage/banners/{id}/image` | multipart `file` JPEG/PNG ≤ 5 MB → BannerView | ADMIN |
| DELETE | `/api/catalog/manage/banners/{id}/image` | Xóa ảnh, giữ banner → BannerView | ADMIN |
| PUT | `/api/catalog/manage/content/{key}` | `{text}` ≤ 10.000 ký tự → ContentView | ADMIN |

`BannerInput`: `title` bắt buộc ≤160 ký tự, `subtitle` ≤320, `linkPath` chỉ `/` hoặc `/products/{id}`, `sortOrder` từ 0 đến 1000, `startsAt` và `endsAt` ISO UTC với bắt đầu trước kết thúc, `active` boolean. Trên trang chủ, `linkPath=/` cuộn tới phần `#collection` thay vì tải lại chính trang chủ; đường dẫn sản phẩm mở chi tiết tương ứng. `BannerView` còn có `id,imageUrl,createdAt,updatedAt`. Ảnh được kiểm tra nội dung thật, giới hạn 6 triệu pixel, tái mã hóa PNG; tên file UUID không nhận từ người dùng. Chỉ ảnh đang được tham chiếu trong database mới đọc được. Banner và ảnh riêng; lưu banner thành công nhưng upload lỗi vẫn còn banner để ADMIN sửa/tải lại.

Khóa nội dung hợp lệ: `ABOUT`, `CONTACT`, `PURCHASE_POLICY`, `RETURN_POLICY`, `SHIPPING_POLICY`, `ANNOUNCEMENT`. Trang công khai: `/about`, `/contact`, `/policy/purchase`, `/policy/return`, `/policy/shipping`. Thông báo có nội dung sẽ hiện trên đầu website. Khi nội dung trống, trang ghi rõ đang cập nhật. React xuất nội dung như văn bản, không render HTML do người dùng nhập.

ADMIN dùng `/admin/banners` và `/admin/store-content`. STAFF/CUSTOMER nhận 403 từ backend cho mọi thao tác quản lý; thiếu JWT nhận 401. Banner ngoài thời hạn hoặc tắt không xuất hiện ở trang chủ. Ảnh banner được hiển thị nguyên tỷ lệ, không cắt và không phủ thêm chữ HTML vì ảnh có thể đã chứa tiêu đề/nút trong thiết kế. Toàn ảnh dẫn tới `linkPath`; trên màn hình nhỏ có thêm chú thích và nút bên dưới ảnh để đọc dễ hơn. Banner không có ảnh hoặc ảnh tải lỗi dùng khối chữ và nút HTML. Sản phẩm đã ngừng bán có thể vẫn được chọn làm link banner; người quản trị cần kiểm tra link trước khi công bố.
