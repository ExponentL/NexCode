package com.example.codingagent.workspace;

import com.example.codingagent.repository.RepositoryAnalysis;

public record WorkspaceSelectionResult(
        String path,
        String name,
        boolean exists,
        boolean readable,
        boolean cancelled,
        String message,
        RepositoryAnalysis analysis
) {
    public static WorkspaceSelectionResult cancelled(String message) {
        return new WorkspaceSelectionResult(null, null, false, false, true, message != null ? message : "No directory selected", null);
    }

    public static WorkspaceSelectionResult error(String message) {
        return new WorkspaceSelectionResult(null, null, false, false, false, message, null);
    }

    public static WorkspaceSelectionResult success(String path, String name, RepositoryAnalysis analysis) {
        return new WorkspaceSelectionResult(path, name, true, true, false, "Directory selected successfully", analysis);
    }
}
