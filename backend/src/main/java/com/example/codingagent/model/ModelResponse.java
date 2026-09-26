package com.example.codingagent.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record ModelResponse(
        String content,
        List<ToolCall> toolCalls,
        String finishReason,
        Map<String, Object> usage
) {
    public ModelResponse {
        if (toolCalls == null) {
            toolCalls = Collections.emptyList();
        }
        if (usage == null) {
            usage = Collections.emptyMap();
        }
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    public static ModelResponse text(String content) {
        return new ModelResponse(content, Collections.emptyList(), "stop", Collections.emptyMap());
    }

    public static ModelResponse withTools(String content, List<ToolCall> toolCalls) {
        return new ModelResponse(content, toolCalls, "tool_calls", Collections.emptyMap());
    }
}
