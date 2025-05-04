package com.example.workflow;

public interface WorkflowComponent {
    ComponentResult execute(ComponentContext ctx);
}
