package com.example.codingagent.tools;

import java.util.Collections;
import java.util.Map;

public record ToolResult(
        boolean success,
        String output,
        String error,
        Map<String, Object> metadata
) {
    public static ToolResult success(String output) {
        return new ToolResult(true, output, null, Collections.emptyMap());
    }

    public static ToolResult success(String output, Map<String, Object> metadata) {
        return new ToolResult(true, output, null, metadata != null ? metadata : Collections.emptyMap());
    }

    public static ToolResult failure(String error) {
        return new ToolResult(false, null, error, Collections.emptyMap());
    }

    public static ToolResult failure(String error, String partialOutput) {
        return new ToolResult(false, partialOutput, error, Collections.emptyMap());
    }

    public static ToolResult failure(String error, String partialOutput, Map<String, Object> metadata) {
        return new ToolResult(false, partialOutput, error, metadata != null ? metadata : Collections.emptyMap());
    }

    public long durationMs() {
        if (metadata != null && metadata.get("durationMs") instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }
}
