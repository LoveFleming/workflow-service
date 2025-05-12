package com.example.workflow.core;

public class ComponentFailedException extends RuntimeException {
    private final String component;

    public ComponentFailedException(String component, Throwable cause) {
        super(cause);
        this.component = component;
    }

    public String getComponent() {
        return component;
    }
}