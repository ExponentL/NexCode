package com.example.codingagent.verification;

public record VerificationCheck(
        String name,
        boolean passed,
        String evidence
) {}
