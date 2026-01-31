package com.applab.applab_backend.common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationErrors(MethodArgumentNotValidException exception) {
        String errorMessage = exception.getBindingResult().getFieldError().getDefaultMessage();
        return new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
    }

    // Handle database unique constraint errors
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex) {
        String message = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        if (message != null && message.contains("Duplicate entry")) {
            message = "Username already exists!";
        }
        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
    }

    // Handle ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of("message", ex.getReason()));
    }

    // Handle other runtime exceptions
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", ex.getMessage()));
    }

}
