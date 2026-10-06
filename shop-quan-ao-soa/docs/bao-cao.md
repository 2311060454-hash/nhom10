# Báo cáo đồ án — nội dung phần 1 đã triển khai

Tài liệu lịch sử phần 1. Hiện trạng mới nhất và nội dung 5 chương cập nhật nằm trong [bao-cao-phan-2.md](bao-cao-phan-2.md).

**Tên đề tài:** Thiết kế và phát triển hệ thống quản lý shop quần áo theo kiến trúc hướng dịch vụ (SOA).

**Phạm vi bản báo cáo hiện tại:** thiết kế tổng thể và triển khai module Auth/User, Gateway cùng giao diện tài khoản. Không dùng tài liệu này để kết luận toàn bộ hệ thống bán hàng đã hoàn thành. Bằng chứng kiểm thử ở `verification.md` và `auth-smoke-result.json`.

## CHƯƠNG 1. TỔNG QUAN ĐỀ TÀI

### 1.1. Lý do chọn đề tài

Cửa hàng quần áo cần quản lý đồng thời sản phẩm có nhiều kích cỡ/màu, số lượng thực tế và giao dịch trực tuyến. Nếu dữ liệu tài khoản, hàng hóa và đơn hàng không thống nhất, cửa hàng có thể giao sai biến thể, nhận đơn vượt tồn hoặc không kiểm soát được quyền thao tác của nhân viên. Đề tài lựa chọn SOA để phân chia nghiệp vụ theo trách nhiệm rõ ràng và giao tiếp bằng hợp đồng REST.

### 1.2. Mục tiêu

Mục tiêu thiết kế là hệ thống phục vụ khách hàng, nhân viên và quản trị; backend xác thực và phân quyền thực tế; dữ liệu lưu MySQL; frontend tiếng Việt. Giai đoạn hiện tại hiện thực hóa nền tảng người dùng và cổng API, làm cơ sở cho các quy trình bán hàng tiếp theo.

### 1.3. Phạm vi và đối tượng

Hệ thống được xây dựng cho một shop trình diễn local trên Windows. Ba đối tượng sử dụng là CUSTOMER, STAFF và ADMIN. Phạm vi đã xây dựng: đăng ký, đăng nhập/đăng xuất, hồ sơ, địa chỉ, đổi/đặt lại mật khẩu bằng token local, quản lý tài khoản nhân viên/quyền/khóa, nhật ký hoạt động. Chưa triển khai giao dịch mua bán, thu tiền thực tế hoặc kết nối hãng vận chuyển.

## CHƯƠNG 2. CƠ SỞ LÝ THUYẾT

### 2.1. SOA và REST

SOA tổ chức khả năng nghiệp vụ thành dịch vụ độc lập, giao tiếp qua hợp đồng được công bố. Trong thiết kế này, mỗi dịch vụ sở hữu schema riêng; không đọc trực tiếp bảng của dịch vụ khác. REST dùng HTTP method, URL tài nguyên, JSON và status code để thể hiện yêu cầu/kết quả. Gateway là điểm truy cập của frontend, không sở hữu dữ liệu tài khoản.

Ranh giới dịch vụ giúp tách thay đổi nghiệp vụ nhưng làm mất transaction SQL toàn cục. Vì vậy luồng đặt hàng được thiết kế theo saga với thao tác giữ/commit/release kho idempotent; phần saga mới là thiết kế, chưa có kết quả chạy.

### 2.2. Công nghệ

Spring Boot 3 cung cấp cấu hình web, JPA và Spring Security. Mã nguồn dùng Java release 17. JPA ánh xạ entity vào bảng và thực hiện truy vấn tham số; Flyway quản lý phiên bản schema. MySQL Server lưu trữ dữ liệu; Workbench là giao diện quản trị và kiểm tra SQL.

React chia giao diện thành component, React Router điều hướng, Axios gửi REST qua Gateway, Bootstrap và CSS tạo bố cục responsive. Vitest/Testing Library kiểm tra thao tác biểu mẫu và guard giao diện.

### 2.3. Xác thực và phân quyền

Mật khẩu được BCrypt với cost 12. JWT ký HMAC-SHA256 mang subject, session ID, roles và thời hạn 30 phút. Phiên còn được lưu trong MySQL để có thể thu hồi trước hạn. Gateway gọi introspection và Auth tự kiểm tra phiên; kiểm tra role ở backend bằng Spring Security và `@PreAuthorize`. Tài nguyên địa chỉ truy vấn đồng thời ID và user ID để chống IDOR. Guard frontend chỉ hỗ trợ trải nghiệm, không thay thế backend.

