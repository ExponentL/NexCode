package com.example.codingagent.recovery;

import com.example.codingagent.testing.FailureAnalysis;
import com.example.codingagent.testing.TestResult;

public record RecoveryOutcome(
        boolean recovered,
        TestResult finalTestResult,
        FailureAnalysis lastFailureAnalysis,
        int attemptsMade,
        String explanation
) {
    public static RecoveryOutcome success(TestResult result, int attempts) {
        return new RecoveryOutcome(true, result, null, attempts, "Successfully recovered; all tests passing.");
    }

    public static RecoveryOutcome exhausted(TestResult result, FailureAnalysis analysis, int attempts, String explanation) {
        return new RecoveryOutcome(false, result, analysis, attempts, explanation);
    }
}
