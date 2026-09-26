package com.example.codingagent.testing;

import com.example.codingagent.tools.RepositoryTool;
import com.example.codingagent.tools.SecurityGuard;
import com.example.codingagent.tools.TerminalTool;
import com.example.codingagent.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class TestRunnerTest {

    private TerminalTool terminalTool;
    private RepositoryTool repositoryTool;
    private TestRunner testRunner;

    @BeforeEach
    void setUp() {
        terminalTool = Mockito.mock(TerminalTool.class);
        SecurityGuard guard = new SecurityGuard();
        repositoryTool = new RepositoryTool(guard);
        testRunner = new TestRunner(terminalTool, repositoryTool);
    }

    @Test
    void testDetectTestCommandsMultiFramework(@TempDir Path tempDir) throws IOException {
        // Maven
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");
        assertEquals("mvn test", testRunner.detectTestCommand(tempDir.toString(), null));
        assertEquals("mvn test -Dtest=UserServiceTest", testRunner.detectTestCommand(tempDir.toString(), "UserServiceTest"));
        Files.delete(tempDir.resolve("pom.xml"));

        // Gradle
        Files.writeString(tempDir.resolve("build.gradle"), "// gradle");
        assertEquals("gradle test", testRunner.detectTestCommand(tempDir.toString(), null));
        Files.createFile(tempDir.resolve("gradlew"));
        assertEquals("./gradlew test", testRunner.detectTestCommand(tempDir.toString(), null));
        Files.delete(tempDir.resolve("build.gradle"));
        Files.delete(tempDir.resolve("gradlew"));

        // Node.js
        Files.writeString(tempDir.resolve("package.json"), "{\"scripts\": {\"test\": \"jest\"}}");
        assertEquals("npm test", testRunner.detectTestCommand(tempDir.toString(), null));
        assertEquals("npm test -- auth.test.ts", testRunner.detectTestCommand(tempDir.toString(), "auth.test.ts"));
        Files.delete(tempDir.resolve("package.json"));

        Files.writeString(tempDir.resolve("package.json"), "{}");
        assertNull(testRunner.detectTestCommand(tempDir.toString(), null));
        Files.delete(tempDir.resolve("package.json"));

        // Rust
        Files.writeString(tempDir.resolve("Cargo.toml"), "[package]");
        assertEquals("cargo test", testRunner.detectTestCommand(tempDir.toString(), null));
        Files.delete(tempDir.resolve("Cargo.toml"));

        // Go
        Files.writeString(tempDir.resolve("go.mod"), "module example");
        assertEquals("go test ./...", testRunner.detectTestCommand(tempDir.toString(), null));
        Files.delete(tempDir.resolve("go.mod"));
    }

    @Test
    void testParseRealMavenPassingOutput() {
        String stdout = """
                [INFO] Scanning for projects...
                [INFO] Running com.example.UserServiceTest
                [INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.234 s
                [INFO] Results:
                [INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
                [INFO] BUILD SUCCESS
                """;

        TestResult result = testRunner.parseTestOutput("mvn test", 0, stdout, "", 1234L, "Java/Maven");

        assertEquals(TestStatus.PASSED, result.status());
        assertTrue(result.isSuccess());
        assertTrue(result.hasParsedMetrics());
        assertEquals(14, result.totalTests());
        assertEquals(14, result.passedTests());
        assertEquals(0, result.failedTests());
        assertEquals(0, result.skippedTests());
        assertEquals(0, result.exitCode());
        assertTrue(result.failures().isEmpty());
    }

    @Test
    void testParseRealMavenFailingOutput() {
        String stdout = """
                [INFO] Running com.example.AuthServiceTest
                [ERROR] Failures: 
                [ERROR]   AuthServiceTest.testLoginFailure:42 expected: <401> but was: <200>
                [INFO] Results:
                [INFO] Tests run: 9, Failures: 2, Errors: 0, Skipped: 1
                [INFO] BUILD FAILURE
                """;

        TestResult result = testRunner.parseTestOutput("mvn test", 1, stdout, "", 2300L, "Java/Maven");

        assertEquals(TestStatus.FAILED, result.status());
        assertFalse(result.isSuccess());
        assertTrue(result.hasParsedMetrics());
        assertEquals(9, result.totalTests());
        assertEquals(6, result.passedTests());
        assertEquals(2, result.failedTests());
        assertEquals(1, result.skippedTests());
        assertEquals(1, result.exitCode());
        assertFalse(result.failures().isEmpty());
        assertTrue(result.failures().getFirst().message().contains("expected: <401> but was: <200>"));
    }

    @Test
    void testParseRealJestPassingAndFailingOutput() {
        // Jest Passing
        String jestPass = """
                PASS tests/unit/auth.test.ts
                PASS tests/unit/token.test.ts
                Test Suites: 2 passed, 2 total
                Tests:       8 passed, 8 total
                Snapshots:   0 total
                Time:        2.105 s
                """;
        TestResult passResult = testRunner.parseTestOutput("npm test", 0, jestPass, "", 2105L, "Node.js");
        assertEquals(TestStatus.PASSED, passResult.status());
        assertTrue(passResult.isSuccess());
        assertEquals(8, passResult.totalTests());
        assertEquals(8, passResult.passedTests());
        assertEquals(0, passResult.failedTests());

        // Jest Failing
        String jestFail = """
                FAIL tests/unit/payment.test.ts
                  ● PaymentService › should charge credit card
                    expect(received).toBe(expected) // Object.is equality
                    Expected: "success"
                    Received: "declined"
                Tests:       2 failed, 10 passed, 12 total
                Time:        3.421 s
                """;
        TestResult failResult = testRunner.parseTestOutput("npm test", 1, jestFail, "", 3421L, "Node.js");
        assertEquals(TestStatus.FAILED, failResult.status());
        assertFalse(failResult.isSuccess());
        assertEquals(12, failResult.totalTests());
        assertEquals(10, failResult.passedTests());
        assertEquals(2, failResult.failedTests());
        assertFalse(failResult.failures().isEmpty());
    }

    @Test
    void testUnparseableOutputPreservesRawMetricsWithoutInventingNumbers() {
        String stdout = "Running custom shell verification script...\nAll 5 components verified cleanly.\nDone.";
        String stderr = "";

        TestResult result = testRunner.parseTestOutput("./verify.sh", 0, stdout, stderr, 450L, "Custom");

        assertEquals(TestStatus.PASSED, result.status());
        assertTrue(result.isSuccess());
        // CRITICAL: We do NOT invent or hardcode numbers like "27 tests passed"
        assertFalse(result.hasParsedMetrics());
        assertNull(result.totalTests());
        assertNull(result.passedTests());
        assertNull(result.failedTests());
        assertEquals(stdout, result.stdout());
        assertEquals(0, result.exitCode());
    }

    @Test
    void testCommandFailureNonZeroExitCode() {
        String stdout = "";
        String stderr = "bash: mvn: command not found";

        TestResult result = testRunner.parseTestOutput("mvn test", 127, stdout, stderr, 50L, "Java/Maven");

        assertEquals(TestStatus.ERROR, result.status());
        assertFalse(result.isSuccess());
        assertFalse(result.hasParsedMetrics());
        assertEquals(127, result.exitCode());
        assertEquals(stderr, result.stderr());
        assertFalse(result.failures().isEmpty());
        assertTrue(result.failures().getFirst().message().contains("command not found"));
    }

    @Test
    void testExecutionThroughTestRunnerDelegatesToTerminal(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");

        String output = "[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0\n[INFO] BUILD SUCCESS";
        when(terminalTool.execute(any(), anyString()))
                .thenReturn(ToolResult.success(output, Map.of("exitCode", 0)));

        TestResult res = testRunner.runTests(tempDir.toString(), null, null);

        assertEquals(TestStatus.PASSED, res.status());
        assertEquals(5, res.totalTests());
        assertEquals(5, res.passedTests());
    }
}
