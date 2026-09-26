package com.example.codingagent.agent;

import com.example.codingagent.context.ContextManager;
import com.example.codingagent.context.RepositoryContext;
import com.example.codingagent.logging.AgentEvent;
import com.example.codingagent.logging.AgentEventPublisher;
import com.example.codingagent.logging.EventType;
import com.example.codingagent.model.ChatMessage;
import com.example.codingagent.model.ModelException;
import com.example.codingagent.model.ModelProvider;
import com.example.codingagent.model.ModelRequest;
import com.example.codingagent.model.ModelResponse;
import com.example.codingagent.model.ToolCall;
import com.example.codingagent.model.action.ActionType;
import com.example.codingagent.model.action.ActionValidationResult;
import com.example.codingagent.model.action.ModelAction;
import com.example.codingagent.model.action.ModelActionParser;
import com.example.codingagent.planner.Planner;
import com.example.codingagent.planner.StructuredPlan;
import com.example.codingagent.tools.GitTool;
import com.example.codingagent.tools.SecurityGuard;
import com.example.codingagent.tools.ToolManager;
import com.example.codingagent.tools.ToolResult;
import com.example.codingagent.verification.FailureRecoveryManager;
import com.example.codingagent.verification.TaskResult;
import com.example.codingagent.verification.VerificationManager;
import com.example.codingagent.verification.VerificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AgentLoop {

    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);
    private static final int MAX_ACTION_TURNS = 18;

    private final ModelProvider modelProvider;
    private final ToolManager toolManager;
    private final ContextManager contextManager;
    private final Planner planner;
    private final ModelActionParser actionParser;
    private final SecurityGuard securityGuard;
    private final VerificationManager verificationManager;
    private final FailureRecoveryManager recoveryManager;
    private final AgentEventPublisher eventPublisher;
    private final GitTool gitTool;

    public AgentLoop(
            ModelProvider modelProvider,
            ToolManager toolManager,
            ContextManager contextManager,
            Planner planner,
            ModelActionParser actionParser,
            SecurityGuard securityGuard,
            VerificationManager verificationManager,
            FailureRecoveryManager recoveryManager,
            AgentEventPublisher eventPublisher,
            GitTool gitTool) {
        this.modelProvider = modelProvider;
        this.toolManager = toolManager;
        this.contextManager = contextManager;
        this.planner = planner;
        this.actionParser = actionParser;
        this.securityGuard = securityGuard;
        this.verificationManager = verificationManager;
        this.recoveryManager = recoveryManager;
        this.eventPublisher = eventPublisher;
        this.gitTool = gitTool;
    }

    public void run(AgentState state, AtomicBoolean cancelFlag) {
        String taskId = state.getTaskId();
        log.info("Starting autonomous agent loop for task {}", taskId);

        try {
            // 1. Task Received
            eventPublisher.publish(AgentEvent.of(taskId, EventType.TASK_RECEIVED,
                    "Task received: " + state.getTask(),
                    Map.of("repository", state.getRepositoryPath())));

            // 2. Load and inspect selected repository
            if (isCancelled(cancelFlag, state)) return;
            state.setStatus(AgentStatus.ANALYZING);
            state.setCurrentStep("Analyzing repository and indexing key structures");

            RepositoryContext repoContext = contextManager.inspectRepository(state.getRepositoryPath());
            eventPublisher.publish(AgentEvent.of(taskId, EventType.REPOSITORY_SCANNED,
                    "Repository scanned: " + repoContext.projectType() + " with " + repoContext.keyFiles().size() + " files indexed",
                    Map.of("projectType", repoContext.projectType(), "keyFiles", repoContext.keyFiles())));

            for (String file : repoContext.keyFiles()) {
                state.recordFileInspected(file);
            }

            // 3. Synthesize Machine-Readable Implementation Plan
            if (isCancelled(cancelFlag, state)) return;
            state.setStatus(AgentStatus.PLANNING);
            state.setCurrentStep("Synthesizing machine-readable implementation plan");

            StructuredPlan plan = planner.createPlan(taskId, state.getTask(), repoContext);
            state.setPlan(plan.toAgentSteps());
            eventPublisher.publish(AgentEvent.of(taskId, EventType.PLAN_CREATED,
                    "Generated structured plan with " + plan.getSteps().size() + " engineering phases",
                    Map.of("phasesCount", plan.getSteps().size())));

            // Verify foundation model credentials
            if (!modelProvider.isConfigured()) {
                String errorMsg = "Model provider '" + modelProvider.getProviderName() + "' requires an API key. Set AGENT_MODEL_API_KEY to enable live model execution.";
                state.recordError(errorMsg);
                state.setStatus(AgentStatus.FAILED);
                state.setCurrentStep("Configuration error: API key missing");
                eventPublisher.publish(AgentEvent.of(taskId, EventType.ERROR, errorMsg));
                eventPublisher.publish(AgentEvent.of(taskId, EventType.TASK_FAILED, "Harness stopped: Foundation model not configured."));
                return;
            }

            // 4. Autonomous Action Execution Loop
            state.setStatus(AgentStatus.EXECUTING);
            state.setAttempts(state.getAttempts() + 1);

            int turn = 0;
            int consecutiveModelErrors = 0;
            boolean finished = false;

            while (turn < MAX_ACTION_TURNS && !finished) {
                if (isCancelled(cancelFlag, state)) return;
                turn++;

                // Build prioritized context (avoids dumping entire repo)
                List<ChatMessage> promptMessages = contextManager.buildPrioritizedContext(state, repoContext, plan);

                eventPublisher.publish(AgentEvent.of(taskId, EventType.MODEL_REQUEST, "Prompting foundation model for next action (Turn " + turn + ")"));
                ModelRequest request = ModelRequest.builder()
                        .messages(promptMessages)
                        .tools(toolManager.getToolDefinitions())
                        .temperature(0.1)
                        .build();

                ModelResponse response;
                try {
                    response = modelProvider.generate(request);
                    consecutiveModelErrors = 0;
                } catch (ModelException me) {
                    consecutiveModelErrors++;
                    log.warn("Turn {} model communication exception: {}", turn, me.getMessage());
                    state.recordError("Model communication error: " + me.getMessage());
                    eventPublisher.publish(AgentEvent.of(taskId, EventType.ERROR, "Model communication error: " + me.getMessage()));
                    if (consecutiveModelErrors >= 3) {
                        log.error("Exceeded 3 consecutive model communication errors; terminating turn loop.");
                        throw me;
                    }
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                    continue;
                }

                // Publish concise action summary (no raw chain-of-thought)
                String conciseSummary = summarizeAction(response);
                eventPublisher.publish(AgentEvent.of(taskId, EventType.MODEL_RESPONSE, conciseSummary));

                // Parse model action from tool call or JSON text
                List<ModelAction> actionsToExecute = extractActions(response);

                if (actionsToExecute.isEmpty()) {
                    boolean isEmptyResponse = response.content() == null || response.content().isBlank();
                    String feedbackError = isEmptyResponse
                            ? "Received empty response from foundation model. Please specify your next action using JSON schema: {\"action\": \"...\", ...}"
                            : "Could not parse actionable ModelAction JSON or tool call from model response: \""
                            + (response.content().length() > 80 ? response.content().substring(0, 80) + "..." : response.content())
                            + "\". Please provide valid JSON conforming to the schema.";

                    log.warn("Turn {}: {}", turn, feedbackError);
                    state.recordError(feedbackError);
                    eventPublisher.publish(AgentEvent.of(taskId, EventType.ERROR, feedbackError));

                    if (turn >= 3 && state.getErrors().size() >= 3) {
                        log.info("Multiple consecutive unparseable responses; proceeding to verification.");
                        break;
                    }
                    continue;
                }

                for (ModelAction action : actionsToExecute) {
                    if (isCancelled(cancelFlag, state)) return;

                    if (action.getAction() == ActionType.FINISH) {
                        log.info("Model signaled completion of task actions.");
                        finished = true;
                        break;
                    }

                    // Validate action before execution (path traversal, unsafe commands, missing parameters)
                    ActionValidationResult validation = action.validate(state.getRepositoryPath(), securityGuard);
                    if (!validation.valid()) {
                        String rejection = "Action rejected by harness safety validator: " + validation.errorMessage();
                        log.warn("Safety rejection: {}", rejection);
                        state.recordError(rejection);
                        eventPublisher.publish(AgentEvent.of(taskId, EventType.ERROR, rejection));

                        String target = action.getPath() != null ? action.getPath()
                                : (action.getCommand() != null ? action.getCommand() : (action.getQuery() != null ? action.getQuery() : ""));
                        state.recordActionResult(ActionResult.failure(action.getAction(), target, rejection, 0));
                        continue;
                    }

                    // Execute validated action
                    state.setCurrentStep("Executing: " + action.getAction() + (action.getPath() != null ? " on " + action.getPath() : ""));
                    ToolResult result = executeModelAction(action, state.getRepositoryPath());

                    recordActionResult(state, action, result);
                }
            }

            // 5. Verification Phase
            if (isCancelled(cancelFlag, state)) return;
            state.setStatus(AgentStatus.VERIFYING);
            state.setCurrentStep("Running verification checks and automated tests");
            eventPublisher.publish(AgentEvent.of(taskId, EventType.VERIFICATION_STARTED, "Beginning comprehensive verification"));

            // Refresh git diff
            ToolResult diffRes = gitTool.execute(Map.of("action", "diff"), state.getRepositoryPath());
            if (diffRes.success() && diffRes.output() != null) {
                state.setDiff(diffRes.output());
            }

            // Run verification manager
            eventPublisher.publish(AgentEvent.of(taskId, EventType.TEST_STARTED, "Executing automated test suite"));
            VerificationResult verification = verificationManager.verify(state);

            if (verification.passed()) {
                eventPublisher.publish(AgentEvent.of(taskId, EventType.TEST_PASSED, "Automated tests passed"));
            } else {
                eventPublisher.publish(AgentEvent.of(taskId, EventType.TEST_FAILED, "Test verification failed: " + verification.summary()));
            }

            // 6. Recovery Loop if verification failed
            if (!verification.passed() && recoveryManager.canRetry(state)) {
                state.setStatus(AgentStatus.RECOVERING);
                state.setCurrentStep("Initiating failure recovery attempt " + (state.getAttempts() + 1));
                eventPublisher.publish(AgentEvent.of(taskId, EventType.RECOVERY_STARTED, "Verification failed; beginning self-repair loop"));
                eventPublisher.publish(AgentEvent.of(taskId, EventType.RECOVERY_ATTEMPT, "Recovery attempt " + (state.getAttempts() + 1) + " of " + state.getMaxAttempts()));

                String recoveryPrompt = recoveryManager.generateRecoveryPlan(state, verification);
                state.setAttempts(state.getAttempts() + 1);

                List<ChatMessage> recoveryMessages = contextManager.buildPrioritizedContext(state, repoContext, plan);
                recoveryMessages.add(ChatMessage.user("Recovery Directive:\n" + recoveryPrompt));

                ModelRequest recoveryRequest = ModelRequest.builder()
                        .messages(recoveryMessages)
                        .tools(toolManager.getToolDefinitions())
                        .build();

                ModelResponse recoveryResp = modelProvider.generate(recoveryRequest);
                List<ModelAction> recoveryActions = extractActions(recoveryResp);
                for (ModelAction ra : recoveryActions) {
                    ActionValidationResult vr = ra.validate(state.getRepositoryPath(), securityGuard);
                    if (vr.valid()) {
                        ToolResult tr = executeModelAction(ra, state.getRepositoryPath());
                        recordActionResult(state, ra, tr);
                    }
                }

                verification = verificationManager.verify(state);
            }

            state.setLatestVerificationResult(verification);
            TaskResult taskResult = verificationManager.generateTaskResult(state, verification);
            state.setFinalTaskResult(taskResult);

            // 7. Evidence-based Completion Determination
            boolean hasEvidenceOfChanges = !state.getFilesModified().isEmpty() || (state.getDiff() != null && !state.getDiff().isBlank());
            boolean testsSuccessful = verification.passed();

            if (hasEvidenceOfChanges && testsSuccessful) {
                state.setStatus(AgentStatus.COMPLETED);
                state.setCurrentStep("Task completed and verified with actual evidence");
                state.setEvidenceSummary("Verified modifications across " + state.getFilesModified().size() + " files; all automated tests passing.");
                eventPublisher.publish(AgentEvent.of(taskId, EventType.TASK_COMPLETED, "Autonomous task successfully completed and verified.", Map.of(
                        "filesModified", state.getFilesModified(),
                        "diffLength", state.getDiff().length()
                )));
            } else {
                state.setStatus(AgentStatus.FAILED);
                String reason = !hasEvidenceOfChanges
                        ? "Task failed: No files were modified as evidence of solution."
                        : "Task failed: Automated tests or verification checks did not pass.";
                state.setCurrentStep(reason);
                state.recordError(reason);
                eventPublisher.publish(AgentEvent.of(taskId, EventType.TASK_FAILED, reason));
            }

        } catch (ModelException me) {
            log.error("Model communication exception: {}", me.getMessage());
            state.recordError("Foundation model error: " + me.getMessage());
            state.setStatus(AgentStatus.FAILED);
            state.setCurrentStep("Foundation model failure");
            eventPublisher.publish(AgentEvent.of(taskId, EventType.ERROR, "Foundation model error: " + me.getMessage()));
            eventPublisher.publish(AgentEvent.of(taskId, EventType.TASK_FAILED, "Task aborted due to model error."));
        } catch (Exception e) {
            log.error("Fatal exception in AgentLoop for task {}: {}", taskId, e.getMessage(), e);
            state.recordError("Harness exception: " + e.getMessage());
            state.setStatus(AgentStatus.FAILED);
            state.setCurrentStep("Fatal error: " + e.getMessage());
            eventPublisher.publish(AgentEvent.of(taskId, EventType.ERROR, "Harness exception: " + e.getMessage()));
            eventPublisher.publish(AgentEvent.of(taskId, EventType.TASK_FAILED, "Task failed."));
        }
    }

    private List<ModelAction> extractActions(ModelResponse response) {
        List<ModelAction> actions = new ArrayList<>();
        // Priority 1: Check tool calls
        if (response.hasToolCalls()) {
            for (ToolCall tc : response.toolCalls()) {
                ModelAction ma = actionParser.parseFromToolCall(tc);
                if (ma != null) actions.add(ma);
            }
        }
        // Priority 2: Check structured JSON text block
        if (actions.isEmpty() && response.content() != null) {
            ModelAction ma = actionParser.parseFromText(response.content());
            if (ma != null) actions.add(ma);
        }
        return actions;
    }

    private ToolResult executeModelAction(ModelAction action, String workingDirectory) {
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

            case FINISH -> ToolResult.success("Task completed by model request.");
        };
    }

    private void recordActionResult(AgentState state, ModelAction action, ToolResult result) {
        String target = action.getPath() != null ? action.getPath()
                : (action.getCommand() != null ? action.getCommand() : (action.getQuery() != null ? action.getQuery() : ""));
        String actionSummary = action.getAction() + (!target.isBlank() ? " (" + target + ")" : "");
        state.recordCommandExecuted(actionSummary);

        ActionResult actionResult = ActionResult.of(
                action.getAction(),
                target,
                result.success(),
                result.output(),
                result.error(),
                result.durationMs()
        );
        state.recordActionResult(actionResult);

        if (result.success()) {
            switch (action.getAction()) {
                case READ_FILE -> {
                    state.recordFileInspected(action.getPath());
                    eventPublisher.publish(AgentEvent.of(state.getTaskId(), EventType.FILE_READ, "Inspected: " + action.getPath()));
                }
                case WRITE_FILE, CREATE_FILE -> {
                    state.recordFileModified(action.getPath());
                    eventPublisher.publish(AgentEvent.of(state.getTaskId(), EventType.FILE_MODIFIED, "Modified: " + action.getPath()));
                }
                case SEARCH -> eventPublisher.publish(AgentEvent.of(state.getTaskId(), EventType.SEARCH_PERFORMED, "Searched: " + action.getQuery()));
                case RUN_COMMAND -> eventPublisher.publish(AgentEvent.of(state.getTaskId(), EventType.COMMAND_EXECUTED, "Executed: " + action.getCommand()));
                default -> {}
            }
        } else {
            String errorMsg = result.error() != null ? result.error() : "Action failed";
            state.recordError(action.getAction() + " error: " + errorMsg);
        }
    }

    private boolean isCancelled(AtomicBoolean flag, AgentState state) {
        if (flag != null && flag.get()) {
            state.setStatus(AgentStatus.CANCELLED);
            state.setCurrentStep("Task cancelled by user");
            eventPublisher.publish(AgentEvent.of(state.getTaskId(), EventType.TASK_CANCELLED, "Execution halted: Cancel requested"));
            return true;
        }
        return false;
    }

    private String summarizeAction(ModelResponse resp) {
        if (resp.hasToolCalls()) {
            List<String> toolNames = resp.toolCalls().stream().map(ToolCall::name).toList();
            return "Model requested actions: " + String.join(", ", toolNames);
        }
        String text = resp.content();
        if (text == null || text.isBlank()) {
            return "Model yielded with no additional action.";
        }
        return text.length() > 140 ? text.substring(0, 140) + "..." : text;
    }
}
