package com.example.codingagent.verification;

import com.example.codingagent.agent.TestResultItem;
import com.example.codingagent.testing.TestResult;

import java.util.Collections;
import java.util.List;

/**
 * Structured outcome of the final verification stage.
 * Verification is based strictly on real evidence, never model assertions.
 */
public record VerificationResult(
        boolean verified,
        String summary,
        List<VerificationCheck> checksPerformed,
        List<String> checksPassed,
        List<String> checksFailed,
        TestResult testResult,
        BuildResult buildResult,
        GitDiffSummary gitDiff,
        List<String> remainingErrors,
        List<TestResultItem> testDetails
) {
    public VerificationResult {
        if (checksPerformed == null) checksPerformed = Collections.emptyList();
        if (checksPassed == null) checksPassed = Collections.emptyList();
        if (checksFailed == null) checksFailed = Collections.emptyList();
        if (remainingErrors == null) remainingErrors = Collections.emptyList();
        if (testDetails == null) testDetails = Collections.emptyList();
    }

    public boolean passed() {
        return verified;
    }

    public List<String> failedChecks() {
        return checksFailed;
    }

    public static VerificationResult success(
            String summary,
            List<VerificationCheck> checksPerformed,
            List<String> checksPassed,
            TestResult testResult,
            BuildResult buildResult,
            GitDiffSummary gitDiff,
            List<TestResultItem> testDetails) {
        return new VerificationResult(
                true,
                summary,
                checksPerformed,
                checksPassed,
                Collections.emptyList(),
                testResult,
                buildResult,
                gitDiff,
                Collections.emptyList(),
                testDetails
        );
    }

    public static VerificationResult failure(
            String summary,
            List<VerificationCheck> checksPerformed,
            List<String> checksPassed,
            List<String> checksFailed,
            TestResult testResult,
            BuildResult buildResult,
            GitDiffSummary gitDiff,
            List<String> remainingErrors,
            List<TestResultItem> testDetails) {
        return new VerificationResult(
                false,
                summary,
                checksPerformed,
                checksPassed,
                checksFailed,
                testResult,
                buildResult,
                gitDiff,
                remainingErrors,
                testDetails
        );
    }
}
