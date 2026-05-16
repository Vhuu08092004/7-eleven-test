package com.seveneleven.exception;

import com.seveneleven.dto.BaseResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "N/A";
    }

    private <T> BaseResponse<T> errorResponse(String message) {
        return BaseResponse.<T>builder()
                .success(false)
                .message(message)
                .traceId(getTraceId())
                .build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex, HttpServletResponse response) {
        log.error("[{}] Resource not found: {}", getTraceId(), ex.getMessage());
        response.setHeader("X-Trace-Id", getTraceId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadRequest(BadRequestException ex, HttpServletResponse response) {
        log.error("[{}] Bad request: {}", getTraceId(), ex.getMessage());
        response.setHeader("X-Trace-Id", getTraceId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<BaseResponse<Void>> handleUnauthorized(UnauthorizedException ex, HttpServletResponse response) {
        log.error("[{}] Unauthorized: {}", getTraceId(), ex.getMessage());
        response.setHeader("X-Trace-Id", getTraceId());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadCredentials(BadCredentialsException ex, HttpServletResponse response) {
        log.error("[{}] Bad credentials: {}", getTraceId(), ex.getMessage());
        response.setHeader("X-Trace-Id", getTraceId());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse("Invalid email or password"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Void>> handleAuthentication(AuthenticationException ex, HttpServletResponse response) {
        log.error("[{}] Authentication failed: {}", getTraceId(), ex.getMessage());
        response.setHeader("X-Trace-Id", getTraceId());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse("Authentication failed"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletResponse response) {
        log.error("[{}] Access denied: {}", getTraceId(), ex.getMessage());
        response.setHeader("X-Trace-Id", getTraceId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorResponse("Access denied"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex, HttpServletResponse response) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.error("[{}] Validation error: {}", getTraceId(), errors);
        response.setHeader("X-Trace-Id", getTraceId());
        BaseResponse<Map<String, String>> responseEntity = BaseResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed")
                .traceId(getTraceId())
                .data(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseEntity);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletResponse response) {
        log.error("[{}] Constraint violation: {}", getTraceId(), ex.getMessage());
        response.setHeader("X-Trace-Id", getTraceId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGeneral(Exception ex, HttpServletResponse response) {
        String traceId = getTraceId();
        log.error("[{}] Unexpected error: ", traceId, ex);
        response.setHeader("X-Trace-Id", traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("An unexpected error occurred. Reference: " + traceId));
    }
}
