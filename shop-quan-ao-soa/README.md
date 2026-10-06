# Hệ thống Quản lý Shop Quần Áo theo Kiến trúc SOA — Hoàn chỉnh 100%

Hệ thống hoàn chỉnh phục vụ đề tài tốt nghiệp / đồ án môn học: **THIẾT KẾ VÀ PHÁT TRIỂN HỆ THỐNG QUẢN LÝ SHOP QUẦN ÁO THEO KIẾN TRÚC HƯỚNG DỊCH VỤ (SOA)**. Bao gồm toàn bộ 8 Microservices Spring Boot 3 + API Gateway, 7 CSDL MySQL phân tán, Frontend React 18 + Vite hiện đại, đầy đủ luồng Khách hàng, Nhân viên, Quản trị viên và tài liệu tốt nghiệp 5 chương chi tiết.

- [**BÁO CÁO ĐỒ ÁN TỐT NGHIỆP HOÀN CHỈNH (5 CHƯƠNG)**](docs/BAO-CAO-DO-AN-HOAN-CHINH.md)


- [Kiến trúc, phân vai, danh sách service](docs/architecture/01-thiet-ke.md)
- [Sơ đồ SOA, Use Case, ERD, Sequence, Activity, Class](docs/diagrams/so-do.md)
- [Thiết kế dữ liệu các module](docs/architecture/02-data-dictionary.md)
- [API đã triển khai](docs/api/auth.md)
- [API Catalog và Inventory, hợp đồng giữ kho](docs/api/catalog-inventory.md)
- [Trạng thái triển khai và bằng chứng kiểm thử](docs/verification.md)
- [API Giỏ hàng và Order](docs/api/cart-order.md)
- [API Payment/Shipping và sơ đồ xử lý](docs/api/payment-shipping.md)
- [API Promotion/Review](docs/api/promotion-review.md)
- [Báo cáo cập nhật phần 5](docs/bao-cao-phan-5.md)
- [API Reporting/Notification](docs/api/reporting-notification.md)
- [Báo cáo cập nhật phần 6](docs/bao-cao-phan-6.md)
- [API Customer Support](docs/api/customer-support.md)
- [Báo cáo cập nhật phần 7](docs/bao-cao-phan-7.md)
- [API Banner và nội dung cửa hàng](docs/api/store-content.md)
- [Báo cáo cập nhật phần 8](docs/bao-cao-phan-8.md)
- [API trả hàng sau hoàn tất](docs/api/returns.md)
- [Báo cáo cập nhật phần 9](docs/bao-cao-phan-9.md)
- [Báo cáo cập nhật phần 10](docs/bao-cao-phan-10.md)
- [Báo cáo cập nhật phần 11](docs/bao-cao-phan-11.md)
- [Báo cáo cập nhật phần 12](docs/bao-cao-phan-12.md)

## 1. Cài công cụ trên Windows

Cài **JDK 17**, đặt JAVA_HOME đến thư mục JDK, thêm `%JAVA_HOME%\bin` vào PATH. Cài Maven 3.9.x, thêm thư mục bin vào PATH. Cài Node.js 22.12+ hoặc Node.js 24 LTS, MySQL Server 8.0 và MySQL Workbench. Dùng PowerShell 7 cho các script (Smoke-Auth dùng SkipHttpErrorCheck). IntelliJ IDEA/Eclipse mở `backend/pom.xml`; chọn Project SDK 17. VS Code mở `frontend`.

```powershell
java -version
mvn.cmd -version
node --version
npm.cmd --version
```

Backend khóa Spring Boot 3.5.16 và biên dịch `--release 17`. Không sử dụng dự án mẫu Spring Boot 4 nằm ở thư mục ngoài.

## 2. MySQL Server và Workbench

Workbench chỉ là công cụ quản lý, không thay thế Server. Trong Workbench tạo Standard TCP/IP connection: Host `localhost`, Port `3310`, Username `root`; nhập mật khẩu local bạn đã cung cấp vào Store in Vault rồi Test Connection.

```powershell
Get-Service *mysql*
Test-NetConnection localhost -Port 3310
Get-NetTCPConnection -LocalPort 3310 -State Listen
```

Nếu Server đang chạy cổng khác, chỉnh `port=3310` trong cấu hình `my.ini` của đúng dịch vụ MySQL rồi khởi động lại dịch vụ bằng quyền quản trị. Không chỉ thay cổng trong Workbench.

