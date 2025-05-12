package com.example.workflow.demo;

import com.example.workflow.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WorkflowRunner {

    private final ComponentRegistry registry;

    @Autowired
    public WorkflowRunner(ComponentRegistry registry) {
        this.registry = registry;
    }

    public void run() {
        ExecutionContext ctx = new ExecutionContext("demo-run", new ConsoleStepLogger());

        ctx.put("payload", Map.of("userId", 123));

        runStep("fetchData", "payload", "rawData", ctx);

        List<?> data = ctx.getAs("rawData", List.class);
        if (data == null || data.isEmpty()) {
            runStep("logger", "No data found", "-", ctx);
            return;
        }

        runStep("transformData", "rawData", "transformed", ctx);
        runStep("logger", "transformed", "-", ctx);
    }

    private void runStep(String name, String inKey, String outKey, ExecutionContext ctx) {
        WorkflowComponent<Object, Object> comp = registry.get(name);
        Object input = "-".equals(inKey) ? null : ctx.get(inKey);
        Object output = comp.execute(ctx, input);
        if (!"-".equals(outKey)) {
            ctx.put(outKey, output);
        }
    }
}