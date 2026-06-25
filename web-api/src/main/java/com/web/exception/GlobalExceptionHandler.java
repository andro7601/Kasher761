package com.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Bad request";
        return ResponseEntity.status(resolveStatus(msg)).body(Map.of("error", msg));
    }

    private HttpStatus resolveStatus(String msg) {
        String lower = msg.toLowerCase();
        if (lower.contains("invalid")) return HttpStatus.UNAUTHORIZED;
        if (lower.contains("taken") || lower.contains("registered")) return HttpStatus.CONFLICT;
        return HttpStatus.BAD_REQUEST;
    }
}
