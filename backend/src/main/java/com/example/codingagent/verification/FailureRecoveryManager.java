package com.example.codingagent.verification;

import com.example.codingagent.agent.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FailureRecoveryManager {

    private static final Logger log = LoggerFactory.getLogger(FailureRecoveryManager.class);

    public boolean canRetry(AgentState state) {
        return state.getAttempts() < state.getMaxAttempts();
    }

    public String generateRecoveryPlan(AgentState state, VerificationResult failureResult) {
        StringBuilder advice = new StringBuilder();
        advice.append("Previous attempt (Attempt ").append(state.getAttempts()).append(" of ").append(state.getMaxAttempts()).append(") failed verification.\n\n");
        advice.append("Identified Failures:\n");

        for (String failure : failureResult.checksFailed()) {
            advice.append("- ").append(failure).append("\n");
        }

        if (!failureResult.testDetails().isEmpty()) {
            advice.append("\nTest Errors:\n");
            for (var test : failureResult.testDetails()) {
                if (!test.passed()) {
                    advice.append("Test: ").append(test.testName()).append("\nError: ").append(test.errorTrace()).append("\n");
                }
            }
        }

        if (failureResult.testResult() != null && !failureResult.testResult().isSuccess()) {
            advice.append("\nActual Failed Test Output:\n");
            String stderr = failureResult.testResult().stderr();
            String stdout = failureResult.testResult().stdout();
            String combined = (stderr != null && !stderr.isBlank() ? stderr.trim() + "\n" : "") +
                              (stdout != null && !stdout.isBlank() ? stdout.trim() : "");
            advice.append(extractConciseDiagnostics(combined, 600)).append("\n");
        }

        if (failureResult.buildResult() != null && !failureResult.buildResult().success()) {
            advice.append("\nActual Build Failure Output:\n");
            if (failureResult.buildResult().output() != null && !failureResult.buildResult().output().isBlank()) {
                advice.append(extractConciseDiagnostics(failureResult.buildResult().output(), 500)).append("\n");
            }
        }

        boolean isCommandNotFound = failureResult.testResult() != null && failureResult.testResult().exitCode() == 127;
        if (isCommandNotFound) {
            advice.append("\nENVIRONMENT DIAGNOSTIC:\n");
            advice.append("The test command exited with code 127 (command not found).\n");
            advice.append("A required build or test executable is not installed on the host system PATH.\n");
            advice.append("Do NOT retry running the missing command. Please verify your source code changes directly.\n");
        }

        if (state.getDetectedTestCommand() != null && !state.getDetectedTestCommand().isBlank()) {
            advice.append("\nDetected Test Command for this repository:\n`").append(state.getDetectedTestCommand()).append("`\n");
        }

        advice.append("\nRequired Recovery Actions:\n");
        advice.append("1. Analyze the root cause of the error output above.\n");
        advice.append("2. Inspect the modified files to locate syntax errors, missing imports, or incorrect logic.\n");
        advice.append("3. Apply surgical file modifications using WRITE_FILE to fix the errors.\n");
        advice.append("4. The harness will automatically rerun the detected test suite after your file changes.\n");

        log.info("Generated recovery instructions for task {}: {} failed checks", state.getTaskId(), failureResult.checksFailed().size());
        return advice.toString();
    }

    private String extractConciseDiagnostics(String output, int maxChars) {
        if (output == null || output.isBlank()) return "";
        String trimmed = output.trim();
        if (trimmed.length() <= maxChars) return trimmed;

        String[] lines = trimmed.split("\n");
        StringBuilder relevant = new StringBuilder();
        for (String line : lines) {
            String l = line.toLowerCase();
            if (l.contains("downloading") || l.contains("progress") || l.startsWith("---") || l.startsWith("[info] downloading")) {
                continue;
            }
            if (l.contains("fail") || l.contains("error") || l.contains("assert") ||
                l.contains("traceback") || l.contains("exception") || l.contains("syntaxerror") ||
                l.contains("typeerror") || l.contains("nameerror") || l.contains("cannot find") ||
                l.contains("not found") || l.contains("line ") || l.contains("failed:")) {
                relevant.append(line).append("\n");
                if (relevant.length() > maxChars) break;
            }
        }
        if (relevant.length() > 0) {
            return relevant.length() > maxChars ? relevant.substring(0, maxChars) + "..." : relevant.toString().trim();
        }
        return "..." + trimmed.substring(trimmed.length() - maxChars);
    }
}
