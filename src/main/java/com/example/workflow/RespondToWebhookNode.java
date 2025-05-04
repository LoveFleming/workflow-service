package com.example.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RespondToWebhookNode implements WorkflowComponent {
    @Override
    public ComponentResult execute(ComponentContext ctx) {
        int code = (int) ctx.inputs().getOrDefault("responseCode", 200);
        Object body = ctx.inputs().get("body");
        return ComponentResult.ok(Map.of(
                "httpStatus", HttpStatus.valueOf(code),
                "body", body));
    }
}
