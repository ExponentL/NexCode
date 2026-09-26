package com.example.codingagent.tools;

import com.example.codingagent.model.ToolCall;
import com.example.codingagent.model.ToolDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ToolManager {

    private static final Logger log = LoggerFactory.getLogger(ToolManager.class);

    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolManager(List<AgentTool> agentTools) {
        for (AgentTool tool : agentTools) {
            registerTool(tool.getName(), tool);
        }
        // Register standard aliases so model requests match naturally
        if (tools.containsKey("file_tool")) {
            tools.put("filesystem", tools.get("file_tool"));
        }
        if (tools.containsKey("search_tool")) {
            tools.put("search", tools.get("search_tool"));
        }
        if (tools.containsKey("repository_tool")) {
            tools.put("repository", tools.get("repository_tool"));
        }
        if (tools.containsKey("terminal_tool")) {
            tools.put("terminal", tools.get("terminal_tool"));
        }
        if (tools.containsKey("git_tool")) {
            tools.put("git", tools.get("git_tool"));
        }
        if (tools.containsKey("test_tool")) {
            tools.put("test_runner", tools.get("test_tool"));
        }
    }

    public void registerTool(String name, AgentTool tool) {
        tools.put(name.toLowerCase(), tool);
        log.info("Registered tool: {}", name);
    }

    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> definitions = new ArrayList<>();
        // Deduplicate definition objects across aliases
        List<String> seen = new ArrayList<>();
        for (AgentTool tool : tools.values()) {
            if (!seen.contains(tool.getName())) {
                seen.add(tool.getName());
                definitions.add(tool.getDefinition());
            }
        }
        return definitions;
    }

    public AgentTool getTool(String name) {
        if (name == null) return null;
        return tools.get(name.toLowerCase());
    }

    public ToolResult executeTool(ToolCall toolCall, String workingDirectory) {
        String toolName = toolCall.name();
        AgentTool tool = getTool(toolName);
        if (tool == null) {
            log.error("Requested tool not found: {}", toolName);
            return ToolResult.failure("Tool not found: " + toolName);
        }

        try {
            Map<String, Object> arguments = Collections.emptyMap();
            if (toolCall.argumentsJson() != null && !toolCall.argumentsJson().isBlank()) {
                arguments = objectMapper.readValue(toolCall.argumentsJson(), new TypeReference<Map<String, Object>>() {});
            }
            log.info("ToolManager dispatching '{}' in {}", toolName, workingDirectory);
            return tool.execute(arguments, workingDirectory);
        } catch (Exception e) {
            log.error("Tool execution error for {}: {}", toolName, e.getMessage());
            return ToolResult.failure("Failed to execute tool '" + toolName + "': " + e.getMessage());
        }
    }

    public ToolResult executeTool(String toolName, Map<String, Object> arguments, String workingDirectory) {
        AgentTool tool = getTool(toolName);
        if (tool == null) {
            return ToolResult.failure("Tool not found: " + toolName);
        }
        return tool.execute(arguments, workingDirectory);
    }
}
