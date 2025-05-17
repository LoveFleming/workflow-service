package com.example.app.controller;

import com.example.app.service.DemoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {
    private final DemoService demoService;
    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/{input}")
    public String process(@PathVariable String input) {
        return demoService.process(input);
    }
}