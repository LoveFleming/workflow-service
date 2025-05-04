package com.example.workflow;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SetNode implements WorkflowComponent {
    @Override
    public ComponentResult execute(ComponentContext ctx) {
        ctx.globals().putAll(ctx.inputs());
        return ComponentResult.ok(ctx.inputs());
    }
}
