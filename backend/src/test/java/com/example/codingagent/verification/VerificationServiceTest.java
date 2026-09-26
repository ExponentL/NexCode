package com.example.codingagent.verification;

import com.example.codingagent.agent.AgentState;
import com.example.codingagent.agent.AgentStep;
import com.example.codingagent.agent.AgentStep.StepStatus;
import com.example.codingagent.testing.TestResult;
import com.example.codingagent.testing.TestRunner;
import com.example.codingagent.tools.GitTool;
import com.example.codingagent.tools.TerminalTool;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class VerificationServiceTest {

    private TestRunner testRunner;
    private GitTool gitTool;
    private TerminalTool terminalTool;
    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        testRunner = Mockito.mock(TestRunner.class);
        gitTool = Mockito.mock(GitTool.class);
        terminalTool = Mockito.mock(TerminalTool.class);

        verificationService = new VerificationService(testRunner, gitTool, terminalTool);
    }

    @Test
    void testSuccessfulVerificationWithRealEvidence(@TempDir Path tempDir) throws IOException {
        // Prepare real repository environment
        Path srcFile = tempDir.resolve("AuthService.java");
        Files.writeString(srcFile, "public class AuthService { public boolean login() { return true; } }");

        AgentState state = new AgentState("task-verify-1", "Fix login validation", tempDir.toString(), 3);
        state.recordFileInspected("AuthService.java");
        state.recordFileModified("AuthService.java");

        AgentStep step1 = new AgentStep(1, "Inspect AuthService", "Read file", "READ_FILE");
        step1.setStatus(StepStatus.COMPLETED);
        AgentStep step2 = new AgentStep(2, "Modify AuthService", "Fix logic", "WRITE_FILE");
        step2.setStatus(StepStatus.COMPLETED);
        state.setPlan(List.of(step1, step2));

        // Mock clean git status & diff
        when(terminalTool.execute(argThat(m -> m != null && "git status --porcelain".equals(m.get("command"))), eq(tempDir.toString())))
                .thenReturn(ToolResult.success("M AuthService.java"));
        when(terminalTool.execute(argThat(m -> m != null && "git diff HEAD".equals(m.get("command"))), eq(tempDir.toString())))
                .thenReturn(ToolResult.success("+ return true;\n- return false;"));

        // Mock clean build (no pom.xml in tempDir -> build check skips gracefully)
        // Mock passing tests
        TestResult passingTests = TestResult.success(
                "mvn test",
                8,
                0,
                1100L,
                "[INFO] Tests run: 8, Failures: 0, Errors: 0\n[INFO] BUILD SUCCESS",
                "",
                "Java/Maven"
        );
        when(testRunner.runTests(eq(tempDir.toString()), any(), any())).thenReturn(passingTests);

        // Execute verification
        VerificationResult result = verificationService.verify(state);

        // Assertions: Strict verification based on real evidence
        assertTrue(result.verified(), "Task must be verified when all evidence checks pass");
        assertTrue(result.summary().startsWith("VERIFIED"), "Summary must reflect verified status");
        assertTrue(result.checksFailed().isEmpty(), "No failed checks should exist");
        assertFalse(result.checksPassed().isEmpty(), "Passed checks must be populated");
        assertNotNull(result.gitDiff(), "Git diff summary must be present");
        assertEquals(1, result.gitDiff().modifiedFiles().size());

        // Verify TaskResult generation
        TaskResult taskResult = verificationService.generateTaskResult(state, result);
        assertNotNull(taskResult);
        assertTrue(taskResult.verified());
        assertEquals("task-verify-1", taskResult.taskId());
    }

    @Test
    void testFailingVerificationWhenNoFilesWereModified(@TempDir Path tempDir) {
        AgentState state = new AgentState("task-verify-2", "Refactor module", tempDir.toString(), 3);
        state.recordFileInspected("Service.java");
        // CRITICAL: No files modified! Model might claim it's done, but harness must reject!

        when(terminalTool.execute(any(), anyString()))
                .thenReturn(ToolResult.success(""));

        VerificationResult result = verificationService.verify(state);

        assertFalse(result.verified(), "Must NEVER verify if no files were modified");
        assertTrue(result.summary().contains("NOT VERIFIED"), "Summary must explicitly declare NOT VERIFIED");
        assertTrue(result.checksFailed().stream().anyMatch(c -> c.contains("Code modification applied")));
    }

    @Test
    void testFailingVerificationWhenTestsFail(@TempDir Path tempDir) throws IOException {
        Path srcFile = tempDir.resolve("Payment.java");
        Files.writeString(srcFile, "public class Payment {}");

        AgentState state = new AgentState("task-verify-3", "Fix payment bug", tempDir.toString(), 3);
        state.recordFileInspected("Payment.java");
        state.recordFileModified("Payment.java");

        when(terminalTool.execute(argThat(m -> m != null && "git status --porcelain".equals(m.get("command"))), eq(tempDir.toString())))
                .thenReturn(ToolResult.success("M Payment.java"));
        when(terminalTool.execute(argThat(m -> m != null && "git diff HEAD".equals(m.get("command"))), eq(tempDir.toString())))
                .thenReturn(ToolResult.success("diff content"));

        // Tests fail with 2 failures!
        TestResult failingTests = TestResult.failure(
                "mvn test",
                1,
                10,
                2,
                1400L,
                "Tests run: 10, Failures: 2",
                "Assertion failed",
                List.of(),
                "Java/Maven"
        );
        when(testRunner.runTests(eq(tempDir.toString()), any(), any())).thenReturn(failingTests);

        VerificationResult result = verificationService.verify(state);

        assertFalse(result.verified(), "Must fail verification when automated tests fail");
        assertTrue(result.summary().contains("NOT VERIFIED"));
        assertTrue(result.checksFailed().stream().anyMatch(c -> c.contains("Tests executed & passed")));
    }

    @Test
    void testFailingVerificationWhenBuildFails(@TempDir Path tempDir) throws IOException {
        // Create pom.xml to trigger real build check
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");
        Path srcFile = tempDir.resolve("Error.java");
        Files.writeString(srcFile, "invalid syntax");

        AgentState state = new AgentState("task-verify-4", "Build task", tempDir.toString(), 3);
        state.recordFileInspected("Error.java");
        state.recordFileModified("Error.java");

        // Mock mvn test-compile failure
        when(terminalTool.execute(argThat(m -> m != null && "mvn test-compile".equals(m.get("command"))), eq(tempDir.toString())))
                .thenReturn(ToolResult.failure("Compilation error at line 1", "error"));

        VerificationResult result = verificationService.verify(state);

        assertFalse(result.verified(), "Must fail verification when build fails");
        assertTrue(result.summary().contains("NOT VERIFIED"));
        assertTrue(result.checksFailed().stream().anyMatch(c -> c.contains("Build completed")));
    }

    @Test
    void testFailingVerificationWhenCriticalSecurityErrorRemains(@TempDir Path tempDir) throws IOException {
        Path srcFile = tempDir.resolve("Safe.java");
        Files.writeString(srcFile, "// content");

        AgentState state = new AgentState("task-verify-5", "Task with errors", tempDir.toString(), 3);
        state.recordFileInspected("Safe.java");
        state.recordFileModified("Safe.java");
        state.recordError("Action rejected by harness safety validator: Dangerous command blocked by SecurityGuard");

        when(terminalTool.execute(any(), anyString()))
                .thenReturn(ToolResult.success(""));

        VerificationResult result = verificationService.verify(state);

        assertFalse(result.verified(), "Must fail verification when unresolved safety errors remain");
        assertFalse(result.remainingErrors().isEmpty());
    }
}
