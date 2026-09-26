package com.example.codingagent.model;

import java.util.Map;

public record ToolDefinition(
        String name,
        String description,
        Map<String, Object> parameters
) {}
