package com.example.codingagent.tools;

import com.example.codingagent.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TestTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(TestTool.class);

    private final TerminalTool terminalTool;
    private final RepositoryTool repositoryTool;

    public TestTool(TerminalTool terminalTool, RepositoryTool repositoryTool) {
        this.terminalTool = terminalTool;
        this.repositoryTool = repositoryTool;
    }

    @Override
    public String getName() {
        return "test_tool";
    }

    @Override
    public String getDescription() {
        return "Execute real automated test suites across Java/Maven, Java/Gradle, Node.js, .NET, C/C++, and parse actual test outcomes.";
    }

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
                getName(),
                getDescription(),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "command", Map.of("type", "string", "description", "Explicit test command to run. If omitted, automatically detects framework."),
                                "filter", Map.of("type", "string", "description", "Optional class or name filter to run specific tests")
                        )
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, String workingDirectory) {
        String explicitCommand = (String) arguments.get("command");
        String filter = (String) arguments.get("filter");

        String commandToRun = explicitCommand;
        if (commandToRun == null || commandToRun.isBlank()) {
            commandToRun = detectTestCommand(workingDirectory, filter);
        }

        if (commandToRun == null) {
            Map<String, Object> meta = Map.of("noTests", true);
            return ToolResult.failure("No test suite detected in repository. Please provide an explicit 'command' parameter.", "", meta);
        }

        log.info("TestTool running command: '{}' in {}", commandToRun, workingDirectory);
        long start = System.currentTimeMillis();

        ToolResult execution = terminalTool.execute(
                Map.of("command", commandToRun, "timeoutSeconds", 180),
                workingDirectory
        );

        long durationMs = System.currentTimeMillis() - start;
        String rawOutput = (execution.output() != null ? execution.output() : "") +
                (execution.error() != null ? "\n" + execution.error() : "");

        Map<String, Object> parsedMetrics = parseRealTestResults(rawOutput);
        Map<String, Object> metadata = new HashMap<>(parsedMetrics);
        metadata.put("command", commandToRun);
        metadata.put("durationMs", durationMs);
        metadata.put("exitCode", execution.metadata() != null ? execution.metadata().get("exitCode") : -1);

        if (execution.success()) {
            return ToolResult.success(rawOutput, metadata);
        } else {
            return ToolResult.failure(rawOutput, rawOutput, metadata);
        }
    }

    public String detectTestCommand(String workingDirectory, String filter) {
        File dir = new File(workingDirectory);

        // 1. Java / Maven
        if (new File(dir, "pom.xml").exists()) {
            if (filter != null && !filter.isBlank()) {
                return "mvn test -Dtest=" + filter;
            }
            return "mvn test";
        }

        // 2. Java / Gradle
        if (new File(dir, "build.gradle").exists() || new File(dir, "build.gradle.kts").exists()) {
            boolean hasWrapper = new File(dir, "gradlew").exists();
            String prefix = hasWrapper ? "./gradlew" : "gradle";
            if (filter != null && !filter.isBlank()) {
                return prefix + " test --tests " + filter;
            }
            return prefix + " test";
        }

        // 3. Node.js (package.json) - only if a valid test script is configured
        if (new File(dir, "package.json").exists()) {
            if (hasValidNodeTestScript(new File(dir, "package.json"))) {
                if (filter != null && !filter.isBlank()) {
                    return "npm test -- " + filter;
                }
                return "npm test";
            }
        }

        // 4. .NET (*.csproj, *.sln)
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".csproj") || f.getName().endsWith(".sln")) {
                    if (filter != null && !filter.isBlank()) {
                        return "dotnet test --filter " + filter;
                    }
                    return "dotnet test";
                }
            }
        }

        // 5. C/C++ (CMake / CTest)
        if (new File(dir, "CMakeLists.txt").exists()) {
            return "ctest --output-on-failure";
        }
        if (new File(dir, "Makefile").exists()) {
            return "make test";
        }

        // 6. Rust
        if (new File(dir, "Cargo.toml").exists()) {
            if (filter != null && !filter.isBlank()) {
                return "cargo test " + filter;
            }
            return "cargo test";
        }

        // 7. Go
        if (new File(dir, "go.mod").exists()) {
            return "go test ./...";
        }

        // 8. Python (pytest / unittest)
        if (new File(dir, "pytest.ini").exists() || new File(dir, "pyproject.toml").exists() || new File(dir, "setup.py").exists()) {
            return "pytest";
        }
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith("test_") && f.getName().endsWith(".py")) {
                    return "python3 -m unittest discover -s . -p 'test_*.py'";
                }
                if (f.getName().endsWith("_test.py")) {
                    return "python3 -m unittest discover -s . -p '*_test.py'";
                }
            }
        }

        return null;
    }

    private boolean hasValidNodeTestScript(File packageJson) {
        try {
            String content = Files.readString(packageJson.toPath());
            Pattern pattern = Pattern.compile("\"test\"\\s*:\\s*\"(.*?)(?<!\\\\)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String script = matcher.group(1).toLowerCase().replace("\\\"", "\"").trim();
                if (script.isBlank() || script.contains("no test specified") || script.contains("exit 1")) {
                    return false;
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parses real test output lines without hardcoding counts or outcomes.
     */
    public Map<String, Object> parseRealTestResults(String output) {
        Map<String, Object> stats = new HashMap<>();
        int testsRun = 0;
        int failures = 0;
        int errors = 0;
        int skipped = 0;
        int passed = 0;

        // 1. Maven Surefire: Tests run: 5, Failures: 1, Errors: 0, Skipped: 0
        Pattern surefire = Pattern.compile("Tests run:\\s*(\\d+),\\s*Failures:\\s*(\\d+),\\s*Errors:\\s*(\\d+),\\s*Skipped:\\s*(\\d+)");
        Matcher sm = surefire.matcher(output);
        while (sm.find()) {
            testsRun = Integer.parseInt(sm.group(1));
            failures = Integer.parseInt(sm.group(2));
            errors = Integer.parseInt(sm.group(3));
            skipped = Integer.parseInt(sm.group(4));
        }

        // 2. Gradle: 12 tests completed, 2 failed, 1 skipped
        Pattern gradle = Pattern.compile("(\\d+)\\s+tests?\\s+completed,\\s*(\\d+)\\s+failed,\\s*(\\d+)\\s+skipped");
        Matcher gm = gradle.matcher(output);
        if (gm.find()) {
            testsRun = Math.max(testsRun, Integer.parseInt(gm.group(1)));
            failures = Math.max(failures, Integer.parseInt(gm.group(2)));
            skipped = Math.max(skipped, Integer.parseInt(gm.group(3)));
        }

        // 3. Jest / Vitest: Tests: 2 failed, 10 passed, 12 total
        Pattern jest = Pattern.compile("Tests:\\s*(?:(\\d+)\\s*failed,)?\\s*(?:(\\d+)\\s*passed,)?\\s*(\\d+)\\s*total");
        Matcher jm = jest.matcher(output);
        if (jm.find()) {
            if (jm.group(1) != null) failures = Math.max(failures, Integer.parseInt(jm.group(1)));
            if (jm.group(2) != null) passed = Math.max(passed, Integer.parseInt(jm.group(2)));
            int total = Integer.parseInt(jm.group(3));
            testsRun = Math.max(testsRun, total);
        }

        // 4. .NET (Flexible order: Failed: 0, Passed: 12, Skipped: 0, Total: 12)
        Matcher totM = Pattern.compile("Total:\\s*(\\d+)").matcher(output);
        Matcher passM = Pattern.compile("Passed:\\s*(\\d+)").matcher(output);
        Matcher failM = Pattern.compile("Failed:\\s*(\\d+)").matcher(output);
        Matcher skipM = Pattern.compile("Skipped:\\s*(\\d+)").matcher(output);
        if (totM.find() && (passM.find() || failM.find())) {
            testsRun = Math.max(testsRun, Integer.parseInt(totM.group(1)));
            passM.reset();
            if (passM.find()) passed = Math.max(passed, Integer.parseInt(passM.group(1)));
            failM.reset();
            if (failM.find()) failures = Math.max(failures, Integer.parseInt(failM.group(1)));
            skipM.reset();
            if (skipM.find()) skipped = Math.max(skipped, Integer.parseInt(skipM.group(1)));
        }

        // 5. Python unittest (Ran 3 tests in 0.001s, FAILED (failures=1))
        Pattern pyRan = Pattern.compile("Ran\\s+(\\d+)\\s+tests?\\s+in\\s+[\\d\\.]+s");
        Matcher pyM = pyRan.matcher(output);
        if (pyM.find()) {
            testsRun = Math.max(testsRun, Integer.parseInt(pyM.group(1)));
            Pattern pyFail = Pattern.compile("FAILED\\s*\\((?:failures=(\\d+))?(?:,?\\s*errors=(\\d+))?(?:,?\\s*skipped=(\\d+))?\\)");
            Matcher pyFailM = pyFail.matcher(output);
            if (pyFailM.find()) {
                if (pyFailM.group(1) != null) failures = Math.max(failures, Integer.parseInt(pyFailM.group(1)));
                if (pyFailM.group(2) != null) errors = Math.max(errors, Integer.parseInt(pyFailM.group(2)));
                if (pyFailM.group(3) != null) skipped = Math.max(skipped, Integer.parseInt(pyFailM.group(3)));
            }
        }

        if (passed == 0 && testsRun > 0) {
            passed = Math.max(0, testsRun - failures - errors - skipped);
        }

        stats.put("testsRun", testsRun);
        stats.put("passed", passed);
        stats.put("failures", failures);
        stats.put("errors", errors);
        stats.put("skipped", skipped);
        stats.put("hasParsedMetrics", testsRun > 0);

        return stats;
    }
}
