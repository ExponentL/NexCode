package com.example.codingagent.model;

import java.util.List;

public record ChatMessage(
        String role,
        String content,
        List<ToolCall> toolCalls,
        String toolCallId
) {
    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, null, null);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, null, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, null, null);
    }

    public static ChatMessage assistantWithToolCalls(String content, List<ToolCall> toolCalls) {
        return new ChatMessage("assistant", content, toolCalls, null);
    }

    public static ChatMessage toolResponse(String toolCallId, String content) {
        return new ChatMessage("tool", content, null, toolCallId);
    }
}
