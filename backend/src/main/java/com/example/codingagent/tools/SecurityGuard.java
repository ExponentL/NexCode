package com.example.codingagent.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SecurityGuard {

    private static final Logger log = LoggerFactory.getLogger(SecurityGuard.class);

    // Dangerous command patterns to prevent catastrophic damage or arbitrary system compromise
    private static final List<Pattern> FORBIDDEN_COMMAND_PATTERNS = List.of(
            Pattern.compile("rm\\s+-rf\\s+/[^\\w]*", Pattern.CASE_INSENSITIVE), // rm -rf /
            Pattern.compile("rm\\s+-rf\\s+~", Pattern.CASE_INSENSITIVE),        // rm -rf ~
            Pattern.compile(":\\(\\)\\s*\\{.*\\};\\s*:", Pattern.CASE_INSENSITIVE), // fork bomb
            Pattern.compile("mkfs.*", Pattern.CASE_INSENSITIVE),               // disk formatting
            Pattern.compile("dd\\s+if=.*of=/dev/.*", Pattern.CASE_INSENSITIVE), // disk write
            Pattern.compile("curl\\s+.*\\|\\s*(ba)?sh", Pattern.CASE_INSENSITIVE), // curl pipe to shell
            Pattern.compile("wget\\s+.*\\|\\s*(ba)?sh", Pattern.CASE_INSENSITIVE), // wget pipe to shell
            Pattern.compile("shutdown\\s+.*", Pattern.CASE_INSENSITIVE),       // system shutdown
            Pattern.compile("reboot\\s*.*", Pattern.CASE_INSENSITIVE)          // reboot
    );

    // Patterns for sensitive keys, tokens, credentials
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|secret|token|password|bearer|authorization)[:=\\s]+(['\"]?)([a-zA-Z0-9_\\-\\.~]{8,})\\2"
    );

    /**
     * Validates that targetPath is strictly within the boundaries of workingDirectory.
     * Prevents directory traversal attacks (e.g., ../../etc/passwd).
     */
    public Path validateAndResolvePath(String workingDirectory, String relativePath) {
        if (workingDirectory == null || workingDirectory.isBlank()) {
            throw new SecurityException("Working directory is not specified.");
        }
        if (relativePath == null || relativePath.isBlank()) {
            throw new SecurityException("Target path cannot be empty.");
        }

        String cleanRel = relativePath.trim();
        if (cleanRel.startsWith("~")) {
            throw new SecurityException("Path traversal outside workspace boundary is forbidden: " + cleanRel);
        }

        Path base = Paths.get(workingDirectory).toAbsolutePath().normalize();
        Path baseReal = null;
        try {
            baseReal = Paths.get(workingDirectory).toRealPath().normalize();
        } catch (IOException ignored) {}

        Path target;
        Path rawPath = Paths.get(cleanRel);
        if (rawPath.isAbsolute()) {
            Path rawNorm = rawPath.normalize();
            // Check if it directly starts with base or baseReal
            if (rawNorm.startsWith(base) || (baseReal != null && rawNorm.startsWith(baseReal))) {
                target = rawNorm;
            } else {
                // Check if this points to an existing system file outside the workspace
                File outsideFile = new File(cleanRel);
                if (outsideFile.exists()) {
                    log.warn("Blocked attempt to access system file outside workspace: '{}' from base '{}'", cleanRel, base);
                    throw new SecurityException("Path traversal outside workspace boundary is forbidden: " + cleanRel);
                }
                // Otherwise, treat leading slashes as repo-root-relative (e.g. "/src/app.js")
                String stripped = cleanRel.replaceFirst("^/+", "");
                target = base.resolve(stripped).normalize();
            }
        } else {
            target = base.resolve(cleanRel).normalize();
        }

        // Verify target is strictly within base or baseReal
        boolean withinBase = target.startsWith(base) || (baseReal != null && target.startsWith(baseReal));
        if (!withinBase) {
            log.warn("Blocked path traversal attempt: '{}' from base '{}'", relativePath, base);
            throw new SecurityException("Path traversal outside workspace boundary is forbidden: " + relativePath);
        }

        return target;
    }

    /**
     * Validates that the shell command does not contain dangerous destructive patterns.
     */
    public void validateCommand(String command) {
        if (command == null || command.isBlank()) {
            throw new SecurityException("Command cannot be blank.");
        }

        for (Pattern forbidden : FORBIDDEN_COMMAND_PATTERNS) {
            if (forbidden.matcher(command).find()) {
                log.error("Dangerous command blocked by SecurityGuard: {}", command);
                throw new SecurityException("Command blocked due to safety restrictions: " + command);
            }
        }
    }

    /**
     * Redacts secrets and API keys from text before exposing to model or events.
     */
    public String redactSecrets(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        // Redact standard secret pattern
        String redacted = SECRET_PATTERN.matcher(text).replaceAll("$1: \"[REDACTED]\"");

        // Also redact any matching environment secrets from current process
        for (var entry : System.getenv().entrySet()) {
            String val = entry.getValue();
            if (val != null && val.length() >= 8 && entry.getKey().toLowerCase().matches(".*(key|secret|token|password).*")) {
                redacted = redacted.replace(val, "[REDACTED_" + entry.getKey() + "]");
            }
        }

        return redacted;
    }
}
