package com.example.codingagent.testing;

import com.example.codingagent.tools.RepositoryTool;
import com.example.codingagent.tools.TerminalTool;
import com.example.codingagent.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes real automated test commands against the target repository and parses actual outcomes.
 * STRICT RULE: Test metrics are ALWAYS extracted from real execution output, never simulated.
 */
@Component
public class TestRunner {

    private static final Logger log = LoggerFactory.getLogger(TestRunner.class);

    private final TerminalTool terminalTool;
    private final RepositoryTool repositoryTool;
    private final com.example.codingagent.tools.TestTool testTool;
    private final com.example.codingagent.tools.ExecutableVerifier executableVerifier;

    public TestRunner(TerminalTool terminalTool, RepositoryTool repositoryTool) {
        this(terminalTool, repositoryTool, new com.example.codingagent.tools.TestTool(terminalTool, repositoryTool), new com.example.codingagent.tools.ExecutableVerifier());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TestRunner(
            TerminalTool terminalTool,
            RepositoryTool repositoryTool,
            com.example.codingagent.tools.TestTool testTool,
            com.example.codingagent.tools.ExecutableVerifier executableVerifier) {
        this.terminalTool = terminalTool;
        this.repositoryTool = repositoryTool;
        this.testTool = testTool != null ? testTool : new com.example.codingagent.tools.TestTool(terminalTool, repositoryTool);
        this.executableVerifier = executableVerifier != null ? executableVerifier : new com.example.codingagent.tools.ExecutableVerifier();
    }

    /**
     * Executes the repository test suite and returns structured TestResult.
     */
    public TestResult runTests(String workingDirectory, String explicitCommand, String filter) {
        File dir = new File(workingDirectory);
        if (!dir.exists() || !dir.isDirectory()) {
            return TestResult.error(
                    explicitCommand != null ? explicitCommand : "unknown",
                    "Working directory does not exist or is not a directory: " + workingDirectory,
                    0L,
                    "Unknown"
            );
        }

        String projectType = repositoryTool.detectProjectType(dir);
        String command = (explicitCommand != null && !explicitCommand.isBlank())
                ? explicitCommand
                : detectTestCommand(workingDirectory, filter);

        if (command == null || command.isBlank()) {
            return TestResult.noTests(
                    "No test suite detected for project type: " + projectType,
                    projectType
            );
        }

        log.info("TestRunner executing: '{}' in {} (type: {})", command, workingDirectory, projectType);
        long startTime = System.currentTimeMillis();

        ToolResult execution = terminalTool.execute(
                Map.of("command", command, "timeoutSeconds", 180),
                workingDirectory
        );

        long durationMs = System.currentTimeMillis() - startTime;
        String stdout = execution.output() != null ? execution.output() : "";
        String stderr = execution.error() != null ? execution.error() : "";
        int exitCode = execution.metadata() != null && execution.metadata().get("exitCode") instanceof Number n
                ? n.intValue()
                : (execution.success() ? 0 : 1);

        return parseTestOutput(command, exitCode, stdout, stderr, durationMs, projectType);
    }

    /**
     * Detects the real native test command for the repository structure.
     */
    public String detectTestCommand(String workingDirectory, String filter) {
        return testTool.detectTestCommand(workingDirectory, filter);
    }

    /**
     * Parses real process output to extract test metrics and failure details.
     * If the output does not conform to recognized test metrics, preserves raw output without inventing numbers.
     */
    public TestResult parseTestOutput(
            String command,
            int exitCode,
            String stdout,
            String stderr,
            long durationMs,
            String projectType) {

        String combined = (stdout != null ? stdout : "") + "\n" + (stderr != null ? stderr : "");

        // 0. Handle command not found / missing executable
        if (exitCode == 127) {
            String missingExecutable = executableVerifier.extractExecutable(command);
            String detail = (stderr != null && !stderr.isBlank())
                    ? stderr.trim()
                    : "Command not found (exit code 127): Executable '" + missingExecutable + "' is not installed or available on PATH.";
            log.warn("TestRunner execution failed with exit code 127: {}", detail);
            return TestResult.commandNotFound(command, stdout, detail, durationMs, projectType);
        }

        // 1. Try Maven Surefire
        TestResult mavenResult = tryParseMavenSurefire(command, exitCode, stdout, stderr, durationMs, projectType);
        if (mavenResult != null) return mavenResult;

        // 2. Try Jest / Vitest / Mocha
        TestResult jestResult = tryParseJestVitest(command, exitCode, stdout, stderr, durationMs, projectType);
        if (jestResult != null) return jestResult;

        // 3. Try Gradle
        TestResult gradleResult = tryParseGradle(command, exitCode, stdout, stderr, durationMs, projectType);
        if (gradleResult != null) return gradleResult;

        // 4. Try .NET
        TestResult dotnetResult = tryParseDotnet(command, exitCode, stdout, stderr, durationMs, projectType);
        if (dotnetResult != null) return dotnetResult;

        // 5. Try Pytest
        TestResult pytestResult = tryParsePytest(command, exitCode, stdout, stderr, durationMs, projectType);
        if (pytestResult != null) return pytestResult;

        // 5b. Try Python unittest
        TestResult unittestResult = tryParsePythonUnittest(command, exitCode, stdout, stderr, durationMs, projectType);
        if (unittestResult != null) return unittestResult;

        // 6. Try Go test
        TestResult goResult = tryParseGo(command, exitCode, stdout, stderr, durationMs, projectType);
        if (goResult != null) return goResult;

        // Unparseable output: DO NOT INVENT NUMBERS. Preserve raw output.
        boolean success = exitCode == 0;
        return TestResult.unparseable(command, success, durationMs, stdout, stderr, exitCode, projectType);
    }

    private TestResult tryParsePythonUnittest(
            String command, int exitCode, String stdout, String stderr, long durationMs, String projectType) {
        String combined = (stdout != null ? stdout : "") + "\n" + (stderr != null ? stderr : "");
        Pattern ranPattern = Pattern.compile("Ran\\s+(\\d+)\\s+tests?\\s+in\\s+[\\d\\.]+s");
        Matcher rm = ranPattern.matcher(combined);
        if (!rm.find()) {
            if (combined.contains("NO TESTS RAN") || (exitCode == 5 && combined.contains("Ran 0 tests"))) {
                return TestResult.noTests("No test cases found in Python repository.", projectType);
            }
            return null;
        }

        int total = Integer.parseInt(rm.group(1));
        if (total == 0) {
            return TestResult.noTests("No test cases found in Python repository.", projectType);
        }

        int failures = 0;
        int errors = 0;
        Matcher failM = Pattern.compile("FAILED\\s*\\((?:failures=(\\d+))?[,\\s]*(?:errors=(\\d+))?\\)").matcher(combined);
        if (failM.find()) {
            if (failM.group(1) != null) failures = Integer.parseInt(failM.group(1));
            if (failM.group(2) != null) errors = Integer.parseInt(failM.group(2));
        }

        int failed = failures + errors;
        int passed = Math.max(0, total - failed);
        boolean success = (exitCode == 0) && (failed == 0);

        List<TestFailureDetail> failureDetails = new ArrayList<>();
        if (failed > 0) {
            Pattern errorPattern = Pattern.compile("(FAIL|ERROR):\\s+([a-zA-Z0-9_]+)\\s*\\(([^\\)]+)\\)");
            Matcher em = errorPattern.matcher(combined);
            while (em.find()) {
                String testName = em.group(2);
                String suiteName = em.group(3);
                failureDetails.add(TestFailureDetail.of(suiteName, testName, "Test " + em.group(1).toLowerCase() + "ed", ""));
            }
        }

        return success
                ? TestResult.success(command, total, passed, 0, durationMs, stdout, stderr, exitCode, projectType)
                : TestResult.failure(command, total, passed, failed, 0, durationMs, stdout, stderr, exitCode, failureDetails, projectType);
    }

    private TestResult tryParseMavenSurefire(
            String command, int exitCode, String stdout, String stderr, long durationMs, String projectType) {
        String combined = stdout + "\n" + stderr;
        Pattern pattern = Pattern.compile("Tests run:\\s*(\\d+),\\s*Failures:\\s*(\\d+),\\s*Errors:\\s*(\\d+),\\s*Skipped:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(combined);

        int total = 0, failuresCount = 0, errorsCount = 0, skippedCount = 0;
        boolean found = false;

        while (matcher.find()) {
            total = Integer.parseInt(matcher.group(1));
            failuresCount = Integer.parseInt(matcher.group(2));
            errorsCount = Integer.parseInt(matcher.group(3));
            skippedCount = Integer.parseInt(matcher.group(4));
            found = true;
        }

        if (!found) return null;

        int failed = failuresCount + errorsCount;
        int passed = Math.max(0, total - failed - skippedCount);
        boolean success = exitCode == 0 && failed == 0;

        List<TestFailureDetail> failureDetails = extractMavenFailures(combined);

        return success
                ? TestResult.success(command, total, passed, skippedCount, durationMs, stdout, stderr, exitCode, projectType)
                : TestResult.failure(command, total, passed, failed, skippedCount, durationMs, stdout, stderr, exitCode, failureDetails, projectType);
    }

    private List<TestFailureDetail> extractMavenFailures(String output) {
        List<TestFailureDetail> details = new ArrayList<>();
        // Look for: [ERROR] Failures: or [ERROR] Errors: followed by test names
        Pattern failureLinePattern = Pattern.compile("\\[ERROR\\]\\s+([a-zA-Z0-9_\\.]+)\\.([a-zA-Z0-9_]+):?([^\n]*)");
        Matcher m = failureLinePattern.matcher(output);
        while (m.find()) {
            String suite = m.group(1);
            String test = m.group(2);
            String msg = m.group(3).trim();
            details.add(TestFailureDetail.of(suite, test, msg, ""));
        }
        return details;
    }

    private TestResult tryParseJestVitest(
            String command, int exitCode, String stdout, String stderr, long durationMs, String projectType) {
        String combined = stdout + "\n" + stderr;
        Pattern pattern = Pattern.compile("Tests:\\s*(?:(\\d+)\\s*failed,)?\\s*(?:(\\d+)\\s*passed,)?\\s*(?:(\\d+)\\s*skipped,)?\\s*(\\d+)\\s*total");
        Matcher m = pattern.matcher(combined);

        if (!m.find()) return null;

        int failed = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
        int passed = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
        int skipped = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
        int total = Integer.parseInt(m.group(4));

        boolean success = exitCode == 0 && failed == 0;
        List<TestFailureDetail> failureDetails = new ArrayList<>();
        if (failed > 0) {
            Pattern failPattern = Pattern.compile("FAIL\\s+([^\\n]+)");
            Matcher fm = failPattern.matcher(combined);
            while (fm.find()) {
                failureDetails.add(TestFailureDetail.of("JestSuite", fm.group(1).trim(), "Test suite failed", ""));
            }
        }

        return success
                ? TestResult.success(command, total, passed, skipped, durationMs, stdout, stderr, exitCode, projectType)
                : TestResult.failure(command, total, passed, failed, skipped, durationMs, stdout, stderr, exitCode, failureDetails, projectType);
    }

    private TestResult tryParseGradle(
            String command, int exitCode, String stdout, String stderr, long durationMs, String projectType) {
        String combined = stdout + "\n" + stderr;
        Pattern pattern = Pattern.compile("(\\d+)\\s+tests?\\s+completed,\\s*(\\d+)\\s+failed,\\s*(\\d+)\\s+skipped");
        Matcher m = pattern.matcher(combined);

        if (!m.find()) return null;

        int total = Integer.parseInt(m.group(1));
        int failed = Integer.parseInt(m.group(2));
        int skipped = Integer.parseInt(m.group(3));
        int passed = Math.max(0, total - failed - skipped);
        boolean success = exitCode == 0 && failed == 0;

        return success
                ? TestResult.success(command, total, passed, skipped, durationMs, stdout, stderr, exitCode, projectType)
                : TestResult.failure(command, total, passed, failed, skipped, durationMs, stdout, stderr, exitCode, Collections.emptyList(), projectType);
    }

    private TestResult tryParseDotnet(
            String command, int exitCode, String stdout, String stderr, long durationMs, String projectType) {
        String combined = stdout + "\n" + stderr;
        Matcher totM = Pattern.compile("Total:\\s*(\\d+)").matcher(combined);
        Matcher passM = Pattern.compile("Passed:\\s*(\\d+)").matcher(combined);
        Matcher failM = Pattern.compile("Failed:\\s*(\\d+)").matcher(combined);
        Matcher skipM = Pattern.compile("Skipped:\\s*(\\d+)").matcher(combined);

        if (!totM.find() || (!passM.find() && !failM.find())) return null;

        int total = Integer.parseInt(totM.group(1));
        passM.reset();
        int passed = passM.find() ? Integer.parseInt(passM.group(1)) : 0;
        failM.reset();
        int failed = failM.find() ? Integer.parseInt(failM.group(1)) : 0;
        skipM.reset();
        int skipped = skipM.find() ? Integer.parseInt(skipM.group(1)) : 0;

        boolean success = exitCode == 0 && failed == 0;

        return success
                ? TestResult.success(command, total, passed, skipped, durationMs, stdout, stderr, exitCode, projectType)
                : TestResult.failure(command, total, passed, failed, skipped, durationMs, stdout, stderr, exitCode, Collections.emptyList(), projectType);
    }

    private TestResult tryParsePytest(
            String command, int exitCode, String stdout, String stderr, long durationMs, String projectType) {
        String combined = stdout + "\n" + stderr;
        Pattern pattern = Pattern.compile("=+\\s*(?:(\\d+)\\s*failed,)?\\s*(?:(\\d+)\\s*passed,)?\\s*(?:(\\d+)\\s*skipped,)?.*in\\s+[\\d\\.]+s\\s*=+", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(combined);

        if (!m.find()) return null;

        int failed = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
        int passed = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
        int skipped = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
        int total = failed + passed + skipped;

        boolean success = exitCode == 0 && failed == 0;

        return success
                ? TestResult.success(command, total, passed, skipped, durationMs, stdout, stderr, exitCode, projectType)
                : TestResult.failure(command, total, passed, failed, skipped, durationMs, stdout, stderr, exitCode, Collections.emptyList(), projectType);
    }

    private TestResult tryParseGo(
            String command, int exitCode, String stdout, String stderr, long durationMs, String projectType) {
        String combined = stdout + "\n" + stderr;
        if (!combined.contains("--- PASS:") && !combined.contains("--- FAIL:")) {
            return null;
        }

        int passed = 0, failed = 0;
        Matcher pm = Pattern.compile("--- PASS:\\s*([A-Za-z0-9_]+)").matcher(combined);
        while (pm.find()) passed++;

        List<TestFailureDetail> failures = new ArrayList<>();
        Matcher fm = Pattern.compile("--- FAIL:\\s*([A-Za-z0-9_]+)").matcher(combined);
        while (fm.find()) {
            failed++;
            failures.add(TestFailureDetail.of("GoPackage", fm.group(1), "Test failed", ""));
        }

        int total = passed + failed;
        boolean success = exitCode == 0 && failed == 0;

        return success
                ? TestResult.success(command, total, passed, 0, durationMs, stdout, stderr, exitCode, projectType)
                : TestResult.failure(command, total, passed, failed, 0, durationMs, stdout, stderr, exitCode, failures, projectType);
    }
}
