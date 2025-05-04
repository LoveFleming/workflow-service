package com.example.workflow;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/workflows")
public class WorkflowController {

    private final HardCodedExecutor executor;

    public WorkflowController(HardCodedExecutor executor) {
        this.executor = executor;
    }

    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(executor.execute(body));
    }
}
