package vn.shop.common;

import java.time.Instant;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class Errors {
    public static Map<String, Object> body(int status, String message) {
        return Map.of("code", status, "message", message, "timestamp", Instant.now().toString());
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException e) { return ResponseEntity.status(e.status()).body(body(e.status(), e.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().findFirst().map(x -> x.getField() + ": " + x.getDefaultMessage()).orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(body(400, message));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class, IllegalArgumentException.class,
        org.springframework.web.bind.ServletRequestBindingException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> malformed(Exception e) { return ResponseEntity.badRequest().body(body(400, "Dữ liệu không hợp lệ")); }
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ResponseEntity<?> notFound() { return ResponseEntity.status(404).body(body(404,"Không tìm thấy đường dẫn")); }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<?> methodNotAllowed() { return ResponseEntity.status(405).body(body(405,"Phương thức HTTP không được hỗ trợ")); }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<?> unsupportedMedia() { return ResponseEntity.status(415).body(body(415,"Định dạng dữ liệu không được hỗ trợ")); }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<?> oversized() { return ResponseEntity.status(413).body(body(413,"Tệp tải lên vượt giới hạn cho phép")); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<?> denied() { return ResponseEntity.status(403).body(body(403, "Bạn không có quyền thực hiện thao tác này")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) {
        // Chỉ ghi loại lỗi; không ghi query, request, secret hoặc dữ liệu cá nhân.
        LoggerFactory.getLogger(Errors.class).error("Unhandled error type: {}", e.getClass().getName());
        return ResponseEntity.status(500).body(body(500, "Lỗi hệ thống. Vui lòng thử lại sau"));
    }
}
