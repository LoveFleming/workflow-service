package com.example.app.processflow;

import org.springframework.stereotype.Component;
import java.util.List;

// Each node should implement this interface
interface ProcessNode {
    String run(String input);
}

// Example nodes
@Component
class TrimNode implements ProcessNode {
    @Override
    public String run(String input) { return input == null ? null : input.trim(); }
}

@Component
class UppercaseNode implements ProcessNode {
    @Override
    public String run(String input) { return input == null ? null : input.toUpperCase(); }
}

@Component
class SuffixNode implements ProcessNode {
    @Override
    public String run(String input) { return input == null ? null : input + " - PROCESSED"; }
}

@Component
public class BusinessProcessFlow {
    private final List<ProcessNode> nodes;

    // Spring injects all beans implementing ProcessNode
    public BusinessProcessFlow(List<ProcessNode> nodes) {
        this.nodes = nodes;
    }

    public String execute(String input) {
        String result = input;
        for (ProcessNode node : nodes) {
            result = node.run(result);
        }
        return result;
    }
}