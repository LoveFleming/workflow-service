package com.example.workflow.core;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ComponentFailedException.class)
    public ResponseEntity<Map<String, Object>> handle(ComponentFailedException ex) {
        Map<String, Object> body = Map.of(
                "type", "about:blank",
                "title", "Component failure",
                "component", ex.getComponent(),
                "detail", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}