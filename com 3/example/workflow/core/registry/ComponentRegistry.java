package com.example.workflow.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ComponentRegistry {
    private final Map<String, WorkflowComponent<?, ?>> components = new HashMap<>();

    @Autowired
    public ComponentRegistry(List<WorkflowComponent<?, ?>> beans) {
        for (WorkflowComponent<?, ?> c : beans) {
            components.put(c.getName(), c);
        }
    }

    @SuppressWarnings("unchecked")
    public <I, O> WorkflowComponent<I, O> get(String name) {
        WorkflowComponent<?, ?> comp = components.get(name);
        if (comp == null) throw new IllegalArgumentException("Component not found: " + name);
        return (WorkflowComponent<I, O>) comp;
    }
}