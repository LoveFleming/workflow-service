package com.example.workflow;

import org.springframework.stereotype.Component;

@Component
public class WebhookNode implements WorkflowComponent {
    @Override
    public ComponentResult execute(ComponentContext ctx) {
        // passthrough
        return ComponentResult.ok(ctx.inputs().get("payload"));
    }
}
