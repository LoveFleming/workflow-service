package com.example.workflow.components;

import com.example.workflow.core.*;
import org.springframework.stereotype.Component;

@Component
public class LoggerComponent extends AbstractComponent<String, Void> {
    @Override public String getName() { return "logger"; }

    @Override protected Void doExecute(ExecutionContext ctx, String input) {
        ctx.logger().info(getName(), "Logged input: " + input);
        return null;
    }
}