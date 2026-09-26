package com.example.codingagent.testing;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the actual result of executing a repository's real test suite.
 * All metrics are derived from actual process execution; values are NEVER hardcoded.
 */
public record TestResult(
        String command,
        TestStatus status,
        Integer totalTests,
        Integer passedTests,
        Integer failedTests,
        Integer skippedTests,
        long durationMs,
        String stdout,
        String stderr,
        int exitCode,
        String output,
        List<TestFailureDetail> failures,
        boolean hasParsedMetrics,
        String projectType
) {
    public TestResult {
        if (failures == null) {
            failures = Collections.emptyList();
        }
    }

    public boolean isSuccess() {
        return status == TestStatus.PASSED;
    }

    public static TestResult success(
            String command,
            Integer total,
            Integer passed,
            Integer skipped,
            long durationMs,
            String stdout,
            String stderr,
            int exitCode,
            String projectType) {
        String combined = (stdout != null ? stdout : "") + (stderr != null && !stderr.isBlank() ? "\n" + stderr : "");
        return new TestResult(
                command,
                TestStatus.PASSED,
                total,
                passed,
                0,
                skipped,
                durationMs,
                stdout != null ? stdout : "",
                stderr != null ? stderr : "",
                exitCode,
                combined,
                Collections.emptyList(),
                total != null && total > 0,
                projectType
        );
    }

    public static TestResult success(
            String command,
            Integer total,
            Integer skipped,
            long durationMs,
            String stdout,
            String stderr,
            String projectType) {
        Integer passed = (total != null && skipped != null) ? Math.max(0, total - skipped) : total;
        return success(command, total, passed, skipped, durationMs, stdout, stderr, 0, projectType);
    }

    public static TestResult failure(
            String command,
            int exitCode,
            Integer total,
            Integer failed,
            long durationMs,
            String stdout,
            String stderr,
            List<TestFailureDetail> failures,
            String projectType) {
        Integer passed = (total != null && failed != null) ? Math.max(0, total - failed) : null;
        return failure(command, total, passed, failed, 0, durationMs, stdout, stderr, exitCode, failures, projectType);
    }

    public static TestResult failure(
            String command,
            Integer total,
            Integer passed,
            Integer failed,
            Integer skipped,
            long durationMs,
            String stdout,
            String stderr,
            int exitCode,
            List<TestFailureDetail> failures,
            String projectType) {
        String combined = (stdout != null ? stdout : "") + (stderr != null && !stderr.isBlank() ? "\n" + stderr : "");
        return new TestResult(
                command,
                TestStatus.FAILED,
                total,
                passed,
                failed,
                skipped,
                durationMs,
                stdout != null ? stdout : "",
                stderr != null ? stderr : "",
                exitCode,
                combined,
                failures != null ? failures : Collections.emptyList(),
                total != null && total > 0,
                projectType
        );
    }

    public static TestResult unparseable(
            String command,
            boolean success,
            long durationMs,
            String stdout,
            String stderr,
            int exitCode,
            String projectType) {
        String combined = (stdout != null ? stdout : "") + (stderr != null && !stderr.isBlank() ? "\n" + stderr : "");
        return new TestResult(
                command,
                success ? TestStatus.PASSED : TestStatus.FAILED,
                null,
                null,
                null,
                null,
                durationMs,
                stdout != null ? stdout : "",
                stderr != null ? stderr : "",
                exitCode,
                combined,
                Collections.emptyList(),
                false,
                projectType
        );
    }

    public static TestResult commandNotFound(
            String command,
            String stdout,
            String stderr,
            long durationMs,
            String projectType) {
        String combined = (stdout != null ? stdout : "") + (stderr != null && !stderr.isBlank() ? "\n" + stderr : "");
        String errMessage = stderr != null && !stderr.isBlank() ? stderr.trim() : "Command not found (exit code 127)";
        List<TestFailureDetail> failureList = List.of(new TestFailureDetail(command, "command_not_found", errMessage, combined));
        return new TestResult(
                command,
                TestStatus.ERROR,
                0,
                0,
                0,
                0,
                durationMs,
                stdout != null ? stdout : "",
                stderr != null ? stderr : "",
                127,
                combined,
                failureList,
                false,
                projectType != null ? projectType : "Unknown"
        );
    }

    public static TestResult error(
            String command,
            String errorMessage,
            long durationMs,
            String projectType) {
        return new TestResult(
                command,
                TestStatus.ERROR,
                null,
                null,
                null,
                null,
                durationMs,
                "",
                errorMessage != null ? errorMessage : "",
                -1,
                errorMessage != null ? errorMessage : "",
                Collections.emptyList(),
                false,
                projectType
        );
    }

    public static TestResult noTests(String reason, String projectType) {
        return new TestResult(
                "none",
                TestStatus.NO_TESTS,
                0,
                0,
                0,
                0,
                0L,
                "",
                "",
                0,
                reason != null ? reason : "No test suite detected",
                Collections.emptyList(),
                false,
                projectType != null ? projectType : "Unknown"
        );
    }
}
