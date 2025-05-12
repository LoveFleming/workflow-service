package com.example.workflow.components;

import com.example.workflow.core.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TransformDataComponent extends AbstractComponent<List<Map<String, Object>>, List<String>> {
    @Override public String getName() { return "transformData"; }

    @Override protected List<String> doExecute(ExecutionContext ctx, List<Map<String, Object>> input) {
        return input.stream().map(m -> m.get("name").toString().toUpperCase()).toList();
    }
}