package com.example.codingagent.recovery;

import com.example.codingagent.agent.ActionResult;
import com.example.codingagent.agent.AgentState;
import com.example.codingagent.agent.AgentStatus;
import com.example.codingagent.logging.AgentEvent;
import com.example.codingagent.logging.AgentEventPublisher;
import com.example.codingagent.logging.EventType;
import com.example.codingagent.model.ChatMessage;
import com.example.codingagent.model.ModelProvider;
import com.example.codingagent.model.ModelRequest;
import com.example.codingagent.model.ModelResponse;
import com.example.codingagent.model.ToolCall;
import com.example.codingagent.model.action.ActionType;
import com.example.codingagent.model.action.ActionValidationResult;
import com.example.codingagent.model.action.ModelAction;
import com.example.codingagent.model.action.ModelActionParser;
import com.example.codingagent.testing.FailureAnalysis;
import com.example.codingagent.testing.FailureDetector;
import com.example.codingagent.testing.TestResult;
import com.example.codingagent.testing.TestRunner;
import com.example.codingagent.tools.SecurityGuard;
import com.example.codingagent.tools.ToolManager;
import com.example.codingagent.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates autonomous self-repair and recovery when automated tests or build checks fail.
 * Strictly adheres to maxAttempts retry limit to prevent infinite loops.
 */
@Component
public class RecoveryManager {

    private static final Logger log = LoggerFactory.getLogger(RecoveryManager.class);

    private final TestRunner testRunner;
    private final FailureDetector failureDetector;
    private final ModelProvider modelProvider;
    private final ToolManager toolManager;
    private final ModelActionParser actionParser;
    private final SecurityGuard securityGuard;
    private final AgentEventPublisher eventPublisher;

    public RecoveryManager(
            TestRunner testRunner,
            FailureDetector failureDetector,
            ModelProvider modelProvider,
            ToolManager toolManager,
            ModelActionParser actionParser,
            SecurityGuard securityGuard,
            AgentEventPublisher eventPublisher) {
        this.testRunner = testRunner;
        this.failureDetector = failureDetector;
        this.modelProvider = modelProvider;
        this.toolManager = toolManager;
        this.actionParser = actionParser;
        this.securityGuard = securityGuard;
        this.eventPublisher = eventPublisher;
    }

    public boolean canRetry(AgentState state) {
        return state.getAttempts() < state.getMaxAttempts();
    }

