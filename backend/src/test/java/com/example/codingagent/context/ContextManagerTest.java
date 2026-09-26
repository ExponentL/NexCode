package com.example.codingagent.context;

import com.example.codingagent.agent.AgentState;
import com.example.codingagent.model.ChatMessage;
import com.example.codingagent.planner.Planner;
import com.example.codingagent.planner.StructuredPlan;
import com.example.codingagent.tools.FileTool;
import com.example.codingagent.tools.GitTool;
import com.example.codingagent.tools.RepositoryTool;
import com.example.codingagent.tools.SecurityGuard;
import com.example.codingagent.tools.TerminalTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContextManagerTest {

    private ContextManager contextManager;
    private Planner planner;

    @BeforeEach
    void setUp() {
        SecurityGuard guard = new SecurityGuard();
        TerminalTool terminalTool = new TerminalTool(guard);
        GitTool gitTool = new GitTool(terminalTool);
        FileTool fileTool = new FileTool(guard);
        RepositoryTool repositoryTool = new RepositoryTool(guard);

        contextManager = new ContextManager(gitTool, fileTool, repositoryTool);
        planner = new Planner();
    }

    @Test
    void testKeywordExtraction() {
        String prompt = "Fix the login validation bug in AuthService";
        Set<String> keywords = contextManager.extractKeywords(prompt);

        assertTrue(keywords.contains("login"));
        assertTrue(keywords.contains("validation"));
        assertTrue(keywords.contains("authservice"));
        assertFalse(keywords.contains("the"));
        assertFalse(keywords.contains("fix"));
    }

    @Test
    void testFindRelevantFilesFiltersUnrelated(@TempDir Path tempDir) throws IOException {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);

        // Relevant files
        Files.writeString(src.resolve("LoginValidator.java"), "public class LoginValidator {}");
        Files.writeString(src.resolve("LoginValidatorTest.java"), "public class LoginValidatorTest {}");
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");

        // Unrelated files
        Files.writeString(src.resolve("InvoiceService.java"), "public class InvoiceService {}");
        Files.writeString(src.resolve("BillingReport.java"), "public class BillingReport {}");

        Set<String> keywords = Set.of("login", "validator");
        ContextManager.RelevantFiles result = contextManager.findRelevantFiles(tempDir, keywords);

        // Should include LoginValidator in sources and LoginValidatorTest in tests
        assertTrue(result.sources().stream().anyMatch(s -> s.contains("LoginValidator.java")));
        assertTrue(result.tests().stream().anyMatch(t -> t.contains("LoginValidatorTest.java")));
        assertTrue(result.configs().stream().anyMatch(c -> c.contains("pom.xml")));

        // Should NOT include unrelated files
        assertFalse(result.sources().stream().anyMatch(s -> s.contains("InvoiceService.java")));
        assertFalse(result.sources().stream().anyMatch(s -> s.contains("BillingReport.java")));
    }

    @Test
    void testBuildPrioritizedContext(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("AuthService.java"), "class AuthService {}");

        AgentState state = new AgentState("t-1", "Fix authentication failure", tempDir.toString(), 3);
        state.recordFileModified("src/AuthService.java");
        state.recordCommandExecuted("terminal: mvn test");
        state.recordError("AuthenticationException at line 42");

        RepositoryContext repoContext = contextManager.inspectRepository(tempDir.toString());
        StructuredPlan plan = planner.createPlan("t-1", state.getTask(), repoContext);

        List<ChatMessage> messages = contextManager.buildPrioritizedContext(state, repoContext, plan);

        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).role());
        assertEquals("user", messages.get(1).role());

        String userContent = messages.get(1).content();
        assertTrue(userContent.contains("=== 1. USER TASK ==="));
        assertTrue(userContent.contains("Fix authentication failure"));
        assertTrue(userContent.contains("=== 2. REPOSITORY STRUCTURE ==="));
        assertTrue(userContent.contains("=== 6. IMPLEMENTATION PLAN ==="));
        assertTrue(userContent.contains("=== 7. PREVIOUS TOOL RESULTS ==="));
        assertTrue(userContent.contains("=== 8. RECENT ERRORS ==="));
        assertTrue(userContent.contains("AuthenticationException at line 42"));
    }

    @Test
    void testTokenOptimizationDeduplicatesFileContentsAcrossTurns(@TempDir Path tempDir) throws IOException {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("AuthService.java"), "public class AuthService { public void login() {} }");

        AgentState state = new AgentState("t-opt-1", "Update AuthService login", tempDir.toString(), 3);
        RepositoryContext repoContext = contextManager.inspectRepository(tempDir.toString());
        StructuredPlan plan = planner.createPlan("t-opt-1", state.getTask(), repoContext);

        // Turn 1: Should sample file content
        List<ChatMessage> turn1 = contextManager.buildPrioritizedContext(state, repoContext, plan);
        String turn1User = turn1.get(1).content();
        assertTrue(turn1User.contains("AuthService.java"));
        assertTrue(turn1User.contains("public void login()"));

        // Turn 2: Should NOT dump file content again; should mark as already inspected
        List<ChatMessage> turn2 = contextManager.buildPrioritizedContext(state, repoContext, plan);
        String turn2User = turn2.get(1).content();
        assertTrue(turn2User.contains("already inspected"));
        assertFalse(turn2User.contains("public void login()"));

        // Cleanup
        contextManager.cleanupTask("t-opt-1");
    }

    @Test
    void testTokenOptimizationDirectsFinishImmediatelyWhenTestsPass(@TempDir Path tempDir) throws IOException {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Calculator.java"), "public class Calculator {}");

        AgentState state = new AgentState("t-opt-2", "Add subtract method to Calculator", tempDir.toString(), 3);
        state.recordFileModified("src/Calculator.java");
        state.recordActionResult(com.example.codingagent.agent.ActionResult.success(
                com.example.codingagent.model.action.ActionType.RUN_TESTS,
                "mvn test",
                "Tests run: 3, Failures: 0, Errors: 0, Skipped: 0",
                100L
        ));

        RepositoryContext repoContext = contextManager.inspectRepository(tempDir.toString());
        StructuredPlan plan = planner.createPlan("t-opt-2", state.getTask(), repoContext);

        List<ChatMessage> messages = contextManager.buildPrioritizedContext(state, repoContext, plan);
        String userContent = messages.get(1).content();

        assertTrue(userContent.contains("Automated tests already passed!"));
        assertTrue(userContent.contains("FINISH"));
    }

    @Test
    void testConciseOutputFilteringIrrelevantLogs() {
        String noisyMavenLog = """
                [INFO] Scanning for projects...
                [INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/apache/commons/1.0.jar
                [INFO] Progress (1): 2.0/4.0 kB
                [INFO] Progress (2): 4.0 kB
                [INFO] --- maven-compiler-plugin:3.13.0:compile (default-compile) @ project ---
                [ERROR] /src/main/java/App.java:[10,5] cannot find symbol
                  symbol:   variable missingVar
                  location: class App
                [INFO] -------------------------------------------------------------
                [INFO] BUILD FAILURE
                """;

        String concise = contextManager.extractConciseOutput(noisyMavenLog, 300);

        assertFalse(concise.contains("Downloading from central"));
        assertFalse(concise.contains("Progress"));
        assertTrue(concise.contains("cannot find symbol") || concise.contains("BUILD FAILURE"));
        assertTrue(concise.length() <= 300);
    }
}
