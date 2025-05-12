package com.example.workflow.components;

import com.example.workflow.core.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FetchDataComponent extends AbstractComponent<Map<String, Object>, List<Map<String, Object>>> {
    @Override public String getName() { return "fetchData"; }

    @Override protected List<Map<String, Object>> doExecute(ExecutionContext ctx, Map<String, Object> in) {
        return List.of(Map.of("id", 1, "name", "Test User"));
    }
}