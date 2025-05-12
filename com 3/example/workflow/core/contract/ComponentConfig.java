package com.example.workflow.core;

import java.util.Map;
import java.util.Objects;

public record ComponentConfig(Map<String, Object> values) {
    public ComponentConfig {
        values = Objects.requireNonNullElse(values, Map.of());
    }

    public Object get(String key) {
        return values.get(key);
    }

    public String getAsString(String key) {
        return Objects.toString(values.get(key), null);
    }

    public Boolean getAsBoolean(String key, boolean defaultVal) {
        Object val = values.get(key);
        return val instanceof Boolean ? (Boolean) val : defaultVal;
    }
}