package com.amins.nphies.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NphiesException.NonRetryable.class)
    public ResponseEntity<Map<String, Object>> handleNonRetryable(NphiesException.NonRetryable ex) {
        log.error("NPHIES rejected request (non-retryable): {}", ex.getMessage());
        return error(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(NphiesCircuitOpenException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitOpen(NphiesCircuitOpenException ex) {
        log.warn("NPHIES circuit breaker open: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(NphiesAuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthError(NphiesAuthException ex) {
        log.error("NPHIES authentication failed: {}", ex.getMessage());
        return error(HttpStatus.BAD_GATEWAY, "NPHIES authentication failed — check tenant credentials");
    }

    @ExceptionHandler(NphiesException.class)
    public ResponseEntity<Map<String, Object>> handleNphies(NphiesException ex) {
        log.error("NPHIES error: {}", ex.getMessage(), ex);
        return error(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