Mở `database/00-create-databases.sql` trong Workbench và Execute. Script tạo đủ 7 database theo thiết kế. Các service tự chạy migration Flyway, gồm Catalog V2 cho banner/nội dung, Order V4/V5, Inventory V2 và Payment V2 cho trả hàng, Auth V2 và Reporting V3 cho thông báo khuyến mãi. Không chạy lại migration thủ công: Flyway tự quản lý lịch sử và nâng cấp khi khởi động service.

## 3. Cấu hình local

Đứng tại thư mục `shop-quan-ao-soa`:

```powershell
.\scripts\Init-Local.ps1
```

Nhập mật khẩu MySQL đã yêu cầu và tự chọn mật khẩu mẫu ít nhất 10 ký tự. Script tạo `.env`, sinh JWT_SECRET và INTERNAL_API_KEY ngẫu nhiên, không in bí mật ra màn hình. `.env` được gitignore. `.env.example` để trống mật khẩu nhằm tuân thủ yêu cầu không đưa bí mật vào repository. Không dùng mật khẩu DB làm khóa JWT.

Trong phiên làm việc này `.env` đã được tạo sẵn: không chạy Init-Local lần nữa; mở file trên máy để lấy `SEED_PASSWORD`. Đó là mật khẩu ngẫu nhiên dùng cho cả ba tài khoản mẫu. Không copy giá trị này vào báo cáo hoặc commit Git.

Để chạy từng service từ IDE, đặt biến môi trường theo `.env`, active profile `local`, working directory là `shop-quan-ao-soa`. Spring Boot không tự đọc file `.env`; script Load-Env.ps1 làm việc đó khi chạy bằng PowerShell.

## 4. Build và chạy

```powershell
.\scripts\Build.ps1
.\scripts\Start-Local.ps1
```

Build chạy Maven `clean verify`, npm ci, test frontend và production build. Có thể truyền `-Maven 'C:\tools\apache-maven\bin\mvn.cmd'`. Start chạy Auth 8081 → Catalog 8082 → Inventory 8083 → Payment/Shipping 8085 → Promotion/Review 8086 → Order 8084 → Reporting/Notification 8087 → Gateway 8080, chờ health UP, ghi log trong `.runtime`. Profile local chạy Flyway và seed; không đổi mật khẩu tài khoản đã có. Dừng backend trước khi build vì Windows khóa JAR đang chạy.