    /**
     * Executes the recovery loop:
     * 1. Capture failure
     * 2. Identify relevant files
     * 3. Send failure information to the model
     * 4. Ask for a corrective action
     * 5. Validate the action
     * 6. Apply the change
     * 7. Run the real tests again
     * 8. Compare the new result
     * 9. Repeat only up to maxAttempts
     */
    public RecoveryOutcome attemptRecovery(
            AgentState state,
            TestResult initialFailure,
            AtomicBoolean cancelFlag) {

        String taskId = state.getTaskId();
        String repoPath = state.getRepositoryPath();
        TestResult currentTestResult = initialFailure;
        FailureAnalysis lastAnalysis = failureDetector.analyzeTestResult(initialFailure);

        eventPublisher.publish(AgentEvent.of(taskId, EventType.RECOVERY_STARTED,
                "Beginning autonomous self-repair: " + lastAnalysis.summary(),
                Map.of("failureType", lastAnalysis.failureType(), "affectedFiles", lastAnalysis.affectedFiles())));

        while (canRetry(state)) {
            if (cancelFlag != null && cancelFlag.get()) {
                log.info("Recovery cancelled by user request for task {}", taskId);
                return RecoveryOutcome.exhausted(currentTestResult, lastAnalysis, state.getAttempts(), "Recovery cancelled by user.");
            }

            state.setAttempts(state.getAttempts() + 1);
            int attemptNum = state.getAttempts();
            state.setStatus(AgentStatus.RECOVERING);
            state.setCurrentStep(String.format("Recovery attempt %d of %d: Diagnosing and fixing %s",
                    attemptNum, state.getMaxAttempts(), lastAnalysis.failureType()));

            eventPublisher.publish(AgentEvent.of(taskId, EventType.RECOVERY_ATTEMPT,
                    String.format("Executing recovery attempt %d of %d", attemptNum, state.getMaxAttempts()),
                    Map.of("attempt", attemptNum, "maxAttempts", state.getMaxAttempts())));

            // 1. Identify relevant files
            Set<String> relevantFiles = new LinkedHashSet<>(lastAnalysis.affectedFiles());
            relevantFiles.addAll(state.getFilesModified());

            // 2. Build targeted recovery prompt for model
            List<ChatMessage> promptMessages = buildRecoveryPrompt(state, currentTestResult, lastAnalysis, relevantFiles);

            // 3. Ask foundation model for corrective action
            ModelResponse modelResp;
            try {
                ModelRequest req = ModelRequest.builder()
                        .messages(promptMessages)
                        .tools(toolManager.getToolDefinitions())
                        .temperature(0.1)
                        .build();
                modelResp = modelProvider.generate(req);
            } catch (Exception me) {
                log.error("Model failure during recovery attempt: {}", me.getMessage());
                state.recordError("Recovery model error: " + me.getMessage());
                break;
            }

            // 4. Extract and validate corrective actions
            List<ModelAction> actions = extractActions(modelResp);
            if (actions.isEmpty()) {
                log.warn("Model provided no actionable directives during recovery attempt {}", attemptNum);
                state.recordError("Model returned no actionable directives for recovery.");
            }

            for (ModelAction action : actions) {
                if (action.getAction() == ActionType.FINISH) continue;

                ActionValidationResult validation = action.validate(repoPath, securityGuard);
                if (!validation.valid()) {
                    String rejection = "Recovery action rejected by security validator: " + validation.errorMessage();
                    state.recordError(rejection);
                    continue;
                }

                // 5. Apply the change via Java tools
                state.setCurrentStep("Applying recovery fix: " + action.getAction() + " on " + action.getPath());
                ToolResult tr = executeAction(action, repoPath);
                recordActionResult(state, action, tr);
            }

            // 6. Run the real tests again
            eventPublisher.publish(AgentEvent.of(taskId, EventType.TEST_STARTED, "Re-running real automated tests to verify recovery"));
            TestResult newTestResult = testRunner.runTests(repoPath, null, null);
            state.setLatestTestResult(newTestResult);

            // 7. Compare results
            log.info("Recovery attempt {} completed. Previous success: {} -> New success: {}",
                    attemptNum, currentTestResult.isSuccess(), newTestResult.isSuccess());

            if (newTestResult.isSuccess()) {
                eventPublisher.publish(AgentEvent.of(taskId, EventType.TEST_PASSED,
                        "Recovery successful! Tests passing: " + (newTestResult.passedTests() != null ? newTestResult.passedTests() : "all") + " passed",
                        Map.of("testsRun", newTestResult.totalTests() != null ? newTestResult.totalTests() : 0,
                                "durationMs", newTestResult.durationMs())));
                return RecoveryOutcome.success(newTestResult, attemptNum);
            } else {
                eventPublisher.publish(AgentEvent.of(taskId, EventType.TEST_FAILED,
                        "Tests still failing after recovery attempt " + attemptNum + ": " +
                                (newTestResult.failedTests() != null ? newTestResult.failedTests() + " failed" : "non-zero exit code"),
                        Map.of("failed", newTestResult.failedTests() != null ? newTestResult.failedTests() : 1)));

                currentTestResult = newTestResult;
                lastAnalysis = failureDetector.analyzeTestResult(newTestResult);
            }
        }

        // Retry limit reached: NEVER loop infinitely.
        String failureSummary = String.format("Exceeded maximum recovery attempts (%d/%d). Remaining failure: %s",
                state.getAttempts(), state.getMaxAttempts(), lastAnalysis.summary());
        log.warn("Task {} recovery exhausted: {}", taskId, failureSummary);

        state.setStatus(AgentStatus.FAILED);
        state.setCurrentStep(failureSummary);
        state.recordError(failureSummary);
        eventPublisher.publish(AgentEvent.of(taskId, EventType.TASK_FAILED, failureSummary));

        return RecoveryOutcome.exhausted(currentTestResult, lastAnalysis, state.getAttempts(), failureSummary);
    }

