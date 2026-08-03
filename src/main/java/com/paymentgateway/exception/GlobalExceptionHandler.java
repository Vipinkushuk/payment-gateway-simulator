package com.paymentgateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles @Valid validation failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "error", "Validation Failed",
                "details", fieldErrors,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // Handles duplicate merchant registration
    @ExceptionHandler(MerchantAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleMerchantAlreadyExists(
            MerchantAlreadyExistsException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // Catch-all for unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", "Something went wrong. Please try again.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(
            OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(OrderAlreadyPaidException.class)
    public ResponseEntity<Map<String, Object>> handleOrderAlreadyPaid(
            OrderAlreadyPaidException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(OrderExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleOrderExpired(
            OrderExpiredException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "status", 422,
                "error", "Unprocessable Entity",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(FraudDetectedException.class)
    public ResponseEntity<Map<String, Object>> handleFraud(
            FraudDetectedException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "status", 429,
                "error", "Too Many Requests",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidState(
            InvalidPaymentStateException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "status", 422,
                "error", "Invalid State Transition",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}