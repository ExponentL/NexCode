package com.example.codingagent.verification;

import com.example.codingagent.agent.AgentStatus;
import com.example.codingagent.agent.AgentStep;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Final immutable task outcome consumed by the dashboard.
 */
public record TaskResult(
        String taskId,
        String task,
        AgentStatus status,
        boolean verified,
        String summary,
        VerificationResult verificationResult,
        List<String> filesInspected,
        List<String> filesModified,
        List<AgentStep> plan,
        GitDiffSummary gitDiffSummary,
        long durationMs,
        Instant createdAt,
        Instant completedAt
) {
    public TaskResult {
        if (filesInspected == null) filesInspected = Collections.emptyList();
        if (filesModified == null) filesModified = Collections.emptyList();
        if (plan == null) plan = Collections.emptyList();
    }
}
