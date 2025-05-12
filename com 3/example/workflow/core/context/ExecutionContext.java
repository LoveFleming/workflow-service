package com.example.workflow.core;

import java.util.HashMap;
import java.util.Map;

public class ExecutionContext {
    private final String correlationId;
    private final StepLogger logger;
    private final Map<String, Object> variables = new HashMap<>();
    private boolean aborted = false;
    private final PlaceholderResolver resolver = new PlaceholderResolver();

    public ExecutionContext(String correlationId, StepLogger logger) {
        this.correlationId = correlationId;
        this.logger = logger;
    }

    public String getCorrelationId() { return correlationId; }
    public StepLogger logger() { return logger; }

    public Object get(String key) { return variables.get(key); }
    public <T> T getAs(String key, Class<T> type) {
        Object val = variables.get(key);
        return type.isInstance(val) ? type.cast(val) : null;
    }
    public void put(String key, Object val) { variables.put(key, val); }
    public boolean isEmpty(String key) {
        Object val = variables.get(key);
        return val == null || (val instanceof Iterable<?> i && !i.iterator().hasNext());
    }

    public PlaceholderResolver resolver() { return resolver; }
    public void abort() { aborted = true; }
    public boolean isAborted() { return aborted; }
}