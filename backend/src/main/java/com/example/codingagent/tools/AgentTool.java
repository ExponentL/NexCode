package com.example.codingagent.tools;

import com.example.codingagent.model.ToolDefinition;

import java.util.Map;

public interface AgentTool {
    String getName();
    String getDescription();
    ToolDefinition getDefinition();
    ToolResult execute(Map<String, Object> arguments, String workingDirectory);
}
