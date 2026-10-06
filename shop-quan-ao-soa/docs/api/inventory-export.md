# Xuất báo cáo tồn kho

Inventory Service sở hữu dữ liệu trong `shop_quan_ao_inventory`. Frontend gọi qua Gateway; báo cáo không đọc database của Catalog hoặc Order.

| HTTP | URL | Đầu vào → đầu ra | Quyền |
|---|---|---|---|
| GET | `/api/inventory/export.csv?low=&variantId=` | CSV UTF-8 BOM, tên `bao-cao-ton-kho.csv` | STAFF, ADMIN |

`low=true` chỉ chọn biến thể có tồn khả dụng nhỏ hơn hoặc bằng mức tối thiểu. `variantId` lọc một biến thể. Bỏ cả hai tham số để xuất toàn bộ kho. CSV gồm mã biến thể, tồn thực tế, số đang giữ, số khả dụng, mức tối thiểu và cờ sắp hết hàng; số khả dụng bằng tồn thực tế trừ số đang giữ. Dữ liệu lấy tại thời điểm yêu cầu, sắp theo mã biến thể tăng dần. Tối đa 10.000 biến thể; vượt giới hạn trả HTTP 413 và yêu cầu lọc theo ID. CSV chỉ có số và các giá trị cố định nên không đưa nội dung người dùng vào ô bảng tính. Trên trang `/inventory`, nút xuất dùng bộ lọc đã áp dụng và tải toàn bộ dòng phù hợp, không chỉ trang hiện tại.
