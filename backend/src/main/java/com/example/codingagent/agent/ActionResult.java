package com.example.codingagent.agent;

import com.example.codingagent.model.action.ActionType;

import java.time.Instant;

/**
 * Captures the execution outcome of an individual action performed by the agent harness.
 * Fed back into agent state and foundation model context.
 */
public record ActionResult(
        ActionType actionType,
        String target,
        boolean success,
        String output,
        String error,
        long durationMs,
        Instant timestamp
) {
    public static ActionResult of(ActionType actionType, String target, boolean success, String output, String error, long durationMs) {
        return new ActionResult(actionType, target, success, output, error, durationMs, Instant.now());
    }

    public static ActionResult success(ActionType actionType, String target, String output, long durationMs) {
        return new ActionResult(actionType, target, true, output, null, durationMs, Instant.now());
    }

    public static ActionResult failure(ActionType actionType, String target, String error, long durationMs) {
        return new ActionResult(actionType, target, false, null, error, durationMs, Instant.now());
    }
}
