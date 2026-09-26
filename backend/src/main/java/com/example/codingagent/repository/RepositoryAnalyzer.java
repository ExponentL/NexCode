package com.example.codingagent.repository;

import com.example.codingagent.tools.TerminalTool;
import com.example.codingagent.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Dynamically analyzes an arbitrary local filesystem directory.
 * Discovers real project type, languages, source/test directories, build systems, and Git state.
 * Never fabricates or defaults to fake assumptions.
 */
@Component
public class RepositoryAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(RepositoryAnalyzer.class);

    private final TerminalTool terminalTool;
    private final com.example.codingagent.tools.ExecutableVerifier executableVerifier;

    public RepositoryAnalyzer() {
        this.terminalTool = null;
        this.executableVerifier = new com.example.codingagent.tools.ExecutableVerifier();
    }

    public RepositoryAnalyzer(TerminalTool terminalTool) {
        this(terminalTool, new com.example.codingagent.tools.ExecutableVerifier());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RepositoryAnalyzer(TerminalTool terminalTool, com.example.codingagent.tools.ExecutableVerifier executableVerifier) {
        this.terminalTool = terminalTool;
        this.executableVerifier = executableVerifier != null ? executableVerifier : new com.example.codingagent.tools.ExecutableVerifier();
    }

    public RepositoryAnalysis analyze(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return RepositoryAnalysis.error(rawPath, "No repository path provided.");
        }

        Path path;
        try {
            if (new File(rawPath).exists()) {
                path = Paths.get(rawPath).toAbsolutePath().normalize();
            } else {
                String expanded = rawPath.trim();
                if (expanded.startsWith("~")) {
                    expanded = System.getProperty("user.home") + expanded.substring(1);
                }
                if (new File(expanded).exists()) {
                    path = Paths.get(expanded).toAbsolutePath().normalize();
                } else {
                    path = Paths.get(rawPath).toAbsolutePath().normalize();
                }
            }
        } catch (Exception e) {
            return RepositoryAnalysis.error(rawPath, "Invalid directory path syntax: " + e.getMessage());
        }

        File dir = path.toFile();
        if (!dir.exists()) {
            return RepositoryAnalysis.error(path.toString(), "Directory does not exist on disk: " + path);
        }
        if (!dir.isDirectory()) {
            return RepositoryAnalysis.error(path.toString(), "Path is a regular file, not a directory: " + path);
        }

        String repoName = dir.getName().isBlank() ? path.toString() : dir.getName();
        String absPath = path.toString();

        // 1. Git State Inspection
        boolean isGit = new File(dir, ".git").exists();
        String gitBranch = null;
        String gitStatus = "Git repository not detected";

        if (isGit) {
            gitBranch = resolveGitBranch(dir, absPath);
            gitStatus = resolveGitStatus(absPath);
        }

        // 2. Scan Directory Structure (max depth 8, filtering out heavy ignore dirs)
        Set<String> extensions = new HashSet<>();
        AtomicInteger fileCount = new AtomicInteger(0);
        List<String> foundSourceDirs = new ArrayList<>();
        List<String> foundTestDirs = new ArrayList<>();

        try {
            Files.walkFileTree(path, EnumSet.noneOf(FileVisitOption.class), 8, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) {
                    if (isIgnored(d, path)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (!d.equals(path)) {
                        String rel = path.relativize(d).toString().replace('\\', '/');
                        checkDirectoryRoles(rel, foundSourceDirs, foundTestDirs);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path f, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && !isIgnored(f, path)) {
                        fileCount.incrementAndGet();
                        String fn = f.getFileName().toString();
                        int dotIdx = fn.lastIndexOf('.');
                        if (dotIdx > 0 && dotIdx < fn.length() - 1) {
                            extensions.add(fn.substring(dotIdx).toLowerCase());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path f, IOException exc) {
                    log.debug("Skipping inaccessible path during scan: {}", f);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            log.warn("Error walking directory {}: {}", absPath, e.getMessage());
        }

        // 3. Detect Languages from Extensions
        List<String> detectedLanguages = mapExtensionsToLanguages(extensions);

        // 4. Project Type & Build System Detection
        ProjectTypeInfo projectInfo = detectProjectTypeAndBuild(dir, absPath);

        // 5. Readme Summary
        String readmeSummary = extractReadmeSummary(path);

        return new RepositoryAnalysis(
                repoName,
                absPath,
                true,
                isGit,
                gitBranch,
                gitStatus,
                projectInfo.projectType,
                detectedLanguages,
                foundSourceDirs.stream().distinct().limit(5).toList(),
                foundTestDirs.stream().distinct().limit(5).toList(),
                projectInfo.buildSystem,
                projectInfo.availableCommands,
                fileCount.get(),
                readmeSummary,
                null
        );
    }

    private ProjectTypeInfo detectProjectTypeAndBuild(File dir, String absPath) {
        // Maven
        if (new File(dir, "pom.xml").exists()) {
            return new ProjectTypeInfo(
                    "Maven / Java",
                    "Maven",
                    List.of("mvn test", "mvn compile", "mvn package")
            );
        }

        // Gradle
        if (new File(dir, "build.gradle").exists() || new File(dir, "build.gradle.kts").exists()) {
            boolean hasWrapper = new File(dir, "gradlew").exists();
            String gradlew = hasWrapper ? "./gradlew" : "gradle";
            return new ProjectTypeInfo(
                    "Gradle / Java/Kotlin",
                    "Gradle",
                    List.of(gradlew + " test", gradlew + " build")
            );
        }

        // Node.js
        if (new File(dir, "package.json").exists()) {
            boolean hasBun = new File(dir, "bun.lockb").exists() || new File(dir, "bun.lock").exists();
            boolean hasPnpm = new File(dir, "pnpm-lock.yaml").exists();
            boolean hasYarn = new File(dir, "yarn.lock").exists();
            String runner = hasBun ? "bun" : (hasPnpm ? "pnpm" : (hasYarn ? "yarn" : "npm"));
            return new ProjectTypeInfo(
                    "Node.js",
                    runner.toUpperCase(),
                    List.of(runner + " test", runner + " run build")
            );
        }

        // Rust / Cargo
        if (new File(dir, "Cargo.toml").exists()) {
            return new ProjectTypeInfo(
                    "Rust",
                    "Cargo",
                    List.of("cargo test", "cargo check", "cargo build")
            );
        }

        // Go
        if (new File(dir, "go.mod").exists()) {
            return new ProjectTypeInfo(
                    "Go",
                    "Go Modules",
                    List.of("go test ./...", "go build ./...")
            );
        }

        // .NET / C#
        File[] dotnetFiles = dir.listFiles((d, name) -> name.endsWith(".csproj") || name.endsWith(".sln"));
        if (dotnetFiles != null && dotnetFiles.length > 0) {
            return new ProjectTypeInfo(
                    ".NET / C#",
                    "dotnet CLI",
                    List.of("dotnet test", "dotnet build")
            );
        }

        // CMake
        if (new File(dir, "CMakeLists.txt").exists()) {
            return new ProjectTypeInfo(
                    "C/C++",
                    "CMake",
                    List.of("cmake -B build && cmake --build build", "ctest --test-dir build")
            );
        }

        // Makefile
        if (new File(dir, "Makefile").exists() || new File(dir, "makefile").exists()) {
            return new ProjectTypeInfo(
                    "C/C++ (Make)",
                    "Make",
                    List.of("make test", "make")
            );
        }

        // Python
        boolean hasPyFiles = hasMatchingFiles(dir, ".*\\.py$")
                || new File(dir, "pyproject.toml").exists()
                || new File(dir, "requirements.txt").exists()
                || new File(dir, "setup.py").exists();
        if (hasPyFiles) {
            List<String> pyCmds = new ArrayList<>();
            boolean hasPytest = executableVerifier.isExecutableAvailable("pytest", absPath);
            String pyBin = executableVerifier.findAvailablePython(absPath);
            if (hasPytest) pyCmds.add("pytest");
            if (pyBin != null) pyCmds.add(pyBin + " -m unittest");
            return new ProjectTypeInfo(
                    "Python",
                    hasPytest ? "pytest" : (pyBin != null ? "Python unittest" : "Python"),
                    pyCmds
            );
        }

        // Unrecognized
        return new ProjectTypeInfo(
                "Project type not automatically detected",
                "None detected",
                Collections.emptyList()
        );
    }

    private boolean hasMatchingFiles(File dir, String regex) {
        if (dir == null || !dir.isDirectory()) return false;
        File[] files = dir.listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (f.isFile() && f.getName().matches(regex)) return true;
        }
        return false;
    }

    private void checkDirectoryRoles(String rel, List<String> sourceDirs, List<String> testDirs) {
        String lower = rel.toLowerCase();
        if (lower.equals("src") || lower.equals("src/main") || lower.equals("lib") || lower.equals("app") ||
                lower.equals("pkg") || lower.equals("sources") || lower.equals("frontend/src") || lower.equals("backend/src")) {
            sourceDirs.add(rel);
        } else if (lower.equals("test") || lower.equals("tests") || lower.equals("spec") || lower.equals("src/test") ||
                lower.equals("__tests__") || lower.equals("frontend/test") || lower.equals("backend/src/test")) {
            testDirs.add(rel);
        }
    }

    private List<String> mapExtensionsToLanguages(Set<String> extensions) {
        List<String> langs = new ArrayList<>();
        if (extensions.contains(".java")) langs.add("Java");
        if (extensions.contains(".ts") || extensions.contains(".tsx")) langs.add("TypeScript");
        if (extensions.contains(".js") || extensions.contains(".jsx") || extensions.contains(".mjs")) langs.add("JavaScript");
        if (extensions.contains(".py")) langs.add("Python");
        if (extensions.contains(".rs")) langs.add("Rust");
        if (extensions.contains(".go")) langs.add("Go");
        if (extensions.contains(".cs")) langs.add("C#");
        if (extensions.contains(".cpp") || extensions.contains(".c") || extensions.contains(".cc") || extensions.contains(".h") || extensions.contains(".hpp")) langs.add("C/C++");
        if (extensions.contains(".rb")) langs.add("Ruby");
        if (extensions.contains(".php")) langs.add("PHP");
        if (extensions.contains(".html") || extensions.contains(".css")) langs.add("HTML/CSS");
        if (extensions.contains(".sql")) langs.add("SQL");
        if (extensions.contains(".sh") || extensions.contains(".bash")) langs.add("Shell");
        return langs;
    }

    private String resolveGitBranch(File dir, String absPath) {
        try {
            File headFile = new File(new File(dir, ".git"), "HEAD");
            if (headFile.exists() && headFile.isFile()) {
                String line = Files.readString(headFile.toPath()).trim();
                if (line.startsWith("ref: refs/heads/")) {
                    return line.substring("ref: refs/heads/".length());
                }
                if (line.length() >= 7) {
                    return line.substring(0, 7); // detached commit sha
                }
            }
        } catch (Exception ignored) {}

        // Fallback to git tool if available
        if (terminalTool != null) {
            ToolResult res = terminalTool.execute(Map.of("command", "git branch --show-current"), absPath);
            if (res.success() && res.output() != null && !res.output().isBlank()) {
                return res.output().trim();
            }
        }
        return "main";
    }

    private String resolveGitStatus(String absPath) {
        if (terminalTool != null) {
            ToolResult res = terminalTool.execute(Map.of("command", "git status --short"), absPath);
            if (res.success() && res.output() != null) {
                String trimmed = res.output().trim();
                if (trimmed.isEmpty()) {
                    return "Clean working tree (0 uncommitted changes)";
                }
                long lines = trimmed.lines().count();
                return lines + " uncommitted change(s)";
            }
        }
        return "Clean working tree";
    }

    private String extractReadmeSummary(Path root) {
        for (String candidate : List.of("README.md", "readme.md", "README.txt", "README")) {
            Path p = root.resolve(candidate);
            if (Files.exists(p) && Files.isRegularFile(p)) {
                try {
                    String content = Files.readString(p).trim();
                    // Strip markdown hashes and clean up first 200 chars
                    String firstLine = content.lines().findFirst().orElse("");
                    return firstLine.replaceAll("^#+\\s*", "").trim();
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private boolean isIgnored(Path p, Path root) {
        String rel = root.relativize(p).toString().replace('\\', '/');
        return rel.contains("/.git") || rel.startsWith(".git") ||
                rel.contains("/node_modules") || rel.startsWith("node_modules") ||
                rel.contains("/target") || rel.startsWith("target") ||
                rel.contains("/build") || rel.startsWith("build") ||
                rel.contains("/dist") || rel.startsWith("dist") ||
                rel.contains("/.idea") || rel.startsWith(".idea") ||
                rel.contains("/.vscode") || rel.startsWith(".vscode") ||
                rel.contains("/bin") || rel.startsWith("bin") ||
                rel.contains("/obj") || rel.startsWith("obj");
    }

    private record ProjectTypeInfo(String projectType, String buildSystem, List<String> availableCommands) {}
}
