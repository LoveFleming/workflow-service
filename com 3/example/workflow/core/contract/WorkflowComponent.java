package com.example.workflow.core;

public interface WorkflowComponent<I, O> {
    String getName();
    void configure(ComponentConfig config);
    O execute(ExecutionContext ctx, I input);
}