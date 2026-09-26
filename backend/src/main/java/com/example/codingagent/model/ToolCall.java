package com.example.codingagent.model;

public record ToolCall(
        String id,
        String name,
        String argumentsJson
) {}