Tài liệu nền: [Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [Spring Security](https://docs.spring.io/spring-security/reference/), [springdoc](https://springdoc.org/v2/).

## CHƯƠNG 3. PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG

### 3.1. Yêu cầu và tác nhân

Yêu cầu chức năng được chia theo ba vai trò trong `architecture/01-thiet-ke.md`. Yêu cầu phi chức năng gồm bảo vệ mật khẩu, phản hồi lỗi thống nhất, kiểm tra dữ liệu hai phía, schema có ràng buộc, hợp đồng API ổn định, timeout và phân biệt dịch vụ không khả dụng với danh sách rỗng.

### 3.2. Sơ đồ

Tệp `diagrams/so-do.md` chứa sơ đồ kiến trúc, Use Case tổng quát, Activity đặt hàng, Sequence giao tiếp, ERD theo miền và Class Diagram Auth. Các sơ đồ đặt hàng và bảng chưa thuộc Auth phản ánh thiết kế đích. Chúng chưa được sử dụng làm bằng chứng triển khai.

### 3.3. Dữ liệu và API đã xây dựng

Auth có users, roles, user_roles, addresses, auth_sessions, password_reset_tokens và audit_logs. Email unique ở database; địa chỉ và phiên có FK đến users; role có FK trong cùng database. Không có FK sang miền khác. Password reset lưu hash token, thời hạn và used; raw token chỉ có trong thư local.

Danh mục endpoint, DTO, quyền, mã lỗi nằm trong `api/auth.md`. Phân trang trả DTO PageResult để tránh phụ thuộc cấu trúc serialize nội bộ của Spring Data. DTO hồ sơ không trả password_hash. Trường ngoài DTO bị từ chối, không thể tự truyền role ADMIN khi đăng ký.

### 3.4. Giao diện đã xây dựng

Các trang thực tế gồm đăng nhập, đăng ký, quên/đặt lại mật khẩu, hồ sơ, địa chỉ, quản lý tài khoản và nhật ký. Quản trị viên có biểu mẫu tạo nhân viên, tìm/lọc/phân trang tài khoản, khóa và cấp quyền. Nhân viên có trang hồ sơ riêng được guard STAFF. Chưa có dashboard doanh thu, giỏ hàng hoặc trang thanh toán.

## CHƯƠNG 4. XÂY DỰNG VÀ TRIỂN KHAI

### 4.1. Tổ chức mã nguồn

Maven multi-module gồm common, auth-user-service và api-gateway. Auth chia controller, service, repository, DTO, entity; lỗi dữ liệu được map HTTP nhất quán. Common không có repository nghiệp vụ. Frontend nằm riêng và dùng Vite proxy gọi Gateway.

### 4.2. Triển khai Windows

Hướng dẫn đầy đủ ở README: cài JDK17/Maven/Node/MySQL; tạo connection localhost:3310 bằng Workbench; chạy SQL tạo database; tạo .env local; build; chạy Auth và Gateway; chạy Vite. Mật khẩu cấu hình qua môi trường, không nằm trong source. Seed chỉ bật profile local và không ghi đè tài khoản có sẵn.

### 4.3. Kết quả kiểm thử

Bộ JUnit/Spring Boot Test kiểm tra đăng ký, email trùng, BCrypt, chặn nâng quyền, thu hồi phiên, phân quyền, IDOR địa chỉ, reset token và API nội bộ. Gateway kiểm tra route không hợp lệ và traversal. Bộ smoke HTTP chạy Gateway → Auth → MySQL, gồm trường hợp dịch vụ Catalog chưa chạy trả 503. Frontend test dùng API mock để kiểm tra biểu mẫu, hiển thị lỗi, guard và VNĐ. Số liệu chính xác và runtime được ghi ở verification.md; không suy rộng kết quả này thành kiểm thử toàn hệ thống.

## CHƯƠNG 5. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

### 5.1. Kết quả đạt được

Đã có thiết kế SOA cố định về dịch vụ, cổng và quyền sở hữu dữ liệu; có module Auth/User và Gateway hoạt động với MySQL; giao diện tài khoản tiếng Việt kết nối REST; có migration, seed BCrypt, script Windows, tài liệu API và kiểm thử tự động.

### 5.2. Hạn chế

Toàn bộ nghiệp vụ sản phẩm/kho/đơn hàng/thanh toán/vận chuyển/khuyến mãi/đánh giá/báo cáo chưa triển khai. Chưa có SMTP, cơ chế saga, retry nghiệp vụ, upload ảnh hoặc test bán đồng thời. Gateway định tuyến local cố định, rate limiter nằm trong bộ nhớ, chưa có TLS hoặc hạ tầng production. Hiện không cho sửa/khóa tài khoản đã là ADMIN qua endpoint phân quyền để tránh mất quản trị; chức năng quản trị vòng đời ADMIN cần thiết kế bổ sung.

### 5.3. Hướng phát triển

Theo đúng thứ tự: Catalog và Inventory với khóa giữ kho; Order và saga bền vững; các dịch vụ thanh toán/vận chuyển, khuyến mãi/đánh giá, báo cáo/thông báo; giao diện bán hàng, quản trị nghiệp vụ, kiểm thử cạnh tranh và lỗi liên dịch vụ. Chỉ cập nhật mục kết quả đạt được sau khi có mã nguồn và bằng chứng test tương ứng.