Mở terminal khác:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Mở [bộ sưu tập](http://localhost:5173). Vite proxy `/api` đến Gateway 8080. Frontend không gọi MySQL trực tiếp. CORS_ORIGIN trong .env hỗ trợ `http://localhost:5173,http://127.0.0.1:5173`; nếu dùng .env cũ, bổ sung địa chỉ thứ hai và khởi động lại backend.

Phiên hiện tại đã bật Vite tại `http://127.0.0.1:5173` bằng process nền ghi ở `.runtime/frontend-process.json`. Khi muốn dừng riêng giao diện, chạy `.\scripts\Stop-Frontend.ps1`; với lần chạy thủ công bằng `npm.cmd run dev`, dùng Ctrl+C trong terminal đó.

| Tài khoản | Vai trò | Mật khẩu |
|---|---|---|
| admin@shop.local | ADMIN | SEED_PASSWORD trong .env local |
| staff@shop.local | STAFF, có cờ quyền kho | SEED_PASSWORD trong .env local |
| customer@shop.local | CUSTOMER | SEED_PASSWORD trong .env local |

Khách xem `/` và `/products/{id}`, quản lý tài khoản tại `/account`, `/addresses`. Admin vào `/admin/dashboard`, `/admin/reports`, `/admin/products`, `/admin/catalog`, `/inventory`, `/admin/users`, `/admin/audit`. Nhân viên có `/staff/profile` và `/inventory`; nhập/xuất/kiểm kê cần quyền inventoryWrite. Các trang dùng API thật, có loading, lỗi, danh sách rỗng và xác nhận thao tác quan trọng.

Catalog seed lần đầu tạo 6 sản phẩm, mỗi sản phẩm 4 size × 2 màu, tổng 48 biến thể. Inventory gọi REST Catalog để nhập 30 đơn vị/biến thể mẫu khi kho rỗng. Seed cơ bản không kèm ảnh; UI có ảnh dự phòng khi chưa có ảnh. Admin sửa sản phẩm và tải JPEG/PNG ở phần Album ảnh, có xem trước. Bộ dữ liệu demo local hiện có 12 ảnh sản phẩm kiểu chụp studio trong `assets/product-photography/`. Chạy `pwsh -NoProfile -File scripts/Replace-DemoProductImages.ps1` sau khi các service đã sẵn sàng để tải ảnh qua API và thay album minh họa cũ cho đúng 12 sản phẩm mẫu; script kiểm tra tên sản phẩm trước khi thay để tránh sửa nhầm ID. Đây là ảnh minh họa **tạo bằng AI**, không phải ảnh của hàng tồn thực tế. Khi bán thật, ADMIN cần thay bằng ảnh chụp chính sản phẩm. Nếu seed bị gián đoạn, dùng giao diện quản trị bổ sung dữ liệu; không xóa database để seed lại.

Swagger bổ sung: [Catalog](http://localhost:8082/swagger-ui/index.html), [Inventory](http://localhost:8083/swagger-ui/index.html). Nội bộ giữ kho dùng X-Internal-Key trực tiếp cổng 8083, không công khai qua Gateway.

Swagger: [Auth](http://localhost:8081/swagger-ui/index.html). Thực hiện login, copy accessToken, bấm Authorize với token, gọi API được cấp quyền. Health: `http://localhost:8081/actuator/health`, `http://localhost:8080/actuator/health`. Gateway không chứa bản sao OpenAPI của Auth; dùng Swagger riêng từng service.

Quên mật khẩu: nhập email tại `/forgot-password`. Mở tệp mới nhất trong `.runtime/mail` trên máy chạy Auth, lấy token và nhập tại `/reset-password`. Token hết hạn sau 15 phút, không gửi qua response, không ghi log. Đây là **hộp thư local**, chưa tích hợp SMTP. Không đưa adapter này lên hệ thống production.

## 5. Kiểm thử và dừng

```powershell
mvn.cmd -f backend/pom.xml verify
cd frontend
npm.cmd test
npm.cmd run build
cd ..
.\scripts\Smoke-Auth.ps1
.\scripts\Smoke-CatalogInventory.ps1
.\scripts\Smoke-Orders.ps1
.\scripts\Smoke-PaymentShipping.ps1
.\scripts\Smoke-PromotionReview.ps1
.\scripts\Smoke-ReportingNotification.ps1
.\scripts\Smoke-Support.ps1
.\scripts\Stop-Local.ps1
```

Smoke yêu cầu các service tương ứng đang chạy, dùng database thật, tạo một khách kiểm thử rồi khóa tài khoản đó ở cuối; không xóa dữ liệu có sẵn. Báo cáo không chứa token hoặc mật khẩu. JUnit dùng H2 để độc lập và test Spring Security; smoke HTTP bổ sung bằng chứng MySQL. Npm test dùng Vitest/Testing Library và API mock, không thay thế kiểm thử browser/HTTP.

Stop chỉ dừng PID do Start ghi, kiểm tra tên process và thời gian khởi động để tránh dừng process bị tái sử dụng PID. Frontend dừng bằng Ctrl+C trong terminal chạy Vite.

## 6. Xử lý lỗi

| Lỗi | Cách kiểm tra |
|---|---|
| Access denied for root@localhost | Test Connection Workbench với cùng host/port/user/password; sửa DB_PASSWORD trong .env; chạy Load-Env lại hoặc restart service |
| Unknown database | Chạy 00-create-databases.sql trên đúng Server 3310; tên Auth là shop_quan_ao_auth |
| Communications link failure / port 3310 | Kiểm tra dịch vụ MySQL, Test-NetConnection và my.ini; bảo đảm server đang listen 3310 |
| Thiếu driver | Build bằng Maven, không chạy class đơn lẻ; mysql-connector-j nằm trong jar Spring Boot |
| Sai datasource / thiếu placeholder | Spring Boot không tự đọc .env; dùng Start-Local hoặc khai báo biến trong IDE; kiểm tra URL và JAVA_HOME |
| Flyway non-empty schema / checksum mismatch | Không sửa migration đã chạy hoặc chạy thủ công V1; kiểm tra lịch sử. Không xóa database thật để chữa lỗi |
| CORS | Dùng localhost:5173 với Vite proxy; khi gọi trực tiếp Gateway, CORS_ORIGIN phải khớp chính xác origin |
| 401 | JWT hết hạn/thu hồi, tài khoản bị khóa hoặc chưa đăng nhập; đăng nhập lại |
| 403 | Tài khoản thiếu vai trò; STAFF không được gọi admin/users; backend luôn kiểm tra quyền |
| 404 | Sai path hoặc tài nguyên không thuộc tài khoản; địa chỉ người khác trả 404 |
| 429 | Hạn chế tần suất Auth local; đợi một phút. Bộ đếm hiện nằm trong bộ nhớ mỗi instance |
| 503 | Kiểm tra health/log của dịch vụ đích; module chưa triển khai cũng trả 503, không phải dữ liệu rỗng giả |
| Cổng 8080/8081/5173 đã dùng | Dừng đúng process trước; không tự đổi cổng vì frontend và gateway dùng hợp đồng cố định |
| Maven không được nhận dạng | Cài Maven và thêm bin vào PATH hoặc truyền -Maven cho Build.ps1 |
| npm.ps1 bị ExecutionPolicy | Dùng npm.cmd; không cần tắt chính sách bảo mật toàn hệ thống |

## 7. Cấu trúc mã nguồn hiện tại

```text
backend/
  pom.xml
  common/
  api-gateway/
  auth-user-service/
  catalog-service/
  inventory-service/
  order-service/
  payment-shipping-service/
  promotion-review-service/
  reporting-notification-service/
frontend/
  src/App.jsx
  src/CatalogPages.jsx
  src/OrderPages.jsx
  src/PromotionPages.jsx
  src/ReportingPages.jsx
  src/StorePages.jsx
database/00-create-databases.sql
docs/architecture/
docs/diagrams/
docs/api/
scripts/
```

Danh mục tên và đường dẫn từng file ở [docs/file-manifest.txt](docs/file-manifest.txt); mã nguồn đầy đủ nằm trong các file thật. Mỗi service sở hữu schema/migration riêng, controller và logic nghiệp vụ của mình; các API nội bộ trao đổi qua REST. Smoke test tạo dữ liệu thử nghiệm thật và giữ lịch sử để đối chiếu, không xóa database để làm sạch kết quả.

Phần tiếp theo: trả một phần/đổi sản phẩm và hoàn thiện những yêu cầu còn lại. Không đổi tên database hoặc cổng trong các phần tiếp theo.

## 8. Trình diễn đặt hàng, thanh toán và giao hàng

- Khách đăng nhập, chọn biến thể và lưu giỏ tại trang sản phẩm; mở `/cart`, cập nhật số lượng, rồi `/checkout`.
- Chọn COD hoặc **Mô phỏng local — không thu tiền thật**. Nhập/chọn địa chỉ và xác nhận. Trang `/orders/:id` hiển thị trạng thái thực sau khi worker xử lý.
- Với simulation, chủ đơn chọn thành công/thất bại trong 15 phút. Không dùng thẻ, tài khoản hoặc khóa API thật.
- ADMIN/STAFF mở `/manage/orders`, vào chi tiết, xác nhận rồi đóng gói. Nhập đơn vị, mã vận đơn, người phụ trách và chi phí hãng để bàn giao.
- Ghi giao thành công. COD còn chờ thu: nhập mã chứng từ, chỉ xác nhận khi đã thu đủ tiền. Đơn simulation hoàn tất vẫn mang trạng thái mô phỏng.
- Nếu giao thất bại, nhập lý do rồi giao lại hoặc xác nhận đã nhận hàng hoàn. Sau khi kho và thanh toán phản hồi, đơn mới kết thúc hủy.
- Nếu thấy yêu cầu đang đối soát, chờ service phục hồi; không tạo yêu cầu mới để vượt qua trạng thái này. Nút kiểm tra lại dùng mã cũ.

Swagger mới: [Order 8084](http://localhost:8084/swagger-ui/index.html), [Payment/Shipping 8085](http://localhost:8085/swagger-ui/index.html).
Mã nguồn bổ sung: `backend/order-service`, `backend/payment-shipping-service`, `frontend/src/OrderPages.jsx`, `frontend/src/FulfillmentPanel.jsx`. Danh sách file: `docs/file-manifest.txt`.
Smoke PaymentShipping giữ 5 đơn thử nghiệm cùng nhật ký và ngừng bán sản phẩm test. Chứng từ COD TEST chỉ là dữ liệu demo. Không xóa lịch sử kho để dọn test.

## 9. Khuyến mãi, mã giảm giá và đánh giá

ADMIN vào `/admin/coupons` và `/admin/promotions` để tạo chương trình hiệu lực theo thời gian. Khách thấy giá giảm tại `/products/:id`, nhập coupon ở `/checkout`, lưu sản phẩm tại `/favorites`, xem đánh giá cá nhân ở `/my-reviews`. Chỉ đơn `COMPLETED` mới được gửi đánh giá; ADMIN duyệt ở `/admin/reviews`. Checkout ghi giá và mã giảm vào snapshot đơn; hủy/đặt thất bại giải phóng lượt coupon. Bài smoke `Smoke-PromotionReview.ps1` kiểm tra 34 tình huống qua Gateway/MySQL. Swagger: [Promotion/Review 8086](http://localhost:8086/swagger-ui/index.html). Hợp đồng API và giới hạn có trong [tài liệu Promotion/Review](docs/api/promotion-review.md).

## 10. Dashboard, báo cáo và thông báo

ADMIN xem `/admin/dashboard`, lọc/xuất CSV tại `/admin/reports`. Khách, nhân viên và quản trị viên xem thông báo đơn của chính mình tại `/notifications`; trạng thái đơn được đồng bộ khi mở trang. Doanh thu chỉ tính COD hoàn tất đã thu tiền; giao dịch mô phỏng hiển thị riêng. Smoke `Smoke-ReportingNotification.ps1` đạt 23 kiểm tra qua MySQL/Gateway. Swagger: [Reporting 8087](http://localhost:8087/swagger-ui/index.html). Xem [hợp đồng báo cáo và thông báo](docs/api/reporting-notification.md) để biết quy tắc tính và giới hạn 10.000 đơn mỗi yêu cầu.

## 11. Gửi và xử lý yêu cầu hỗ trợ

Khách đã đăng nhập mở `/support`, điền chủ đề/nội dung rồi theo dõi trao đổi ở `/support/:id`. STAFF/ADMIN mở `/manage/support` để lọc, tiếp nhận, phản hồi và giải quyết; khách có thể đóng ticket đã giải quyết. Mọi tin nhắn và lịch sử trạng thái lưu trong MySQL, kiểm tra chủ sở hữu tại backend. `Smoke-Support.ps1` đạt 25 kiểm tra qua Gateway/MySQL. Hợp đồng REST và luồng trạng thái ở [Customer Support API](docs/api/customer-support.md).

## 12. Banner và nội dung cửa hàng

ADMIN mở `/admin/banners` để tạo, sửa, đặt thời gian hiệu lực, bật/tắt và tải ảnh banner JPEG/PNG. Banner đang hiệu lực hiện trên trang chủ từ MySQL; khi chưa có banner, trang chủ dùng khối giới thiệu mặc định. ADMIN mở `/admin/store-content` để lưu giới thiệu, liên hệ, chính sách mua hàng/đổi trả/vận chuyển và thông báo đầu trang. Khách đọc ở footer qua `/about`, `/contact`, `/policy/purchase`, `/policy/return`, `/policy/shipping`.

`scripts/Smoke-StoreContent.ps1` đạt 18 kiểm tra qua Gateway/MySQL, khôi phục nội dung liên hệ và tắt banner thử nghiệm sau khi chạy. Catalog dùng Flyway V2; [hợp đồng API](docs/api/store-content.md) mô tả DTO, quyền và giới hạn ảnh. Backend `mvn clean verify` đạt 84/84 JUnit; frontend Vitest 24/24 và Vite production build thành công trong lần cập nhật này.

## 13. Trả hàng sau khi đơn hoàn tất

Khách mở chi tiết đơn `/orders/{id}` và gửi lý do trả toàn bộ đơn trong 14 ngày sau khi hoàn tất. STAFF/ADMIN mở hàng đợi `/manage/returns` để lọc yêu cầu, duyệt hoặc từ chối, rồi xác nhận đã nhận hàng; Order đối soát Payment và Inventory, hoàn kho đúng một lần. Giao dịch mô phỏng được ghi hoàn mô phỏng; COD giữ trạng thái chờ hoàn đến khi ADMIN đã thực hiện hoàn tiền ngoài hệ thống và nhập mã chứng từ. Sau đó đơn chuyển `RETURNED` và được loại khỏi doanh thu. Mã chứng từ TEST trong smoke chỉ dành cho đơn demo, không phải bằng chứng chuyển khoản.

Order Flyway V4 và Payment Flyway V2 tự chạy khi service khởi động. [Hợp đồng REST và trạng thái](docs/api/returns.md) mô tả quyền, idempotency, worker và giới hạn. `scripts/Smoke-Returns.ps1` đạt **54/54 kiểm tra** qua Gateway/MySQL. Toàn backend `mvn clean verify` đạt **93/93 JUnit**; frontend Vitest **27/27** và Vite build production thành công.

## 14. Trả hàng một phần

Khách chọn “Một phần đơn” tại chi tiết đơn đã hoàn tất, nhập số lượng cho từng biến thể và lý do. Backend chặn biến thể không thuộc đơn, số lượng vượt số đã mua hoặc chọn hết toàn bộ đơn trong chế độ này. Tiền hoàn dùng giá snapshot và phân bổ giảm giá theo tỷ lệ, không hoàn phí vận chuyển. STAFF/ADMIN duyệt và xác nhận nhận hàng như quy trình trả toàn bộ. Inventory chỉ cộng lại đúng những món được chọn theo return ID; Payment chỉ tạo một refund cho đơn. COD chờ ADMIN ghi chứng từ đã hoàn tiền ngoài hệ thống; sau đó doanh thu thuần giảm đúng tiền hoàn, đơn vẫn `COMPLETED` và `PAID`. Giao dịch mô phỏng ghi rõ hoàn mô phỏng, không chuyển tiền thật.

Order Flyway V5 và Inventory Flyway V2 chạy tự động khi service khởi động. [API trả hàng](docs/api/returns.md) mô tả giới hạn mỗi đơn một yêu cầu và quy tắc doanh thu. `scripts/Smoke-PartialReturns.ps1` chạy luồng thật qua Gateway/MySQL; kết quả ở [partial-returns-smoke-result.json](docs/partial-returns-smoke-result.json). Bản cập nhật này qua **97/97 JUnit**, **28/28 Vitest** và Vite production build.

## 15. Thông báo tiến trình trả hàng

Khách mở `/notifications` để xem các mốc yêu cầu trả hàng: đã nhận, duyệt hoặc từ chối, cửa hàng nhận hàng, chờ hoàn COD và hoàn tiền một phần. Thông báo được đồng bộ từ lịch sử Order qua REST nội bộ, có khóa duy nhất chống tạo trùng. Trả toàn bộ vẫn dùng thông báo đơn `RETURNED` ở mốc cuối. Chỉ chủ tài khoản xem và đánh dấu đã đọc. Đây là thông báo trong ứng dụng, chưa có email/push; xem [hợp đồng API](docs/api/reporting-notification.md).

`scripts/Smoke-PartialReturns.ps1` đạt **29/29 kiểm tra** qua Gateway/MySQL, gồm hai thông báo mới; `scripts/Smoke-Returns.ps1` kiểm tra hồi quy trả toàn bộ. Toàn backend `mvn clean verify` đạt **98/98 JUnit**, frontend **28/28 Vitest** và production build từ phần trước vẫn hợp lệ vì phần này không sửa frontend.

## 16. Thông báo khuyến mãi theo lựa chọn của khách

Trong `/account`, CUSTOMER bật “Đồng ý nhận thông báo khuyến mãi trong ứng dụng” rồi lưu. Tài khoản mới mặc định tắt. Khi mở `/notifications`, khách chỉ nhận chương trình còn hiệu lực được tạo, cập nhật hoặc bắt đầu từ lúc đồng ý; thông báo dẫn tới sản phẩm hoặc danh mục tương ứng. Tắt lựa chọn chặn thông báo mới, còn các thông báo đã nhận vẫn ở lịch sử. Auth giữ thời điểm đồng ý; Promotion cung cấp chương trình qua REST nội bộ; Reporting/Notification lưu một bản ghi cho mỗi khách và chương trình. Không có email hoặc push.

Auth Flyway V2 và Reporting Flyway V3 tự chạy khi khởi động. `scripts/Smoke-MarketingNotifications.ps1` đạt **16/16 kiểm tra** qua Gateway/MySQL; kết quả ở [marketing-notifications-smoke-result.json](docs/marketing-notifications-smoke-result.json). Toàn backend `mvn clean verify` đạt **101/101 JUnit**, frontend **30/30 Vitest** và Vite production build thành công. Xem [API thông báo](docs/api/reporting-notification.md) và [báo cáo phần 12](docs/bao-cao-phan-12.md).

## 17. Liên hệ của khách chưa đăng nhập

Trang `/contact` có form gửi liên hệ không cần tài khoản, lưu vào `shop_quan_ao_reporting.guest_contacts`. Khách nhận mã tiếp nhận nhưng không thể xem lại nội dung bằng mã công khai. STAFF/ADMIN vào `/manage/support`, chọn liên kết “Xử lý liên hệ từ khách vãng lai” để tìm, xem, tiếp nhận, ghi chú và đánh dấu đã giải quyết. Nội dung liên hệ chỉ hiển thị cho nhân viên được phân quyền. Form có idempotency key để tránh gửi trùng khi mạng lỗi; backend giới hạn 3 lần/giờ cho mỗi email. Flyway Reporting V4 tự chạy khi service khởi động. Hợp đồng ở [Customer Support API](docs/api/customer-support.md); kiểm tra thực tế bằng `./scripts/Smoke-GuestContact.ps1`. Nhân viên chủ động liên hệ qua email/điện thoại; hệ thống chưa tự gửi thư hoặc SMS.

`Smoke-GuestContact.ps1` đạt **19/19 kiểm tra** qua Gateway/MySQL; [kết quả](docs/guest-contact-smoke-result.json). Toàn backend `mvn clean verify` đạt **104/104 JUnit**, frontend **32/32 Vitest** và Vite production build thành công. Xem [báo cáo phần 13](docs/bao-cao-phan-13.md) và [trạng thái kiểm thử](docs/verification.md).

## 18. Tổng quan công việc nhân viên

STAFF mở `/staff` từ mục “Khu vực nhân viên” để xem số đơn chờ xác nhận, đang đóng gói, đang giao; yêu cầu trả hàng và hỗ trợ mới; liên hệ khách vãng lai mới; biến thể sắp hết hàng. Mỗi ô dùng `totalElements` của API có phân quyền và dữ liệu MySQL, dẫn tới hàng chờ xử lý tương ứng. Nút “Làm mới số liệu” gọi lại cả bảy nguồn; lỗi một service được hiện riêng, các ô khác vẫn hoạt động. Trang cho biết quyền ghi kho hiện tại và liên kết hồ sơ tại `/staff/profile`. `scripts/Smoke-StaffDashboard.ps1` đạt **7/7 nguồn dữ liệu** qua Gateway/MySQL; [kết quả](docs/staff-dashboard-smoke-result.json). Frontend đạt **34/34 Vitest** và Vite build. Backend không thay đổi ở phần này; kết quả `mvn clean verify` **104/104 JUnit** từ phần 13 vẫn là lần kiểm thử backend gần nhất. Xem [báo cáo phần 14](docs/bao-cao-phan-14.md).

## 19. Tìm kiếm đơn hàng cho nhân viên

STAFF/ADMIN tại `/manage/orders` tìm theo mã đơn, tên hoặc số điện thoại người nhận; lọc theo khoảng ngày đặt và trạng thái. Bộ lọc chạy trong Order Service trên MySQL trước phân trang, ngày theo `Asia/Ho_Chi_Minh` gồm trọn ngày kết thúc. Kết quả sắp xếp mới nhất trước; CUSTOMER vẫn chỉ xem đơn của chính mình. Tên/điện thoại tìm kiếm là thông tin người nhận lưu trong đơn, chưa tìm theo hồ sơ tài khoản Auth. [API Order](docs/api/cart-order.md) ghi rõ tham số và quyền. `scripts/Smoke-OrderSearch.ps1` đạt **7/7 kiểm tra** qua Gateway/MySQL; [kết quả](docs/order-search-smoke-result.json). Toàn backend `mvn clean verify` đạt **105/105 JUnit**, frontend **35/35 Vitest** và Vite build thành công. Xem [báo cáo phần 15](docs/bao-cao-phan-15.md).

## 20. Xuất báo cáo tồn kho CSV

STAFF/ADMIN vào `/inventory`, áp dụng bộ lọc ID biến thể hoặc “Sắp hết hàng”, rồi chọn “Xuất CSV tồn kho”. File `bao-cao-ton-kho.csv` lấy toàn bộ biến thể phù hợp trực tiếp từ Inventory Service/MySQL, gồm tồn thực tế, đang giữ, khả dụng và mức tối thiểu; không chỉ xuất trang đang xem. CSV UTF-8 BOM mở được tiếng Việt trên Windows. API giới hạn 10.000 biến thể và trả 413 nếu vượt giới hạn để tránh báo cáo thiếu dòng. [Hợp đồng API](docs/api/inventory-export.md) và [smoke 7/7](docs/inventory-export-smoke-result.json) ghi rõ quyền và dữ liệu kiểm tra. Toàn backend `mvn clean verify` đạt **106/106 JUnit**, frontend **36/36 Vitest** và Vite production build thành công. Xem [báo cáo phần 16](docs/bao-cao-phan-16.md).

## 21. Báo cáo đơn hủy và hoàn trả

ADMIN mở `/admin/reports`, chọn “Xem báo cáo đơn hủy và hoàn trả” để lọc theo ngày sự kiện, xem số đơn hủy, trả toàn bộ, trả một phần và tải CSV. Tiền hoàn COD đã ghi nhận được tách khỏi giá trị hoàn mô phỏng. Reporting lấy snapshot và lịch sử Return qua REST nội bộ của Order Service; không đọc database Order trực tiếp. Chỉ sự kiện hủy hoặc hoàn tiền đã hoàn tất được tính, yêu cầu trả đang chờ không được cộng. [Hợp đồng API](docs/api/reporting-notification.md), [kết quả smoke 9/9](docs/order-exceptions-smoke-result.json) và [báo cáo phần 17](docs/bao-cao-phan-17.md) mô tả chi tiết. Toàn backend `mvn clean verify` đạt **107/107 JUnit**, frontend **37/37 Vitest** và Vite production build thành công.

## 22. Doanh thu theo danh mục

ADMIN mở `/admin/reports`, chọn “Xem doanh thu theo danh mục” để lọc ngày hoàn tất, xem doanh thu hàng hóa COD sau giảm giá và hoàn một phần, số sản phẩm, số đơn, cùng phí vận chuyển tách riêng; có xuất CSV. Order Flyway V6 lưu `category_id` trên từng dòng đơn mới; Reporting dùng ID này để doanh thu lịch sử không đổi nhóm khi sản phẩm chuyển danh mục. Đơn cũ có `category_id` NULL vẫn được tra danh mục hiện tại từ Catalog qua REST. Tổng danh mục cộng phí giao khớp doanh thu COD đã thu của báo cáo thời gian. [Hợp đồng API](docs/api/reporting-notification.md) và [báo cáo phần 18](docs/bao-cao-phan-18.md) mô tả cách tính. `scripts/Smoke-CategoryRevenue.ps1` đạt **9/9 kiểm tra** qua Gateway/MySQL trước thay đổi snapshot; [kết quả](docs/category-revenue-smoke-result.json). Kết quả kiểm thử ở thời điểm phần 18: **109/109 JUnit**, **38/38 Vitest** và Vite build production.

Sau sửa snapshot: Order V6 đã áp dụng trên MySQL 8; `scripts/Smoke-CategorySnapshot.ps1` đạt **8/8** qua Gateway/MySQL ([kết quả](docs/category-snapshot-smoke-result.json)). `scripts/Smoke-CategoryRevenue.ps1` đã chạy lại thành công **9/9** sau khi tài khoản mẫu local được đồng bộ với `SEED_PASSWORD`.

## 23. Lọc sản phẩm theo danh mục cha và danh mục con

Khi chọn một danh mục trên trang `/products`, Catalog Service lọc cả sản phẩm gắn trực tiếp và sản phẩm thuộc mọi cấp danh mục con, sau đó mới áp dụng các bộ lọc giá, giới tính, thương hiệu, size, màu và phân trang. Danh sách chọn hiển thị cây danh mục để thấy phạm vi lọc. API và quy tắc ở [Catalog/Inventory](docs/api/catalog-inventory.md). Bản cập nhật đạt **111/111 JUnit**, **40/40 Vitest** và frontend build production.

## 24. Sản phẩm bán chạy từ đơn hàng thật

Khách chọn “Bán chạy” trên trang bộ sưu tập hoặc mục sắp xếp. Reporting đọc các đơn COD đã hoàn tất và ghi nhận thu tiền từ Order qua REST, trừ số lượng đã hoàn một phần, rồi gửi thống kê ID/số lượng sản phẩm cho Catalog qua endpoint nội bộ có khóa. Catalog áp dụng bộ lọc MySQL, sắp xếp toàn bộ kết quả theo lượng bán ròng trước khi phân trang. Khi bằng số lượng, sản phẩm mới hơn đứng trước. Nếu Reporting hoặc Order ngừng phản hồi, API trả 503 thay vì hiển thị thứ tự sai. Quy tắc và giới hạn 10.000 đơn/sản phẩm ở [Catalog/Inventory API](docs/api/catalog-inventory.md). `scripts/Smoke-BestSellers.ps1` đạt **4/4** qua Gateway/MySQL ([kết quả](docs/best-sellers-smoke-result.json)). Toàn backend đạt **113/113 JUnit**, frontend **45/45 Vitest** và Vite production build.

