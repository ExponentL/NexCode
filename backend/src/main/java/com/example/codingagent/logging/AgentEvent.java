package com.example.codingagent.logging;

import java.time.Instant;
import java.util.Map;

public record AgentEvent(
        String eventId,
        String taskId,
        EventType type,
        String message,
        Map<String, Object> details,
        Instant timestamp
) {
    public static AgentEvent of(String taskId, EventType type, String message) {
        return new AgentEvent(
                java.util.UUID.randomUUID().toString(),
                taskId,
                type,
                message,
                Map.of(),
                Instant.now()
        );
    }

    public static AgentEvent of(String taskId, EventType type, String message, Map<String, Object> details) {
        return new AgentEvent(
                java.util.UUID.randomUUID().toString(),
                taskId,
                type,
                message,
                details != null ? details : Map.of(),
                Instant.now()
        );
    }
}
