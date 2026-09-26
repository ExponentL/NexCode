package com.example.codingagent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestToolTest {

    private TestTool testTool;

    @BeforeEach
    void setUp() {
        SecurityGuard guard = new SecurityGuard();
        TerminalTool terminalTool = new TerminalTool(guard);
        RepositoryTool repositoryTool = new RepositoryTool(guard);
        testTool = new TestTool(terminalTool, repositoryTool);
    }

    @Test
    void testDetectTestCommands(@TempDir Path tempDir) throws IOException {
        // Maven
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");
        assertEquals("mvn test", testTool.detectTestCommand(tempDir.toString(), null));
        assertEquals("mvn test -Dtest=AuthTest", testTool.detectTestCommand(tempDir.toString(), "AuthTest"));

        Files.delete(tempDir.resolve("pom.xml"));

        // Node.js with valid test script
        Files.writeString(tempDir.resolve("package.json"), "{\"scripts\": {\"test\": \"jest\"}}");
        assertEquals("npm test", testTool.detectTestCommand(tempDir.toString(), null));
        Files.delete(tempDir.resolve("package.json"));

        // Node.js with empty / no test script returns null
        Files.writeString(tempDir.resolve("package.json"), "{}");
        assertNull(testTool.detectTestCommand(tempDir.toString(), null));
        Files.delete(tempDir.resolve("package.json"));

        // Node.js with npm init placeholder returns null
        Files.writeString(tempDir.resolve("package.json"), "{\"scripts\": {\"test\": \"echo \\\"Error: no test specified\\\" && exit 1\"}}");
        assertNull(testTool.detectTestCommand(tempDir.toString(), null));
        Files.delete(tempDir.resolve("package.json"));

        // Gradle
        Files.writeString(tempDir.resolve("build.gradle"), "// gradle");
        Files.createFile(tempDir.resolve("gradlew"));
        assertEquals("./gradlew test", testTool.detectTestCommand(tempDir.toString(), null));
        Files.delete(tempDir.resolve("build.gradle"));
        Files.delete(tempDir.resolve("gradlew"));

        // Python unittest
        Files.writeString(tempDir.resolve("test_calculator.py"), "import unittest");
        String pythonCmd = testTool.detectTestCommand(tempDir.toString(), null);
        assertNotNull(pythonCmd);
        assertTrue(pythonCmd.contains("unittest") || pythonCmd.contains("pytest"));
        Files.delete(tempDir.resolve("test_calculator.py"));
    }

    @Test
    void testParseRealMavenSurefireOutput() {
        String output = """
                [INFO] Running com.example.AuthTest
                [INFO] Tests run: 8, Failures: 1, Errors: 0, Skipped: 1, Time elapsed: 0.123 s
                [INFO] Results:
                [INFO] Tests run: 8, Failures: 1, Errors: 0, Skipped: 1
                """;

        Map<String, Object> stats = testTool.parseRealTestResults(output);

        assertEquals(8, stats.get("testsRun"));
        assertEquals(1, stats.get("failures"));
        assertEquals(0, stats.get("errors"));
        assertEquals(1, stats.get("skipped"));
        assertEquals(6, stats.get("passed"));
        assertTrue((Boolean) stats.get("hasParsedMetrics"));
    }

    @Test
    void testParseRealJestOutput() {
        String output = """
                PASS src/auth.test.ts
                FAIL src/payment.test.ts
                Tests:       1 failed, 7 passed, 8 total
                Time:        2.345 s
                """;

        Map<String, Object> stats = testTool.parseRealTestResults(output);

        assertEquals(8, stats.get("testsRun"));
        assertEquals(1, stats.get("failures"));
        assertEquals(7, stats.get("passed"));
    }

    @Test
    void testParseRealDotnetOutput() {
        String output = """
                Passed! - Failed: 0, Passed: 12, Skipped: 0, Total: 12, Duration: 450 ms
                """;

        Map<String, Object> stats = testTool.parseRealTestResults(output);

        assertEquals(12, stats.get("testsRun"));
        assertEquals(0, stats.get("failures"));
        assertEquals(12, stats.get("passed"));
    }

    @Test
    void testExecuteWhenNoTestSuiteDetected(@TempDir Path tempDir) {
        ToolResult res = testTool.execute(Map.of(), tempDir.toString());

        assertFalse(res.success());
        assertTrue(res.error().contains("No test suite detected"));
        assertNotNull(res.metadata());
        assertTrue((Boolean) res.metadata().get("noTests"));
    }

    @Test
    void testParseRealPythonUnittestOutput() {
        String output = """
                FAIL: test_add (test_calculator.TestCalculator.test_add)
                ----------------------------------------------------------------------
                AssertionError: -1 != 5
                ----------------------------------------------------------------------
                Ran 3 tests in 0.001s

                FAILED (failures=1)
                """;

        Map<String, Object> stats = testTool.parseRealTestResults(output);

        assertEquals(3, stats.get("testsRun"));
        assertEquals(1, stats.get("failures"));
        assertEquals(2, stats.get("passed"));
        assertTrue((Boolean) stats.get("hasParsedMetrics"));
    }
}
