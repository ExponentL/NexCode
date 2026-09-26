package com.example.codingagent.tools;

import com.example.codingagent.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class RepositoryTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(RepositoryTool.class);

    private final SecurityGuard securityGuard;

    public RepositoryTool(SecurityGuard securityGuard) {
        this.securityGuard = securityGuard;
    }

    @Override
    public String getName() {
        return "repository_tool";
    }

    @Override
    public String getDescription() {
        return "Inspect repository directory tree, discover build configurations, and detect project technology stack.";
    }

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
                getName(),
                getDescription(),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "maxDepth", Map.of("type", "integer", "description", "Maximum tree depth to inspect (default 3)")
                        )
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, String workingDirectory) {
        Path root;
        try {
            root = Paths.get(workingDirectory).toRealPath().normalize();
        } catch (Exception e) {
            root = Paths.get(workingDirectory).toAbsolutePath().normalize();
        }

        File rootDir = root.toFile();
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            return ToolResult.failure("Repository directory invalid or inaccessible: " + workingDirectory);
        }

        int maxDepth = arguments.get("maxDepth") instanceof Number n ? n.intValue() : 3;
        maxDepth = Math.max(1, Math.min(maxDepth, 6));

        String projectType = detectProjectType(rootDir);
        List<String> projectFiles = identifyProjectFiles(rootDir);
        List<String> tree = buildDirectoryTree(root, maxDepth);

        StringBuilder summary = new StringBuilder();
        summary.append("Repository Inspection Report:\n");
        summary.append("- Root Path: ").append(root).append("\n");
        summary.append("- Detected Framework: ").append(projectType).append("\n");
        summary.append("- Key Project Descriptors: ").append(String.join(", ", projectFiles)).append("\n\n");
        summary.append("Directory Structure (depth ").append(maxDepth).append("):\n");
        for (String line : tree) {
            summary.append(line).append("\n");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectType", projectType);
        metadata.put("projectFiles", projectFiles);
        metadata.put("totalFilesSampled", tree.size());

        return ToolResult.success(summary.toString(), metadata);
    }

    public String detectProjectType(File root) {
        if (new File(root, "pom.xml").exists()) return "Java/Maven";
        if (new File(root, "build.gradle").exists() || new File(root, "build.gradle.kts").exists()) return "Java/Gradle";
        if (new File(root, "package.json").exists()) return "Node.js (npm/yarn/pnpm)";
        if (hasExtension(root, ".csproj") || hasExtension(root, ".sln")) return ".NET (C#)";
        if (new File(root, "CMakeLists.txt").exists()) return "C/C++ (CMake)";
        if (new File(root, "Makefile").exists()) return "C/C++ (Makefile)";
        if (new File(root, "Cargo.toml").exists()) return "Rust (Cargo)";
        if (new File(root, "go.mod").exists()) return "Go";
        return "Project type not automatically detected";
    }

    private List<String> identifyProjectFiles(File root) {
        List<String> descriptors = new ArrayList<>();
        String[] candidates = {
                "pom.xml", "build.gradle", "build.gradle.kts", "settings.gradle",
                "package.json", "tsconfig.json", "Cargo.toml", "go.mod",
                "CMakeLists.txt", "Makefile", "Dockerfile", ".gitignore", "README.md"
        };
        for (String c : candidates) {
            if (new File(root, c).exists()) {
                descriptors.add(c);
            }
        }
        return descriptors;
    }

    private List<String> buildDirectoryTree(Path root, int maxDepth) {
        List<String> lines = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root, maxDepth)) {
            stream.filter(p -> !isIgnored(p))
                    .limit(60)
                    .forEach(p -> {
                        int depth = root.relativize(p).getNameCount();
                        String indent = "  ".repeat(Math.max(0, depth - 1));
                        String prefix = Files.isDirectory(p) ? "📁 " : "📄 ";
                        lines.add(indent + prefix + p.getFileName().toString());
                    });
        } catch (IOException e) {
            log.warn("Error walking directory tree: {}", e.getMessage());
        }
        return lines;
    }

    private boolean hasExtension(File dir, String ext) {
        File[] files = dir.listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (f.getName().endsWith(ext)) return true;
        }
        return false;
    }

    private boolean isIgnored(Path p) {
        String str = p.toString();
        return str.contains("/.git") ||
                str.contains("/target") ||
                str.contains("/node_modules") ||
                str.contains("/bin") ||
                str.contains("/obj") ||
                str.contains("/dist") ||
                str.contains("/.idea");
    }
}
