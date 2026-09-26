package com.example.codingagent.recovery;

import com.example.codingagent.agent.AgentState;
import com.example.codingagent.agent.AgentStatus;
import com.example.codingagent.logging.AgentEventPublisher;
import com.example.codingagent.model.ChatMessage;
import com.example.codingagent.model.ModelProvider;
import com.example.codingagent.model.ModelRequest;
import com.example.codingagent.model.ModelResponse;
import com.example.codingagent.model.action.ActionType;
import com.example.codingagent.model.action.ModelAction;
import com.example.codingagent.model.action.ModelActionParser;
import com.example.codingagent.testing.FailureAnalysis;
import com.example.codingagent.testing.FailureDetector;
import com.example.codingagent.testing.FailureType;
import com.example.codingagent.testing.TestFailureDetail;
import com.example.codingagent.testing.TestResult;
import com.example.codingagent.testing.TestRunner;
import com.example.codingagent.tools.SecurityGuard;
import com.example.codingagent.tools.ToolManager;
import com.example.codingagent.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecoveryManagerTest {

    private TestRunner testRunner;
    private FailureDetector failureDetector;
    private ModelProvider modelProvider;
    private ToolManager toolManager;
    private ModelActionParser actionParser;
    private SecurityGuard securityGuard;
    private AgentEventPublisher eventPublisher;
    private RecoveryManager recoveryManager;

    @BeforeEach
    void setUp() {
        testRunner = Mockito.mock(TestRunner.class);
        failureDetector = new FailureDetector();
        modelProvider = Mockito.mock(ModelProvider.class);
        toolManager = Mockito.mock(ToolManager.class);
        actionParser = new ModelActionParser();
        securityGuard = new SecurityGuard();
        eventPublisher = Mockito.mock(AgentEventPublisher.class);

        recoveryManager = new RecoveryManager(
                testRunner,
                failureDetector,
                modelProvider,
                toolManager,
                actionParser,
                securityGuard,
                eventPublisher
        );
    }

    @Test
    void testFailureDetectorDetectsCompilationErrors() {
        String output = """
                [ERROR] COMPILATION ERROR : 
                [ERROR] /workspace/src/main/java/com/example/AuthService.java:[28,15] cannot find symbol
                  symbol:   class InvalidCredentialsException
                  location: class com.example.AuthService
                """;

        FailureAnalysis analysis = failureDetector.analyzeCompilationError(output);

        assertEquals(FailureType.COMPILATION_ERROR, analysis.failureType());
        assertTrue(analysis.summary().contains("Compilation failed"));
        assertTrue(analysis.affectedFiles().contains("src/main/java/com/example/AuthService.java")
                || analysis.affectedFiles().stream().anyMatch(f -> f.endsWith("AuthService.java")));
        assertFalse(analysis.keyErrorLines().isEmpty());
    }

    @Test
    void testFailureDetectorDetectsTestFailures() {
        TestFailureDetail detail = new TestFailureDetail(
                "UserServiceTest",
                "testUserNotFound",
                "expected: <404> but was: <500>",
                "org.opentest4j.AssertionFailedError: expected: <404> but was: <500>\n\tat UserServiceTest.java:35"
        );
        TestResult testResult = TestResult.failure(
                "mvn test",
                1,
                10,
                1,
                1200L,
                "stdout",
                "stderr",
                List.of(detail),
                "Java/Maven"
        );

        FailureAnalysis analysis = failureDetector.analyzeTestResult(testResult);

        assertEquals(FailureType.TEST_FAILURE, analysis.failureType());
        assertTrue(analysis.summary().contains("1 test assertion failure(s)"));
        assertTrue(analysis.affectedFiles().stream().anyMatch(f -> f.contains("UserServiceTest")));
    }

    @Test
    void testRecoveryStrictlyEnforcesMaxAttemptsAndPreventsInfiniteLoop(@TempDir Path tempDir) {
        AgentState state = new AgentState("task-123", "Fix failing login", tempDir.toString(), 2);
        state.setAttempts(0);
        state.setMaxAttempts(2);

        TestResult failingResult = TestResult.failure(
                "mvn test", 1, 5, 2, 800L,
                "Failures: 2", "error", List.of(), "Java/Maven"
        );

        // Model returns a write action each time
        when(modelProvider.generate(any(ModelRequest.class)))
                .thenReturn(ModelResponse.text("```json\n{\"action\": \"WRITE_FILE\", \"path\": \"Fix.java\", \"content\": \"// fix\"}\n```"));
        when(toolManager.executeTool(eq("file_tool"), any(), any()))
                .thenReturn(ToolResult.success("File written"));
        // Tests keep failing on every run
        when(testRunner.runTests(anyString(), any(), any()))
                .thenReturn(failingResult);

        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        RecoveryOutcome outcome = recoveryManager.attemptRecovery(state, failingResult, cancelFlag);

        // Verification: MUST NOT loop infinitely. Must stop exactly at maxAttempts (2).
        assertFalse(outcome.recovered());
        assertEquals(2, outcome.attemptsMade());
        assertEquals(2, state.getAttempts());
        assertEquals(AgentStatus.FAILED, state.getStatus());
        assertTrue(state.getCurrentStep().contains("Exceeded maximum recovery attempts"));
        verify(testRunner, times(2)).runTests(anyString(), any(), any());
    }

    @Test
    void testSuccessfulRecoveryRepairsAndPassesTests(@TempDir Path tempDir) throws IOException {
        Path srcFile = tempDir.resolve("Calculator.java");
        Files.writeString(srcFile, "public class Calculator { public int add(int a, int b) { return a - b; } }");

        AgentState state = new AgentState("task-456", "Fix addition bug", tempDir.toString(), 3);
        state.setAttempts(0);
        state.setMaxAttempts(3);

        TestResult initialFail = TestResult.failure(
                "mvn test", 1, 4, 1, 600L,
                "expected: 4 but was: 0", "", List.of(), "Java/Maven"
        );
        TestResult repairedPass = TestResult.success(
                "mvn test", 4, 0, 750L,
                "[INFO] Tests run: 4, Failures: 0, Errors: 0\n[INFO] BUILD SUCCESS", "", "Java/Maven"
        );

        when(modelProvider.generate(any(ModelRequest.class)))
                .thenReturn(ModelResponse.text("```json\n{\"action\": \"WRITE_FILE\", \"path\": \"Calculator.java\", \"content\": \"public class Calculator { public int add(int a, int b) { return a + b; } }\"}\n```"));
        when(toolManager.executeTool(eq("file_tool"), any(), any()))
                .thenReturn(ToolResult.success("Updated Calculator.java"));
        // First re-run succeeds!
        when(testRunner.runTests(anyString(), any(), any()))
                .thenReturn(repairedPass);

        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        RecoveryOutcome outcome = recoveryManager.attemptRecovery(state, initialFail, cancelFlag);

        assertTrue(outcome.recovered());
        assertEquals(1, outcome.attemptsMade());
        assertEquals(1, state.getAttempts());
        assertEquals(repairedPass, state.getLatestTestResult());
    }
}
