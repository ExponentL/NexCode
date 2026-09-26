package com.example.codingagent.tools;

import com.example.codingagent.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class TerminalTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(TerminalTool.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 90;
    private static final int MAX_OUTPUT_CHARS = 100_000;

    private final SecurityGuard securityGuard;
    private final ExecutableVerifier executableVerifier;

    public TerminalTool(SecurityGuard securityGuard) {
        this(securityGuard, new ExecutableVerifier());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TerminalTool(SecurityGuard securityGuard, ExecutableVerifier executableVerifier) {
        this.securityGuard = securityGuard;
        this.executableVerifier = executableVerifier != null ? executableVerifier : new ExecutableVerifier();
    }

    @Override
    public String getName() {
        return "terminal_tool";
    }

    @Override
    public String getDescription() {
        return "Execute permitted shell commands in repository working directory. Returns stdout, stderr, exitCode, durationMs.";
    }

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
                getName(),
                getDescription(),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "command", Map.of("type", "string", "description", "Shell command to execute"),
                                "timeoutSeconds", Map.of("type", "integer", "description", "Execution timeout in seconds")
                        ),
                        "required", List.of("command")
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, String workingDirectory) {
        String command = (String) arguments.get("command");
        if (command == null || command.isBlank()) {
            return ToolResult.failure("Parameter 'command' is required.");
        }

        // Validate command safety
        try {
            securityGuard.validateCommand(command);
        } catch (SecurityException se) {
            return ToolResult.failure("Command rejected by security guard: " + se.getMessage());
        }

        File workDir = new File(workingDirectory).getAbsoluteFile();
        if (!workDir.exists() || !workDir.isDirectory()) {
            return ToolResult.failure("Invalid working directory: " + workingDirectory);
        }

        String executable = executableVerifier.extractExecutable(command);
        if (executable != null && !executableVerifier.isShellBuiltin(executable)) {
            // Check if executable is 'python' but only 'python3' is available on system
            if (executable.equals("python") && !executableVerifier.isExecutableAvailable("python", workingDirectory)
                    && executableVerifier.isExecutableAvailable("python3", workingDirectory)) {
                log.info("Auto-adapting 'python' command to 'python3' for execution");
                command = command.replaceFirst("^python\\b", "python3");
                executable = "python3";
            }

            if (!executableVerifier.isExecutableAvailable(executable, workingDirectory)) {
                String errorMsg = "Command not found (exit code 127): Executable '" + executable + "' does not exist or is not available on PATH.";
                log.warn("Executable pre-verification failed: {}", errorMsg);
                Map<String, Object> meta = new HashMap<>();
                meta.put("exitCode", 127);
                meta.put("command", command);
                meta.put("durationMs", 0L);
                meta.put("stderr", errorMsg);
                return ToolResult.failure(errorMsg, "", meta);
            }
        }

        int timeout = arguments.get("timeoutSeconds") instanceof Number n ? n.intValue() : DEFAULT_TIMEOUT_SECONDS;
        log.info("Executing command: '{}' in {}", command, workDir.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
        pb.directory(workDir);

        // Inherit path and ensure all discovered tool directories are included
        Map<String, String> env = pb.environment();
        String fullPath = String.join(":", executableVerifier.getSearchPaths());
        env.put("PATH", fullPath);

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        long startTime = System.currentTimeMillis();

        try {
            Process process = pb.start();

            Thread outThread = new Thread(() -> readStream(process.getInputStream(), stdout));
            Thread errThread = new Thread(() -> readStream(process.getErrorStream(), stderr));
            outThread.start();
            errThread.start();

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

            outThread.join(2000);
            errThread.join(2000);

            long durationMs = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return ToolResult.failure("Command timed out after " + timeout + " seconds.", stdout.toString());
            }

            int exitCode = process.exitValue();
            String cleanStdout = securityGuard.redactSecrets(stdout.toString());
            String cleanStderr = securityGuard.redactSecrets(stderr.toString());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("exitCode", exitCode);
            metadata.put("command", command);
            metadata.put("durationMs", durationMs);
            metadata.put("stderr", cleanStderr);

            if (exitCode == 0) {
                return ToolResult.success(cleanStdout, metadata);
            } else if (exitCode == 127) {
                String detail = !cleanStderr.isBlank() ? cleanStderr.trim() : "Executable '" + executable + "' not found on system PATH";
                String errorMsg = "Command not found (exit code 127): " + detail;
                return ToolResult.failure(errorMsg, cleanStdout, metadata);
            } else {
                String errorMsg = !cleanStderr.isBlank() ? cleanStderr.trim() : "Process exited with code " + exitCode;
                return ToolResult.failure(errorMsg, cleanStdout, metadata);
            }

        } catch (Exception e) {
            log.error("Execution error for command '{}': {}", command, e.getMessage());
            return ToolResult.failure("Command execution exception: " + e.getMessage());
        }
    }

    private void readStream(java.io.InputStream stream, StringBuilder buffer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (buffer.length() < MAX_OUTPUT_CHARS) {
                    buffer.append(line).append("\n");
                }
            }
        } catch (Exception ignored) {
        }
    }
}
