package com.example.workflow;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handle(Exception ex, HttpServletRequest req) {
        return new ResponseEntity<>(
                Map.of("timestamp", Instant.now().toString(),
                       "path", req.getRequestURI(),
                       "message", ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
