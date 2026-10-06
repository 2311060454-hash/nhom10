package vn.shop.catalog;

import vn.shop.common.Errors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.annotation.Order;

@RestControllerAdvice @Order(0)
public class DatabaseErrors {
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict() { return ResponseEntity.status(409).body(Errors.body(409,"Dữ liệu trùng hoặc vi phạm ràng buộc")); }
}