    private List<ChatMessage> buildRecoveryPrompt(
            AgentState state,
            TestResult testResult,
            FailureAnalysis analysis,
            Set<String> relevantFiles) {

        StringBuilder sb = new StringBuilder();
        sb.append("=== AUTOMATED RECOVERY DIRECTIVE ===\n");
        sb.append("Task: ").append(state.getTask()).append("\n");
        sb.append("Recovery Attempt: ").append(state.getAttempts()).append(" of ").append(state.getMaxAttempts()).append("\n\n");

        sb.append("Failure Classification: ").append(analysis.failureType()).append("\n");
        sb.append("Summary: ").append(analysis.summary()).append("\n");
        sb.append("Root Cause: ").append(analysis.rootCause()).append("\n\n");

        if (!analysis.keyErrorLines().isEmpty()) {
            sb.append("Key Error Diagnostics:\n");
            for (String line : analysis.keyErrorLines()) {
                sb.append("  ").append(line).append("\n");
            }
            sb.append("\n");
        }

        if (!relevantFiles.isEmpty()) {
            sb.append("Affected Files to Inspect/Fix: ").append(String.join(", ", relevantFiles)).append("\n\n");
        }

        sb.append("Recent Test Command: ").append(testResult.command()).append(" (Exit code: ").append(testResult.exitCode()).append(")\n");
        if (testResult.output() != null && !testResult.output().isBlank()) {
            String out = testResult.output().trim();
            if (out.length() > 800) {
                out = "..." + out.substring(out.length() - 800);
            }
            sb.append("Tail of Test Output:\n").append(out).append("\n\n");
        }

        sb.append("Suggested Remediation: ").append(analysis.suggestedRemediation()).append("\n\n");
        sb.append("Instructions:\n");
        sb.append("1. Provide surgical code modifications using WRITE_FILE or CREATE_FILE to fix the exact root cause.\n");
        sb.append("2. Respond with a valid ModelAction JSON or tool call.");

        return List.of(
                ChatMessage.system("You are an expert autonomous code-repair agent. Your job is to analyze real compiler and test failures and fix them directly."),
                ChatMessage.user(sb.toString())
        );
    }

    private List<ModelAction> extractActions(ModelResponse response) {
        List<ModelAction> actions = new ArrayList<>();
        if (response.hasToolCalls()) {
            for (ToolCall tc : response.toolCalls()) {
                ModelAction ma = actionParser.parseFromToolCall(tc);
                if (ma != null) actions.add(ma);
            }
        }
        if (actions.isEmpty() && response.content() != null) {
            ModelAction ma = actionParser.parseFromText(response.content());
            if (ma != null) actions.add(ma);
        }
        return actions;
    }

    private ToolResult executeAction(ModelAction action, String workingDirectory) {
        return switch (action.getAction()) {
            case READ_FILE -> toolManager.executeTool("file_tool", Map.of(
                    "action", "READ_FILE",
                    "path", action.getPath(),
                    "startLine", action.getStartLine() != null ? action.getStartLine() : 1,
                    "endLine", action.getEndLine() != null ? action.getEndLine() : 500
            ), workingDirectory);

            case WRITE_FILE -> toolManager.executeTool("file_tool", Map.of(
                    "action", "WRITE_FILE",
                    "path", action.getPath(),
                    "content", action.getContent() != null ? action.getContent() : ""
            ), workingDirectory);

            case CREATE_FILE -> toolManager.executeTool("file_tool", Map.of(
                    "action", "CREATE_FILE",
                    "path", action.getPath(),
                    "content", action.getContent() != null ? action.getContent() : ""
            ), workingDirectory);

            case SEARCH -> toolManager.executeTool("search_tool", Map.of(
                    "action", "SEARCH_CONTENT",
                    "query", action.getQuery() != null ? action.getQuery() : ""
            ), workingDirectory);

            case RUN_COMMAND -> toolManager.executeTool("terminal_tool", Map.of(
                    "command", action.getCommand() != null ? action.getCommand() : ""
            ), workingDirectory);

            case RUN_TESTS -> toolManager.executeTool("test_tool", action.getCommand() != null
                    ? Map.of("command", action.getCommand())
                    : Map.of(), workingDirectory);

            case GIT_STATUS -> toolManager.executeTool("git_tool", Map.of("action", "status"), workingDirectory);
            case GIT_DIFF -> toolManager.executeTool("git_tool", Map.of("action", "diff"), workingDirectory);
            case FINISH -> ToolResult.success("Recovery action finished");
        };
    }

    private void recordActionResult(AgentState state, ModelAction action, ToolResult result) {
        String target = action.getPath() != null ? action.getPath()
                : (action.getCommand() != null ? action.getCommand() : "");
        state.recordCommandExecuted("RECOVERY: " + action.getAction() + (!target.isBlank() ? " (" + target + ")" : ""));

        ActionResult ar = ActionResult.of(
                action.getAction(),
                target,
                result.success(),
                result.output(),
                result.error(),
                result.durationMs()
        );
        state.recordActionResult(ar);

        if (result.success()) {
            if (action.getAction() == ActionType.WRITE_FILE || action.getAction() == ActionType.CREATE_FILE) {
                state.recordFileModified(action.getPath());
                eventPublisher.publish(AgentEvent.of(state.getTaskId(), EventType.FILE_MODIFIED, "Recovery patched: " + action.getPath()));
            }
        } else {
            state.recordError("Recovery action error: " + result.error());
        }
    }
}
