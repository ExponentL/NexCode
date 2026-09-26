package com.example.codingagent.testing;

import java.util.Collections;
import java.util.List;

/**
 * Structured diagnostic diagnosis of actual failures detected in real commands, compilers, or tests.
 */
public record FailureAnalysis(
        FailureType failureType,
        String summary,
        List<String> affectedFiles,
        List<String> keyErrorLines,
        String rootCause,
        String suggestedRemediation
) {
    public FailureAnalysis {
        if (affectedFiles == null) affectedFiles = Collections.emptyList();
        if (keyErrorLines == null) keyErrorLines = Collections.emptyList();
    }
}
