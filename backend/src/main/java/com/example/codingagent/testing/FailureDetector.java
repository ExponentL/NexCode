package com.example.codingagent.testing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes real command execution outputs, test results, compiler diagnostics, and runtime traces
 * to isolate failure types, affected source files, and root causes without fabricating data.
 */
@Component
public class FailureDetector {

    private static final Logger log = LoggerFactory.getLogger(FailureDetector.class);

    // Java compilation: [ERROR] /path/to/File.java:[line,col] message
    private static final Pattern JAVAC_ERROR_PATTERN = Pattern.compile("(?:\\[ERROR\\]\\s+)?([^\\s:]+\\.java):\\[(\\d+),(\\d+)\\]\\s+(.*)");

    // TypeScript compilation: src/file.ts(line,col): error TS...
    private static final Pattern TS_ERROR_PATTERN = Pattern.compile("([^\\s:]+\\.tsx?)\\((\\d+),(\\d+)\\):\\s+error\\s+(TS\\d+:\\s+.*)");

    // C/C++ compilation: file.cpp:line:col: error: ...
    private static final Pattern CPP_ERROR_PATTERN = Pattern.compile("([^\\s:]+\\.(?:c|cpp|cc|h|hpp)):(\\d+):(?:\\d+:)?\\s+error:\\s+(.*)");

    // Rust compilation: error[E0425]: ... --> src/main.rs:10:5
    private static final Pattern RUST_ERROR_PATTERN = Pattern.compile("error(?:\\[E\\d+\\])?:\\s+(.*)\\n\\s+-->\\s+([^\\s:]+\\.rs):(\\d+):(\\d+)");

    // Generic stack trace: at com.example.Class.method(File.java:42)
    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile("at\\s+([a-zA-Z0-9_\\.]+)\\.([a-zA-Z0-9_]+)\\(([^:]+\\.[a-zA-Z]+):(\\d+)\\)");

    // Python trace: File "path/to/file.py", line 42, in ...
    private static final Pattern PYTHON_TRACE_PATTERN = Pattern.compile("File\\s+\"([^\"]+\\.py)\",\\s+line\\s+(\\d+),");

    public FailureAnalysis analyzeTestResult(TestResult testResult) {
        if (testResult == null) {
            return new FailureAnalysis(
                    FailureType.UNKNOWN_ERROR,
                    "No test result provided for analysis",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    "Null TestResult",
                    "Execute tests to produce diagnostics."
            );
        }

        if (testResult.isSuccess()) {
            return new FailureAnalysis(
                    FailureType.UNKNOWN_ERROR,
                    "Tests passed successfully; no active failure detected.",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    "None",
                    "No remediation needed."
            );
        }

        if (!testResult.failures().isEmpty()) {
            List<String> affectedFiles = new ArrayList<>();
            List<String> keyLines = new ArrayList<>();
            for (TestFailureDetail f : testResult.failures()) {
                if (f.suiteName() != null && !f.suiteName().isBlank()) affectedFiles.add(f.suiteName());
                if (f.message() != null && !f.message().isBlank()) keyLines.add(f.message());
                if (f.errorTrace() != null && !f.errorTrace().isBlank()) {
                    affectedFiles.addAll(extractAffectedFiles(f.errorTrace()));
                }
            }
            String summary = testResult.failures().size() + " test assertion failure(s) in " +
                    (testResult.command() != null ? testResult.command() : "test suite");
            String rootCause = !keyLines.isEmpty() ? keyLines.getFirst() : "Assertion error";
            return new FailureAnalysis(
                    FailureType.TEST_FAILURE,
                    summary,
                    affectedFiles.stream().distinct().toList(),
                    keyLines,
                    rootCause,
                    "Examine failed test assertions and adjust business logic to satisfy expectations."
            );
        }

        String rawOutput = testResult.output();
        return analyze(rawOutput, testResult.exitCode(), "Test command: " + testResult.command());
    }

