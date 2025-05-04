package com.example.workflow;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class IfNode implements WorkflowComponent {
    @Override
    public ComponentResult execute(ComponentContext ctx) {
        Map<String, Object> inputs = ctx.inputs();
        String v1 = (String) inputs.get("value1");
        String v2 = (String) inputs.get("value2");
        boolean cond = v1 != null && v1.equals(v2);
        return ComponentResult.ok(Map.of("condition", cond));
    }
}
