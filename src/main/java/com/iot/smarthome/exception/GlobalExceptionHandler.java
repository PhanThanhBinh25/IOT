package com.iot.smarthome.exception;

import com.iot.smarthome.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý tập trung tất cả exception từ controller.
 * Trả về response dạng ApiResponse thống nhất.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý lỗi validation dữ liệu đầu vào (@Valid trên DTO).
     * Thu thập danh sách các trường vi phạm và trả về HTTP 400 Bad Request kèm thông báo chi tiết.
     *
     * @param ex Ngoại lệ MethodArgumentNotValidException do Spring ném ra khi validation thất bại
     * @return ResponseEntity chứa ApiResponse chứa map lỗi chi tiết từng trường
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field   = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Dữ liệu đầu vào không hợp lệ")
                        .data(errors)
                        .build());
    }

    /**
     * Xử lý lỗi xác thực khi tên đăng nhập hoặc mật khẩu không chính xác.
     *
     * @param ex Ngoại lệ BadCredentialsException của Spring Security
     * @return ResponseEntity HTTP 401 Unauthorized kèm ApiResponse lỗi
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Tên đăng nhập hoặc mật khẩu không đúng"));
    }

    /**
     * Xử lý lỗi không tìm thấy tài nguyên trong cơ sở dữ liệu (User, Device, Sensor...).
     *
     * @param ex Ngoại lệ ResourceNotFoundException tự định nghĩa
     * @return ResponseEntity HTTP 404 Not Found kèm ApiResponse lỗi
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Xử lý lỗi tham số logic không hợp lệ (tên thiết bị hoặc trạng thái lệnh không đúng chuẩn).
     *
     * @param ex Ngoại lệ IllegalArgumentException
     * @return ResponseEntity HTTP 400 Bad Request kèm ApiResponse lỗi
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Bắt và xử lý tất cả các ngoại lệ không mong đợi còn lại trong toàn hệ thống.
     * Ghi log vết lỗi chi tiết và trả về thông điệp an toàn cho client với HTTP 500 Internal Server Error.
     *
     * @param ex Ngoại lệ Exception chung
     * @return ResponseEntity HTTP 500 kèm ApiResponse thông báo lỗi hệ thống
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Lỗi không xác định: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi hệ thống. Vui lòng thử lại sau."));
    }
}

