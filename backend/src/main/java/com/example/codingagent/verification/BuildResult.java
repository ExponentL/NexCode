package com.example.codingagent.verification;

public record BuildResult(
        boolean success,
        String command,
        int exitCode,
        String output,
        long durationMs,
        String message
) {
    public static BuildResult skipped(String message) {
        return new BuildResult(true, "none", 0, "", 0L, message);
    }

    public static BuildResult success(String command, int exitCode, String output, long durationMs) {
        return new BuildResult(true, command, exitCode, output, durationMs, "Build succeeded");
    }

    public static BuildResult failure(String command, int exitCode, String output, long durationMs, String message) {
        return new BuildResult(false, command, exitCode, output, durationMs, message);
    }
}
