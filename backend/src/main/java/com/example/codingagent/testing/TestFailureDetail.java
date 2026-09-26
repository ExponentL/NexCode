package com.example.codingagent.testing;

public record TestFailureDetail(
        String suiteName,
        String testName,
        String message,
        String errorTrace
) {
    public static TestFailureDetail of(String suiteName, String testName, String message, String errorTrace) {
        return new TestFailureDetail(suiteName, testName, message, errorTrace);
    }
}
