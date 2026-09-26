package com.example.codingagent.agent;

public record TestResultItem(
        String suiteName,
        String testName,
        boolean passed,
        String errorTrace,
        long durationMs
) {
    public static TestResultItem success(String suiteName, String testName, long durationMs) {
        return new TestResultItem(suiteName, testName, true, null, durationMs);
    }

    public static TestResultItem failure(String suiteName, String testName, String errorTrace, long durationMs) {
        return new TestResultItem(suiteName, testName, false, errorTrace, durationMs);
    }
}
