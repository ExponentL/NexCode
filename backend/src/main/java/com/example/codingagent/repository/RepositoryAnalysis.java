package com.example.codingagent.repository;

import java.util.Collections;
import java.util.List;

/**
 * Real-time analysis of a physical repository or workspace on the local filesystem.
 * Never hard-coded; strictly discovered dynamically from actual filesystem inspection.
 */
public record RepositoryAnalysis(
        String name,
        String path,
        boolean exists,
        boolean isGit,
        String gitBranch,
        String gitStatus,
        String projectType,
        List<String> detectedLanguages,
        List<String> sourceDirectories,
        List<String> testDirectories,
        String buildSystem,
        List<String> availableCommands,
        int totalFiles,
        String readmeSummary,
        String errorMessage
) {
    public RepositoryAnalysis {
        if (detectedLanguages == null) detectedLanguages = Collections.emptyList();
        if (sourceDirectories == null) sourceDirectories = Collections.emptyList();
        if (testDirectories == null) testDirectories = Collections.emptyList();
        if (availableCommands == null) availableCommands = Collections.emptyList();
    }

    public static RepositoryAnalysis error(String path, String error) {
        return new RepositoryAnalysis(
                path != null ? path : "Unknown",
                path,
                false,
                false,
                null,
                "Directory not accessible",
                "Unknown",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "None",
                Collections.emptyList(),
                0,
                null,
                error
        );
    }
}
