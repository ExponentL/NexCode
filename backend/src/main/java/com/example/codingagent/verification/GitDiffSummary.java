package com.example.codingagent.verification;

import java.util.Collections;
import java.util.List;

public record GitDiffSummary(
        List<String> modifiedFiles,
        List<String> addedFiles,
        List<String> deletedFiles,
        String diff,
        int totalChanges
) {
    public static GitDiffSummary empty() {
        return new GitDiffSummary(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "", 0);
    }
}