    public FailureAnalysis analyzeToolError(String toolName, String error, String output) {
        String combined = (error != null ? error : "") + "\n" + (output != null ? output : "");
        Set<String> affectedFiles = extractAffectedFiles(combined);
        List<String> keyLines = extractKeyErrorLines(combined, 5);

        if (error != null && (error.contains("Path traversal") || error.contains("SecurityGuard") || error.contains("Dangerous command"))) {
            return new FailureAnalysis(
                    FailureType.TOOL_ERROR,
                    "Tool safety violation: " + error,
                    new ArrayList<>(affectedFiles),
                    keyLines,
                    "Security policy constraint blocked action",
                    "Adjust target paths to stay within repository workspace and avoid prohibited commands."
            );
        }

        return new FailureAnalysis(
                FailureType.TOOL_ERROR,
                "Tool execution failed for " + toolName + ": " + (error != null ? error : "Unknown tool error"),
                new ArrayList<>(affectedFiles),
                keyLines,
                error != null ? error : "Tool failed",
                "Review tool arguments and repository filesystem permissions."
        );
    }

    public FailureAnalysis analyzeCompilationError(String output) {
        return analyze(output, 1, "compilation");
    }

    public FailureAnalysis analyze(String output, int exitCode, String context) {
        if (output == null || output.isBlank()) {
            return new FailureAnalysis(
                    FailureType.COMMAND_ERROR,
                    "Command terminated with non-zero exit code (" + exitCode + ") but produced no output.",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    "Process failure with exit code " + exitCode,
                    "Check command syntax and environment binaries."
            );
        }

        Set<String> affectedFiles = extractAffectedFiles(output);
        List<String> keyLines = extractKeyErrorLines(output, 6);

        // 1. Check for compilation errors
        if (isCompilationError(output)) {
            String rootCause = findCompilationRootCause(output, keyLines);
            return new FailureAnalysis(
                    FailureType.COMPILATION_ERROR,
                    "Compilation failed across " + (!affectedFiles.isEmpty() ? affectedFiles.size() : "unresolved") + " source file(s)",
                    new ArrayList<>(affectedFiles),
                    keyLines,
                    rootCause,
                    "Inspect affected source files at reported line numbers and fix syntax/type/import errors."
            );
        }

        // 2. Check for test assertion failures
        if (isTestFailure(output)) {
            String rootCause = findTestFailureRootCause(output, keyLines);
            return new FailureAnalysis(
                    FailureType.TEST_FAILURE,
                    "Automated tests failed: " + rootCause,
                    new ArrayList<>(affectedFiles),
                    keyLines,
                    rootCause,
                    "Examine failed test assertions and adjust business logic or test fixtures to satisfy expectations."
            );
        }

        // 3. Check for runtime exceptions
        if (isRuntimeError(output)) {
            String rootCause = findRuntimeRootCause(output, keyLines);
            return new FailureAnalysis(
                    FailureType.RUNTIME_ERROR,
                    "Runtime error encountered: " + rootCause,
                    new ArrayList<>(affectedFiles),
                    keyLines,
                    rootCause,
                    "Check for null references, missing runtime configurations, or unhandled exceptions."
            );
        }

        // 4. Fallback to generic command error
        return new FailureAnalysis(
                FailureType.COMMAND_ERROR,
                "Command failed with exit code " + exitCode,
                new ArrayList<>(affectedFiles),
                keyLines,
                !keyLines.isEmpty() ? keyLines.get(0) : "Non-zero exit code: " + exitCode,
                "Verify dependencies, working directory path, and command flags."
        );
    }

