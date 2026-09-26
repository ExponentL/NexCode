package com.example.codingagent.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

@Component
public class ExecutableVerifier {

    private static final Logger log = LoggerFactory.getLogger(ExecutableVerifier.class);

    private static final Set<String> SHELL_BUILTINS = Set.of(
            "echo", "cd", "pwd", "export", "source", "alias", "unalias",
            "set", "unset", "true", "false", "test", "exit", "return",
            "eval", "exec", "type", "hash", "help", "history", "jobs",
            "kill", "read", "shift", "times", "trap", "ulimit", "umask",
            "wait", "for", "while", "until", "if", "then", "else", "elif",
            "fi", "case", "esac", "do", "done", "let", "local"
    );

    /**
     * Extracts the primary executable binary name from a shell command string.
     * E.g. "mvn test" -> "mvn", "./gradlew build" -> "./gradlew", "python3 -c '...'" -> "python3"
     */
    public String extractExecutable(String command) {
        if (command == null || command.isBlank()) return null;
        String trimmed = command.trim();

        // Handle leading environment variable assignments (e.g., "FOO=bar cmd arg")
        while (trimmed.matches("^[a-zA-Z_][a-zA-Z0-9_]*=.*")) {
            int spaceIdx = trimmed.indexOf(' ');
            if (spaceIdx > 0 && spaceIdx < trimmed.length() - 1) {
                trimmed = trimmed.substring(spaceIdx + 1).trim();
            } else {
                break;
            }
        }

        // Handle command chains or subshells (take first command before pipe/&&/;)
        String firstCmd = trimmed.split("[;&|]")[0].trim();
        if (firstCmd.isBlank()) return null;

        // Split by whitespace
        String[] tokens = firstCmd.split("\\s+");
        return tokens.length > 0 ? tokens[0].trim() : null;
    }

    public boolean isShellBuiltin(String executable) {
        if (executable == null) return false;
        return SHELL_BUILTINS.contains(executable.toLowerCase());
    }

    /**
     * Verifies that the required executable exists on the system or in the workspace.
     */
    public boolean isExecutableAvailable(String executableName, String workingDirectory) {
        if (executableName == null || executableName.isBlank()) return false;
        String name = executableName.trim();

        if (isShellBuiltin(name)) {
            return true;
        }

        // Relative path or absolute path
        if (name.startsWith("./") || name.startsWith("/") || name.contains(File.separator)) {
            File target = name.startsWith("/")
                    ? new File(name)
                    : new File(workingDirectory, name);
            return target.exists() && (target.canExecute() || target.isFile());
        }

        // Search PATH directories
        List<String> pathDirs = getSearchPaths();
        for (String dir : pathDirs) {
            File candidate = new File(dir, name);
            if (candidate.exists() && candidate.canExecute() && !candidate.isDirectory()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the detected Python executable name ("python3" or "python"), or null if none available.
     */
    public String findAvailablePython(String workingDirectory) {
        if (isExecutableAvailable("python3", workingDirectory)) return "python3";
        if (isExecutableAvailable("python", workingDirectory)) return "python";
        return null;
    }

    public List<String> getSearchPaths() {
        Set<String> paths = new LinkedHashSet<>();
        String envPath = System.getenv("PATH");
        if (envPath != null) {
            for (String p : envPath.split(":")) {
                if (!p.isBlank()) paths.add(p.trim());
            }
        }
        // Standard system directories
        paths.add("/opt/homebrew/bin");
        paths.add("/opt/homebrew/sbin");
        paths.add("/Library/Frameworks/Python.framework/Versions/Current/bin");
        paths.add("/Library/Frameworks/Python.framework/Versions/3.14/bin");
        paths.add("/Library/Frameworks/Python.framework/Versions/3.13/bin");
        paths.add("/Library/Frameworks/Python.framework/Versions/3.12/bin");
        paths.add("/usr/local/bin");
        paths.add("/usr/bin");
        paths.add("/bin");
        paths.add("/usr/sbin");
        paths.add("/sbin");
        return new ArrayList<>(paths);
    }
}
