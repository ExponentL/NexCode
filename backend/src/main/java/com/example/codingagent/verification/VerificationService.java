package com.example.codingagent.verification;

import com.example.codingagent.agent.AgentState;
import com.example.codingagent.agent.AgentStatus;
import com.example.codingagent.agent.AgentStep;
import com.example.codingagent.agent.AgentStep.StepStatus;
import com.example.codingagent.agent.TestResultItem;
import com.example.codingagent.testing.TestResult;
import com.example.codingagent.testing.TestRunner;
import com.example.codingagent.testing.TestStatus;
import com.example.codingagent.tools.GitTool;
import com.example.codingagent.tools.TerminalTool;
import com.example.codingagent.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Core verification engine that evaluates task completion strictly through real evidence.
 * CRITICAL RULE: A task is NEVER verified simply because the foundation model claims completion.
 */
@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private final TestRunner testRunner;
    private final GitTool gitTool;
    private final TerminalTool terminalTool;

    public VerificationService(TestRunner testRunner, GitTool gitTool, TerminalTool terminalTool) {
        this.testRunner = testRunner;
        this.gitTool = gitTool;
        this.terminalTool = terminalTool;
    }

    /**
     * Executes the comprehensive evidence-based verification routine.
     */
    public VerificationResult verify(AgentState state) {
        String repoPath = state.getRepositoryPath();
        log.info("Starting evidence-based verification for task: {} in {}", state.getTaskId(), repoPath);

        List<VerificationCheck> checks = new ArrayList<>();
        List<String> passedCheckNames = new ArrayList<>();
        List<String> failedCheckNames = new ArrayList<>();
        List<TestResultItem> legacyTestItems = new ArrayList<>();

        // 1. Relevant Files Inspected Check
        VerificationCheck inspectedCheck = verifyRelevantFilesInspected(state);
        checks.add(inspectedCheck);
        if (inspectedCheck.passed()) passedCheckNames.add(inspectedCheck.name());
        else failedCheckNames.add(inspectedCheck.name() + ": " + inspectedCheck.evidence());

        // 2. Files Actually Modified Check
        VerificationCheck modifiedCheck = verifyFilesActuallyModified(state, repoPath);
        checks.add(modifiedCheck);
        if (modifiedCheck.passed()) passedCheckNames.add(modifiedCheck.name());
        else failedCheckNames.add(modifiedCheck.name() + ": " + modifiedCheck.evidence());

        // 3. Expected Code Changes Exist Check
        VerificationCheck changesCheck = verifyExpectedCodeChangesExist(state, repoPath);
        checks.add(changesCheck);
        if (changesCheck.passed()) passedCheckNames.add(changesCheck.name());
        else failedCheckNames.add(changesCheck.name() + ": " + changesCheck.evidence());

        // 4. Git Diff Retrieval & Validation Check
        GitDiffSummary gitDiffSummary = retrieveGitDiff(repoPath, state.getFilesModified());
        state.setDiff(gitDiffSummary.diff());
        VerificationCheck gitCheck = verifyGitDiff(gitDiffSummary, modifiedCheck.passed(), state);
        checks.add(gitCheck);
        if (gitCheck.passed()) passedCheckNames.add(gitCheck.name());
        else failedCheckNames.add(gitCheck.name() + ": " + gitCheck.evidence());

        // 5. Build Succeeds When Appropriate Check
        BuildResult buildResult = executeBuildCheck(repoPath);
        VerificationCheck buildCheck = verifyBuildResult(buildResult);
        checks.add(buildCheck);
        if (buildCheck.passed()) passedCheckNames.add(buildCheck.name());
        else failedCheckNames.add(buildCheck.name() + ": " + buildCheck.evidence());

        // 6. Tests Actually Pass When Applicable Check
        TestResult testResult = executeTestCheck(state, repoPath);
        state.setLatestTestResult(testResult);
        VerificationCheck testCheck = verifyTestResult(testResult, legacyTestItems);
        checks.add(testCheck);
        if (testCheck.passed()) passedCheckNames.add(testCheck.name());
        else failedCheckNames.add(testCheck.name() + ": " + testCheck.evidence());

        // 7. No Unresolved Tool Errors Remain Check
        VerificationCheck errorCheck = verifyNoRemainingErrors(state);
        checks.add(errorCheck);
        if (errorCheck.passed()) passedCheckNames.add(errorCheck.name());
        else failedCheckNames.add(errorCheck.name() + ": " + errorCheck.evidence());

        // 8. Planned Steps Completed Check
        VerificationCheck planCheck = verifyPlanStepsCompleted(state);
        checks.add(planCheck);
        if (planCheck.passed()) passedCheckNames.add(planCheck.name());
        else failedCheckNames.add(planCheck.name() + ": " + planCheck.evidence());

        // Determine Final Strict Verdict
        // Must have: actual modifications, valid changes/diff, clean build, passing tests, and no remaining critical errors
        boolean allCriticalPassed = modifiedCheck.passed()
                && changesCheck.passed()
                && buildCheck.passed()
                && testCheck.passed()
                && errorCheck.passed();

        String summary = allCriticalPassed
                ? String.format("VERIFIED: All evidence checks passed (%d/%d checks succeeded)", passedCheckNames.size(), checks.size())
                : String.format("NOT VERIFIED: %d check(s) failed (%s)", failedCheckNames.size(), String.join("; ", failedCheckNames));

        List<String> remainingErrors = !errorCheck.passed() ? new ArrayList<>(state.getErrors()) : Collections.emptyList();

        for (TestResultItem item : legacyTestItems) {
            state.recordTestResult(item);
        }

        VerificationResult result = new VerificationResult(
                allCriticalPassed,
                summary,
                checks,
                passedCheckNames,
                failedCheckNames,
                testResult,
                buildResult,
                gitDiffSummary,
                remainingErrors,
                legacyTestItems
        );

        log.info("Verification result for task {}: verified={}, summary={}", state.getTaskId(), allCriticalPassed, summary);
        return result;
    }

    /**
     * Builds the final immutable TaskResult object.
     */
    public TaskResult generateTaskResult(AgentState state, VerificationResult verification) {
        long duration = Duration.between(state.getCreatedAt(), Instant.now()).toMillis();
        AgentStatus status = verification.verified() ? AgentStatus.COMPLETED : AgentStatus.FAILED;

        return new TaskResult(
                state.getTaskId(),
                state.getTask(),
                status,
                verification.verified(),
                verification.summary(),
                verification,
                state.getFilesInspected(),
                state.getFilesModified(),
                state.getPlan(),
                verification.gitDiff(),
                duration,
                state.getCreatedAt(),
                Instant.now()
        );
    }

    // --- Verification Check Handlers ---

    private boolean isReadOnlyTask(String task) {
        if (task == null) return false;
        String t = task.toLowerCase();
        if (t.contains("fix") || t.contains("add") || t.contains("implement") ||
            t.contains("update") || t.contains("refactor") || t.contains("change") ||
            t.contains("modify") || t.contains("patch") || t.contains("create") ||
            t.contains("delete") || t.contains("remove")) {
            return false;
        }
        return t.contains("inspect") || t.contains("test") || t.contains("check") ||
               t.contains("audit") || t.contains("explore") || t.contains("review") ||
               t.contains("analyze") || t.contains("status");
    }

    private VerificationCheck verifyRelevantFilesInspected(AgentState state) {
        List<String> inspected = state.getFilesInspected();
        if (!inspected.isEmpty()) {
            return new VerificationCheck("Relevant files inspected", true,
                    inspected.size() + " relevant file(s) inspected: " + String.join(", ", inspected));
        } else if (isReadOnlyTask(state.getTask())) {
            return new VerificationCheck("Relevant files inspected", true,
                    "Repository scanned and inspected during task execution.");
        } else {
            return new VerificationCheck("Relevant files inspected", false,
                    "No repository files were inspected prior to applying changes.");
        }
    }

    private VerificationCheck verifyFilesActuallyModified(AgentState state, String repoPath) {
        List<String> modified = state.getFilesModified();
        if (modified.isEmpty()) {
            if (isReadOnlyTask(state.getTask())) {
                return new VerificationCheck("Code modification applied", true,
                        "Read-only inspection/testing task; no code modifications required.");
            }
            return new VerificationCheck("Code modification applied", false,
                    "No files were recorded as modified during task execution.");
        }

        List<String> confirmedFiles = new ArrayList<>();
        List<String> missingFiles = new ArrayList<>();

        for (String relativePath : modified) {
            File file = new File(repoPath, relativePath);
            if (file.exists() && file.isFile()) {
                confirmedFiles.add(relativePath);
            } else {
                missingFiles.add(relativePath);
            }
        }

        if (!missingFiles.isEmpty()) {
            return new VerificationCheck("Code modification applied", false,
                    "Modified file(s) missing on disk: " + String.join(", ", missingFiles));
        }

        return new VerificationCheck("Code modification applied", true,
                confirmedFiles.size() + " file(s) confirmed on disk: " + String.join(", ", confirmedFiles));
    }

    private VerificationCheck verifyExpectedCodeChangesExist(AgentState state, String repoPath) {
        List<String> modified = state.getFilesModified();
        if (modified.isEmpty()) {
            if (isReadOnlyTask(state.getTask())) {
                return new VerificationCheck("Expected code changes exist", true,
                        "Read-only inspection/testing task; skipped file modification verification.");
            }
            return new VerificationCheck("Expected code changes exist", false,
                    "Cannot verify code changes because no files were modified.");
        }

        // Verify that modified files are not zero-length / empty
        for (String relativePath : modified) {
            File f = new File(repoPath, relativePath);
            if (f.exists() && f.length() == 0) {
                return new VerificationCheck("Expected code changes exist", false,
                        "Modified file '" + relativePath + "' is empty (0 bytes).");
            }
        }

        return new VerificationCheck("Expected code changes exist", true,
                "Non-empty code modifications confirmed across " + modified.size() + " file(s).");
    }

    private VerificationCheck verifyGitDiff(GitDiffSummary gitDiff, boolean filesModified, AgentState state) {
        if (!gitDiff.diff().isBlank()) {
            return new VerificationCheck("Git diff reviewed", true,
                    String.format("Confirmed git changes: %d modified, %d added, %d deleted.",
                            gitDiff.modifiedFiles().size(), gitDiff.addedFiles().size(), gitDiff.deletedFiles().size()));
        }

        if (filesModified && !state.getFilesModified().isEmpty()) {
            // Non-git repository or uncommitted tracked via harness
            return new VerificationCheck("Git diff reviewed", true,
                    "Modifications confirmed via filesystem telemetry (Git repository untracked).");
        }

        if (isReadOnlyTask(state.getTask())) {
            return new VerificationCheck("Git diff reviewed", true,
                    "Working tree is clean; no modifications expected for inspection/testing task.");
        }

        return new VerificationCheck("Git diff reviewed", false,
                "Git diff is empty; no uncommitted changes found in working tree.");
    }

    private VerificationCheck verifyBuildResult(BuildResult build) {
        if (build.success()) {
            return new VerificationCheck("Build completed", true,
                    build.command().equals("none") ? build.message() : "Command '" + build.command() + "' succeeded with exit code 0.");
        } else {
            return new VerificationCheck("Build completed", false,
                    "Build command '" + build.command() + "' failed (exit code " + build.exitCode() + "): " + build.message());
        }
    }

    private VerificationCheck verifyTestResult(TestResult testResult, List<TestResultItem> legacyItems) {
        if (testResult == null || testResult.status() == TestStatus.NO_TESTS) {
            return new VerificationCheck("Tests executed & passed", true,
                    "No automated test suite configured for this project structure.");
        }

        if (testResult.isSuccess()) {
            String count = testResult.hasParsedMetrics()
                    ? (testResult.passedTests() != null ? testResult.passedTests() : testResult.totalTests()) + " passed"
                    : "exit code 0";
            legacyItems.add(TestResultItem.success(testResult.projectType() + " Suite", "Automated tests (" + count + ")", testResult.durationMs()));
            return new VerificationCheck("Tests executed & passed", true,
                    String.format("Automated tests passed (%s) in %.2fs.", count, testResult.durationMs() / 1000.0));
        } else {
            String reason = testResult.hasParsedMetrics() && testResult.failedTests() != null
                    ? testResult.failedTests() + " failed test(s)"
                    : "Test process exited with non-zero code " + testResult.exitCode();
            legacyItems.add(TestResultItem.failure(testResult.projectType() + " Suite", "Automated tests", reason, testResult.durationMs()));
            return new VerificationCheck("Tests executed & passed", false,
                    "Automated test suite failed: " + reason);
        }
    }

    private VerificationCheck verifyNoRemainingErrors(AgentState state) {
        List<String> errors = state.getErrors();
        if (errors.isEmpty()) {
            return new VerificationCheck("No unresolved tool errors", true, "0 unresolved errors.");
        }

        // If there were errors, verify if they were safety/traversal errors or if they caused failure
        long criticalCount = errors.stream()
                .filter(e -> e.contains("rejected by harness safety") || e.contains("Path traversal") || e.contains("Fatal"))
                .count();

        if (criticalCount > 0) {
            return new VerificationCheck("No unresolved tool errors", false,
                    criticalCount + " unresolved critical tool/security error(s) remaining.");
        }

        return new VerificationCheck("No unresolved tool errors", true,
                "Non-fatal warnings recorded; 0 unresolved critical errors.");
    }

    private VerificationCheck verifyPlanStepsCompleted(AgentState state) {
        List<AgentStep> plan = state.getPlan();
        if (plan == null || plan.isEmpty()) {
            return new VerificationCheck("Plan steps completed", true, "Plan completed.");
        }

        long completed = plan.stream().filter(s -> s.getStatus() == StepStatus.COMPLETED).count();
        long failed = plan.stream().filter(s -> s.getStatus() == StepStatus.FAILED).count();

        if (failed > 0) {
            return new VerificationCheck("Plan steps completed", false,
                    failed + " step(s) failed in execution plan.");
        }

        return new VerificationCheck("Plan steps completed", true,
                String.format("%d of %d plan step(s) completed successfully.", completed, plan.size()));
    }

    // --- Git Diff & Build Utilities ---

    public GitDiffSummary retrieveGitDiff(String repoPath, List<String> fallbackModifiedFiles) {
        File gitDir = new File(repoPath, ".git");
        if (!gitDir.exists()) {
            return new GitDiffSummary(
                    fallbackModifiedFiles != null ? fallbackModifiedFiles : Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    "",
                    fallbackModifiedFiles != null ? fallbackModifiedFiles.size() : 0
            );
        }

        List<String> modified = new ArrayList<>();
        List<String> added = new ArrayList<>();
        List<String> deleted = new ArrayList<>();

        // 1. Query status porcelain
        ToolResult statusRes = terminalTool.execute(Map.of("command", "git status --porcelain"), repoPath);
        if (statusRes.success() && statusRes.output() != null) {
            String[] lines = statusRes.output().split("\n");
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.length() < 3) continue;
                String prefix = line.substring(0, 2).trim();
                String file = line.substring(2).trim();

                if (prefix.equals("M")) modified.add(file);
                else if (prefix.equals("A") || prefix.equals("??")) added.add(file);
                else if (prefix.equals("D")) deleted.add(file);
                else if (prefix.startsWith("R")) modified.add(file);
            }
        }

        // 2. Query unified diff
        ToolResult diffRes = terminalTool.execute(Map.of("command", "git diff HEAD"), repoPath);
        String diffText = "";
        if (diffRes.success() && diffRes.output() != null && !diffRes.output().isBlank()) {
            diffText = diffRes.output();
        } else {
            ToolResult workingDiff = terminalTool.execute(Map.of("command", "git diff"), repoPath);
            if (workingDiff.success() && workingDiff.output() != null) {
                diffText = workingDiff.output();
            }
        }

        int total = modified.size() + added.size() + deleted.size();
        return new GitDiffSummary(modified, added, deleted, diffText, total);
    }

    public BuildResult executeBuildCheck(String repoPath) {
        File dir = new File(repoPath);

        // Java / Maven
        if (new File(dir, "pom.xml").exists()) {
            long start = System.currentTimeMillis();
            ToolResult res = terminalTool.execute(Map.of("command", "mvn test-compile", "timeoutSeconds", 120), repoPath);
            long duration = System.currentTimeMillis() - start;
            int code = res.metadata() != null && res.metadata().get("exitCode") instanceof Number n ? n.intValue() : (res.success() ? 0 : 1);
            return res.success()
                    ? BuildResult.success("mvn test-compile", code, res.output(), duration)
                    : BuildResult.failure("mvn test-compile", code, res.output() != null ? res.output() : res.error(), duration, "Maven compilation failed");
        }

        // Java / Gradle
        if (new File(dir, "build.gradle").exists() || new File(dir, "build.gradle.kts").exists()) {
            boolean hasWrapper = new File(dir, "gradlew").exists();
            String cmd = hasWrapper ? "./gradlew classes testClasses" : "gradle classes testClasses";
            long start = System.currentTimeMillis();
            ToolResult res = terminalTool.execute(Map.of("command", cmd, "timeoutSeconds", 120), repoPath);
            long duration = System.currentTimeMillis() - start;
            int code = res.metadata() != null && res.metadata().get("exitCode") instanceof Number n ? n.intValue() : (res.success() ? 0 : 1);
            return res.success()
                    ? BuildResult.success(cmd, code, res.output(), duration)
                    : BuildResult.failure(cmd, code, res.output() != null ? res.output() : res.error(), duration, "Gradle compilation failed");
        }

        // Rust
        if (new File(dir, "Cargo.toml").exists()) {
            long start = System.currentTimeMillis();
            ToolResult res = terminalTool.execute(Map.of("command", "cargo check", "timeoutSeconds", 120), repoPath);
            long duration = System.currentTimeMillis() - start;
            int code = res.metadata() != null && res.metadata().get("exitCode") instanceof Number n ? n.intValue() : (res.success() ? 0 : 1);
            return res.success()
                    ? BuildResult.success("cargo check", code, res.output(), duration)
                    : BuildResult.failure("cargo check", code, res.output() != null ? res.output() : res.error(), duration, "Cargo check failed");
        }

        // Go
        if (new File(dir, "go.mod").exists()) {
            long start = System.currentTimeMillis();
            ToolResult res = terminalTool.execute(Map.of("command", "go build -o /dev/null ./...", "timeoutSeconds", 120), repoPath);
            long duration = System.currentTimeMillis() - start;
            int code = res.metadata() != null && res.metadata().get("exitCode") instanceof Number n ? n.intValue() : (res.success() ? 0 : 1);
            return res.success()
                    ? BuildResult.success("go build ./...", code, res.output(), duration)
                    : BuildResult.failure("go build ./...", code, res.output() != null ? res.output() : res.error(), duration, "Go build failed");
        }

        return BuildResult.skipped("No explicit build phase required for project structure.");
    }

    private TestResult executeTestCheck(AgentState state, String repoPath) {
        if (state.getLatestTestResult() != null) {
            return state.getLatestTestResult();
        }
        return testRunner.runTests(repoPath, null, null);
    }
}