    public Set<String> extractAffectedFiles(String output) {
        Set<String> files = new LinkedHashSet<>();
        if (output == null) return files;

        // Java errors
        Matcher jm = JAVAC_ERROR_PATTERN.matcher(output);
        while (jm.find()) {
            files.add(normalizeFilePath(jm.group(1)));
        }

        // TypeScript errors
        Matcher tm = TS_ERROR_PATTERN.matcher(output);
        while (tm.find()) {
            files.add(normalizeFilePath(tm.group(1)));
        }

        // C/C++ errors
        Matcher cm = CPP_ERROR_PATTERN.matcher(output);
        while (cm.find()) {
            files.add(normalizeFilePath(cm.group(1)));
        }

        // Rust errors
        Matcher rm = RUST_ERROR_PATTERN.matcher(output);
        while (rm.find()) {
            files.add(normalizeFilePath(rm.group(2)));
        }

        // Python trace
        Matcher pyM = PYTHON_TRACE_PATTERN.matcher(output);
        while (pyM.find()) {
            files.add(normalizeFilePath(pyM.group(1)));
        }

        // Stack traces
        Matcher stM = STACK_TRACE_PATTERN.matcher(output);
        while (stM.find()) {
            String fileName = stM.group(3);
            if (!fileName.contains("NativeMethodAccessorImpl") && !fileName.contains("Method.java")) {
                files.add(normalizeFilePath(fileName));
            }
        }

        return files;
    }

    private String normalizeFilePath(String raw) {
        String trimmed = raw.trim();
        // Remove leading prefixes like "[ERROR] " or quotes
        if (trimmed.startsWith("[ERROR]")) {
            trimmed = trimmed.substring(7).trim();
        }
        return trimmed.replace('\\', '/');
    }

    private boolean isCompilationError(String output) {
        return output.contains("COMPILATION ERROR") ||
                output.contains("Compilation failure") ||
                output.contains("error TS") ||
                output.contains(": error:") ||
                output.contains("SyntaxError:") ||
                output.contains("javac [debug");
    }

    private boolean isTestFailure(String output) {
        return output.contains("There are test failures") ||
                output.contains("AssertionError") ||
                output.contains("AssertionFailedError") ||
                output.contains("FAIL ") ||
                output.contains("Tests:       ") ||
                output.contains("Failures: ") ||
                output.contains("--- FAIL:");
    }

    private boolean isRuntimeError(String output) {
        return output.contains("Exception in thread") ||
                output.contains("NullPointerException") ||
                output.contains("ClassNotFoundException") ||
                output.contains("NoSuchMethodError") ||
                output.contains("panic:") ||
                output.contains("Segmentation fault");
    }

    private List<String> extractKeyErrorLines(String output, int maxLines) {
        List<String> keyLines = new ArrayList<>();
        String[] lines = output.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("[ERROR]") ||
                    trimmed.contains("error:") ||
                    trimmed.contains("error TS") ||
                    trimmed.contains("FAILED") ||
                    trimmed.contains("Exception:") ||
                    trimmed.contains("Error:") ||
                    trimmed.startsWith("FAIL") ||
                    trimmed.contains("AssertionFailedError")) {
                keyLines.add(trimmed);
                if (keyLines.size() >= maxLines) break;
            }
        }
        return keyLines;
    }

    private String findCompilationRootCause(String output, List<String> keyLines) {
        for (String line : keyLines) {
            if (line.contains("cannot find symbol") ||
                    line.contains("incompatible types") ||
                    line.contains("not supported") ||
                    line.contains("error TS") ||
                    line.contains("error:")) {
                return line;
            }
        }
        return !keyLines.isEmpty() ? keyLines.get(0) : "Compilation failure";
    }

    private String findTestFailureRootCause(String output, List<String> keyLines) {
        for (String line : keyLines) {
            if (line.contains("expected:") ||
                    line.contains("AssertionFailedError") ||
                    line.contains("AssertionError") ||
                    line.contains("FAIL")) {
                return line;
            }
        }
        return !keyLines.isEmpty() ? keyLines.get(0) : "Test assertion failure";
    }

    private String findRuntimeRootCause(String output, List<String> keyLines) {
        for (String line : keyLines) {
            if (line.contains("Exception") || line.contains("Error")) {
                return line;
            }
        }
        return !keyLines.isEmpty() ? keyLines.get(0) : "Runtime exception";
    }
}
