package com.example.workflow;

import java.util.Map;

/**
 * Context passed to components.
 */
public record ComponentContext(Map<String, Object> inputs,
                               Map<String, Object> globals) { }
