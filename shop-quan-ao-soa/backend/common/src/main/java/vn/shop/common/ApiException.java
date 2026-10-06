package vn.shop.common;

public class ApiException extends RuntimeException {
    private final int status;
    public ApiException(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
    public static ApiException missing() { return new ApiException(404, "Không tìm thấy dữ liệu"); }
}
