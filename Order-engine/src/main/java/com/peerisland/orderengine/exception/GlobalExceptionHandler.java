package com.peerisland.orderengine.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.peerisland.orderengine.dto.response.ErrorResponse;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException exception,
                                                             HttpServletRequest request) {
        log.warn("Order not found handled: {}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), "ORDER_NOT_FOUND", request.getRequestURI());
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidOrderStateException exception,
                                                                  HttpServletRequest request) {
        log.warn("Invalid transition handled: {}", exception.getMessage());
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), "INVALID_TRANSITION", request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgs(IllegalArgumentException exception,
                                                           HttpServletRequest request) {
        log.warn("Illegal argument handled: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), "BAD_REQUEST", request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception,
                                                          HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Validation failed");
        log.warn("Validation failure handled: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, "VALIDATION_ERROR", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error: {}", exception.getMessage(), exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", "INTERNAL_ERROR", request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String code, String path) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .code(code)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
