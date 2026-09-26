package com.example.codingagent.agent;

import com.example.codingagent.context.ContextManager;
import com.example.codingagent.context.RepositoryContext;
import com.example.codingagent.logging.AgentEvent;
import com.example.codingagent.logging.AgentEventPublisher;
import com.example.codingagent.model.ModelProvider;
import com.example.codingagent.model.ModelRequest;
import com.example.codingagent.model.ModelResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentLoopTest {

    private ModelProvider modelProvider;
    private ToolManager toolManager;
    private ContextManager contextManager;
    private Planner planner;
    private ModelActionParser actionParser;
    private SecurityGuard securityGuard;
    private VerificationManager verificationManager;
    private FailureRecoveryManager recoveryManager;
    private AgentEventPublisher eventPublisher;
    private GitTool gitTool;
    private AgentLoop agentLoop;

    @BeforeEach
    void setUp() {
        modelProvider = Mockito.mock(ModelProvider.class);
        toolManager = Mockito.mock(ToolManager.class);
        contextManager = Mockito.mock(ContextManager.class);
        planner = Mockito.mock(Planner.class);
        actionParser = Mockito.mock(ModelActionParser.class);
        securityGuard = new SecurityGuard();
        verificationManager = Mockito.mock(VerificationManager.class);
        recoveryManager = Mockito.mock(FailureRecoveryManager.class);
        eventPublisher = Mockito.mock(AgentEventPublisher.class);
        gitTool = Mockito.mock(GitTool.class);

        agentLoop = new AgentLoop(
                modelProvider,
                toolManager,
                contextManager,
                planner,
                actionParser,
                securityGuard,
                verificationManager,
                recoveryManager,
                eventPublisher,
                gitTool
        );

        when(gitTool.execute(any(), any())).thenReturn(ToolResult.success(""));
    }

    @Test
    void testUnconfiguredModelHaltLifecycle() {
        AgentState state = new AgentState("task-test-1", "Add health check endpoint", ".", 5);
        AtomicBoolean cancelFlag = new AtomicBoolean(false);

        RepositoryContext repoCtx = new RepositoryContext(".", "test-repo", "Java/Maven", "README", List.of("pom.xml"), "clean");
        when(contextManager.inspectRepository(".")).thenReturn(repoCtx);

        StructuredPlan plan = new StructuredPlan("task-test-1", "Add health check endpoint", Collections.emptyList());
        when(planner.createPlan(any(), any(), any())).thenReturn(plan);

        // Model provider is not configured (missing API key)
        when(modelProvider.isConfigured()).thenReturn(false);
        when(modelProvider.getProviderName()).thenReturn("openai");

        agentLoop.run(state, cancelFlag);

        assertEquals(AgentStatus.FAILED, state.getStatus());
        assertTrue(state.getErrors().stream().anyMatch(e -> e.contains("API key")));
        verify(eventPublisher, atLeastOnce()).publish(any(AgentEvent.class));
    }

    @Test
    void testCancellationHaltsExecutionCleanly() {
        AgentState state = new AgentState("task-test-cancel", "Refactor service", ".", 5);
        AtomicBoolean cancelFlag = new AtomicBoolean(true); // Pre-cancelled

        agentLoop.run(state, cancelFlag);

        assertEquals(AgentStatus.CANCELLED, state.getStatus());
        verify(modelProvider, never()).generate(any());
    }

    @Test
    void testSuccessfulExecutionAndVerification() {
        AgentState state = new AgentState("task-test-success", "Fix bug in calculation", ".", 5);
        AtomicBoolean cancelFlag = new AtomicBoolean(false);

        RepositoryContext repoCtx = new RepositoryContext(".", "test-repo", "Java/Maven", "README", List.of("pom.xml"), "clean");
        when(contextManager.inspectRepository(".")).thenReturn(repoCtx);

        StructuredPlan plan = new StructuredPlan("task-test-success", "Fix bug in calculation", Collections.emptyList());
        when(planner.createPlan(any(), any(), any())).thenReturn(plan);

        when(modelProvider.isConfigured()).thenReturn(true);
        when(modelProvider.getProviderName()).thenReturn("openai");

        ModelResponse resp1 = ModelResponse.text("{\"action\": \"WRITE_FILE\", \"path\": \"pom.xml\", \"content\": \"<project></project>\"}");
        when(modelProvider.generate(any(ModelRequest.class))).thenReturn(resp1);

        com.example.codingagent.model.action.ModelAction writeAction = new com.example.codingagent.model.action.ModelAction();
        writeAction.setAction(com.example.codingagent.model.action.ActionType.WRITE_FILE);
        writeAction.setPath("pom.xml");
        writeAction.setContent("<project></project>");
        when(actionParser.parseFromText(any())).thenReturn(writeAction).thenReturn(null);

        when(toolManager.executeTool(eq("file_tool"), any(), any())).thenReturn(ToolResult.success("File written"));

        VerificationResult verificationResult = VerificationResult.success(
                "All checks passed",
                Collections.emptyList(),
                List.of("Code modification applied", "Tests passed"),
                null,
                null,
                null,
                Collections.emptyList()
        );

        when(verificationManager.verify(any())).thenReturn(verificationResult);
        TaskResult tr = new TaskResult(
                "task-test-success",
                "Fix bug in calculation",
                AgentStatus.COMPLETED,
                true,
                "Verified",
                verificationResult,
                List.of("pom.xml"),
                List.of("pom.xml"),
                Collections.emptyList(),
                null,
                500,
                Instant.now(),
                Instant.now()
        );
        when(verificationManager.generateTaskResult(any(), any())).thenReturn(tr);

        agentLoop.run(state, cancelFlag);

        assertEquals(AgentStatus.COMPLETED, state.getStatus());
        assertNotNull(state.getLatestVerificationResult());
        assertTrue(state.getLatestVerificationResult().verified());
        assertNotNull(state.getFinalTaskResult());
    }
}
