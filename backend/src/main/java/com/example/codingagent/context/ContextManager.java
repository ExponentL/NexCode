package com.example.codingagent.context;

import com.example.codingagent.agent.ActionResult;
import com.example.codingagent.agent.AgentState;
import com.example.codingagent.agent.TestResultItem;
import com.example.codingagent.model.ChatMessage;
import com.example.codingagent.model.action.ActionType;
import com.example.codingagent.planner.StructuredPlan;
import com.example.codingagent.planner.StructuredPlanStep;
import com.example.codingagent.tools.FileTool;
import com.example.codingagent.tools.GitTool;
import com.example.codingagent.tools.RepositoryTool;
import com.example.codingagent.tools.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Token-optimized context manager that provides focused, relevant prompts
 * while preventing content duplication, repeated file dumps, and bloated tool outputs.
 */
@Component
public class ContextManager {

    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);
    private static final int MAX_CONTEXT_CHARS = 12_000; // Strict token budget ~3,000 tokens

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "in", "on", "at", "to", "for", "with",
            "is", "are", "was", "were", "of", "by", "from", "as", "it", "this",
            "that", "be", "do", "does", "did", "please", "can", "you", "fix",
            "add", "create", "make", "implement", "update", "modify", "bug", "issue"
    );

    private final GitTool gitTool;
    private final FileTool fileTool;
    private final RepositoryTool repositoryTool;

    // Caches to avoid redundant filesystem walking and file re-reading across turns
    private final Map<String, RepositoryContext> repoContextCache = new ConcurrentHashMap<>();
    private final Map<String, RelevantFiles> relevantFilesCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sentFilesPerTask = new ConcurrentHashMap<>();

    public ContextManager(GitTool gitTool, FileTool fileTool, RepositoryTool repositoryTool) {
        this.gitTool = gitTool;
        this.fileTool = fileTool;
        this.repositoryTool = repositoryTool;
    }

    /**
     * Inspects target repository, caching result to prevent repeated directory scans across turns.
     */
    public RepositoryContext inspectRepository(String repoPath) {
        Path root = Paths.get(repoPath).toAbsolutePath().normalize();
        String key = root.toString();

        RepositoryContext cached = repoContextCache.get(key);
        if (cached != null) {
            return cached;
        }

        File rootFile = root.toFile();
        if (!rootFile.exists() || !rootFile.isDirectory()) {
            throw new IllegalArgumentException("Target repository path does not exist or is not a directory: " + repoPath);
        }

        String repoName = root.getFileName().toString();
        String projectType = repositoryTool.detectProjectType(rootFile);
        String readme = readReadme(root);
        List<String> keyFiles = findSampleFiles(root);

        ensureGitInitialized(rootFile);

        String gitStatus = "Not a git repository";
        if (new File(rootFile, ".git").exists()) {
            ToolResult res = gitTool.execute(Map.of("action", "status"), repoPath);
            if (res.success() && res.output() != null) {
                gitStatus = res.output().trim();
            }
        }

        RepositoryContext context = new RepositoryContext(root.toString(), repoName, projectType, readme, keyFiles, gitStatus);
        repoContextCache.put(key, context);
        return context;
    }

    /**
     * Builds highly concise, token-efficient prompt messages for the foundation model.
     * Prevents re-sending files already inspected, avoids dumping the entire repo,
     * and truncates verbose logs.
     */
    public List<ChatMessage> buildPrioritizedContext(AgentState state, RepositoryContext repoContext, StructuredPlan plan) {
        StringBuilder system = new StringBuilder();
        system.append("You are an autonomous senior software engineering agent working in a codebase repository.\n");
        system.append("Project: ").append(repoContext.projectType()).append(" | ").append(repoContext.repositoryName()).append("\n\n");

        system.append("Action Format: Respond strictly with a JSON object:\n");
        system.append("{\"action\":\"READ_FILE\"|\"WRITE_FILE\"|\"CREATE_FILE\"|\"SEARCH\"|\"RUN_COMMAND\"|\"RUN_TESTS\"|\"FINISH\",");
        system.append("\"path\":\"relative/path\",\"content\":\"...\",\"command\":\"...\",\"reason\":\"...\"}\n\n");

        system.append("CRITICAL EXECUTION RULES:\n");
        system.append("1. REAL FILE CREATION & MODIFICATION: When files must be created or modified, you MUST emit a structured CREATE_FILE or WRITE_FILE action containing the actual file content.\n");
        system.append("   - To create a new file on the user's device, emit {\"action\": \"CREATE_FILE\", \"path\": \"relative/path/to/file.ext\", \"content\": \"...\"}.\n");
        system.append("   - Do NOT merely describe, explain, or suggest code.\n");
        system.append("   - The 'content' field must contain the FULL file source code to write to disk.\n");
        system.append("2. CONTINUATION TO TESTING: Immediately after creating or modifying files, execute RUN_TESTS or RUN_COMMAND to test your changes.\n");
        system.append("3. FAILURE RECOVERY: If tests or verification fail, inspect the real error and emit another CREATE_FILE or WRITE_FILE action with the necessary fix.\n");
        system.append("4. COMPLETION: Once all requested files are created or modified and verified by passing tests, emit {\"action\": \"FINISH\"}.\n");
        system.append("5. RESPONSE FORMAT: Always emit valid JSON conforming to the schema.");

        StringBuilder userContext = new StringBuilder();

        // 1. User task
        userContext.append("=== 1. USER TASK ===\n");
        userContext.append(state.getTask()).append("\n\n");

        // 2. Repository structure (compact summary)
        userContext.append("=== 2. REPOSITORY STRUCTURE ===\n");
        userContext.append("Project Type: ").append(repoContext.projectType()).append("\n");
        if (!repoContext.keyFiles().isEmpty()) {
            userContext.append("Key Files: ").append(String.join(", ", repoContext.keyFiles())).append("\n\n");
        } else {
            userContext.append("\n");
        }

        // 3. Relevant files - CACHED & DEDUPLICATED
        Set<String> alreadySent = sentFilesPerTask.computeIfAbsent(
                state.getTaskId(), k -> Collections.synchronizedSet(new HashSet<>()));

        Set<String> taskKeywords = extractKeywords(state.getTask());
        Path repoRoot = Paths.get(state.getRepositoryPath());
        RelevantFiles relevant = findRelevantFilesCached(repoRoot, taskKeywords);

        // Sources
        userContext.append("=== 3. RELEVANT SOURCE FILES ===\n");
        if (relevant.sources.isEmpty()) {
            userContext.append("(None isolated yet. Use SEARCH to locate source files).\n\n");
        } else {
            for (String srcFile : relevant.sources) {
                if (alreadySent.contains(srcFile)) {
                    userContext.append("- ").append(srcFile).append(" (already inspected)\n");
                } else {
                    userContext.append("--- File: ").append(srcFile).append(" ---\n");
                    userContext.append(sampleFileContent(repoRoot, srcFile, 35)).append("\n");
                    alreadySent.add(srcFile);
                }
                if (userContext.length() > MAX_CONTEXT_CHARS) break;
            }
            userContext.append("\n");
        }

        // Tests
        userContext.append("=== 4. RELEVANT TESTS ===\n");
        if (relevant.tests.isEmpty()) {
            userContext.append("(None isolated yet. Use SEARCH to locate test files).\n\n");
        } else {
            for (String testFile : relevant.tests) {
                if (alreadySent.contains(testFile)) {
                    userContext.append("- ").append(testFile).append(" (already inspected)\n");
                } else {
                    userContext.append("--- Test: ").append(testFile).append(" ---\n");
                    userContext.append(sampleFileContent(repoRoot, testFile, 30)).append("\n");
                    alreadySent.add(testFile);
                }
                if (userContext.length() > MAX_CONTEXT_CHARS) break;
            }
            userContext.append("\n");
        }

        // Configs - only include if not yet sent
        userContext.append("=== 5. PROJECT CONFIGURATION ===\n");
        for (String cfg : relevant.configs) {
            if (!alreadySent.contains(cfg)) {
                userContext.append("--- Config: ").append(cfg).append(" ---\n");
                userContext.append(sampleFileContent(repoRoot, cfg, 20)).append("\n");
                alreadySent.add(cfg);
            } else {
                userContext.append("- ").append(cfg).append(" (configured)\n");
            }
            if (userContext.length() > MAX_CONTEXT_CHARS) break;
        }
        userContext.append("\n");

        // 6. Implementation plan (compact summary)
        userContext.append("=== 6. IMPLEMENTATION PLAN ===\n");
        if (plan != null && !plan.getSteps().isEmpty()) {
            StructuredPlanStep active = plan.getCurrentStep();
            if (active != null) {
                userContext.append(String.format("Current Phase (%d/%d): %s\n\n",
                        active.getStepNumber(), plan.getSteps().size(), active.getTitle()));
            } else {
                userContext.append("Plan active: ").append(plan.getSteps().size()).append(" engineering phases.\n\n");
            }
        } else {
            userContext.append("Plan active.\n\n");
        }

        // 7. Previous tool results (concise, focused on outcomes)
        userContext.append("=== 7. PREVIOUS TOOL RESULTS ===\n");
        List<ActionResult> actionResults = state.getActionResults();
        if (actionResults != null && !actionResults.isEmpty()) {
            int startIdx = Math.max(0, actionResults.size() - 3);
            for (int i = startIdx; i < actionResults.size(); i++) {
                ActionResult ar = actionResults.get(i);
                userContext.append(String.format("[Action %d: %s %s -> %s]\n",
                        (i + 1),
                        ar.actionType(),
                        ar.target() != null ? ar.target() : "",
                        ar.success() ? "SUCCESS" : "FAILED"));

                if (ar.output() != null && !ar.output().isBlank()) {
                    String conciseOut;
                    if (ar.actionType() == ActionType.READ_FILE) {
                        conciseOut = ar.output().length() > 4000
                                ? ar.output().substring(0, 4000) + "\n...[truncated remainder of file]"
                                : ar.output();
                    } else {
                        conciseOut = extractConciseOutput(ar.output(), 800);
                    }
                    if (!conciseOut.isBlank()) {
                        userContext.append("Output:\n").append(conciseOut).append("\n");
                    }
                }
                if (ar.error() != null && !ar.error().isBlank()) {
                    String conciseErr = extractConciseOutput(ar.error(), 500);
                    if (!conciseErr.isBlank()) {
                        userContext.append("Error:\n").append(conciseErr).append("\n");
                    }
                }
                userContext.append("\n");
            }
        } else if (!state.getCommandsExecuted().isEmpty()) {
            List<String> commands = state.getCommandsExecuted();
            int cmdStart = Math.max(0, commands.size() - 3);
            for (int i = cmdStart; i < commands.size(); i++) {
                userContext.append("- ").append(commands.get(i)).append("\n");
            }
            userContext.append("\n");
        } else {
            userContext.append("No tools executed yet.\n\n");
        }

        // 8. Recent errors (compact)
        userContext.append("=== 8. RECENT ERRORS ===\n");
        if (state.getErrors().isEmpty()) {
            userContext.append("No active errors.\n\n");
        } else {
            List<String> errs = state.getErrors();
            int errStart = Math.max(0, errs.size() - 2);
            for (int i = errStart; i < errs.size(); i++) {
                String e = errs.get(i);
                if (e.length() > 200) e = e.substring(0, 200) + "...";
                userContext.append("! ").append(e).append("\n");
            }
            userContext.append("\n");
        }

        // 9. Previous failed attempts (compact)
        if (state.getAttempts() > 1 && !state.getTestResults().isEmpty()) {
            userContext.append("=== 9. PREVIOUS ATTEMPTS ===\n");
            userContext.append("Attempt ").append(state.getAttempts()).append(" of ").append(state.getMaxAttempts()).append(".\n");
            for (TestResultItem test : state.getTestResults()) {
                if (!test.passed()) {
                    String trace = test.errorTrace() != null && test.errorTrace().length() > 150
                            ? test.errorTrace().substring(0, 150) + "..." : test.errorTrace();
                    userContext.append("Failed Test: ").append(test.testName()).append(" -> ").append(trace).append("\n");
                }
            }
            userContext.append("\n");
        }

        // 10. Working tree Git diff (compact)
        if (state.getDiff() != null && !state.getDiff().isBlank()) {
            userContext.append("=== 10. WORKING TREE GIT DIFF ===\n");
            String diff = state.getDiff();
            if (diff.length() > 600) {
                diff = diff.substring(0, 600) + "\n...[truncated]";
            }
            userContext.append(diff).append("\n\n");
        }

        // Targeted Directive
        userContext.append("=== DIRECTIVE ===\n");
        if (state.getFilesModified().isEmpty()) {
            if (!state.getFilesInspected().isEmpty()) {
                userContext.append("Files inspected: ").append(String.join(", ", state.getFilesInspected())).append(".\n");
                userContext.append("CRITICAL: You have already inspected the codebase. Do NOT call READ_FILE or SEARCH again.\n");
                userContext.append("You MUST emit a WRITE_FILE action NOW to implement the required modifications on disk.\n");
                userContext.append("Action Format: {\"action\":\"WRITE_FILE\",\"path\":\"relative/path\",\"content\":\"full updated file source code\",\"reason\":\"...\"}\n\n");
            } else {
                userContext.append("Inspect target files using READ_FILE or immediately apply the necessary changes using WRITE_FILE with the complete updated content.\n\n");
            }
        } else {
            boolean testsPassedAfterModification = actionResults != null && actionResults.stream()
                    .anyMatch(ar -> ar.actionType() == ActionType.RUN_TESTS && ar.success());
            userContext.append("Modified files on disk: ").append(String.join(", ", state.getFilesModified())).append(".\n");
            if (testsPassedAfterModification) {
                userContext.append("Automated tests already passed! Emit {\"action\": \"FINISH\"} immediately to complete the task.\n\n");
            } else {
                userContext.append("Files modified on disk. Next step: Execute RUN_TESTS now to run the real test suite and verify changes.\n\n");
            }
        }

        userContext.append("What is your next concrete action? Provide a valid ModelAction JSON.");

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(system.toString()));
        messages.add(ChatMessage.user(userContext.toString()));

        return messages;
    }

    /**
     * Extracts concise, relevant output from tool results, preserving error traces and key metrics.
     */
    public String extractConciseOutput(String output, int maxChars) {
        if (output == null || output.isBlank()) return "";
        String trimmed = output.trim();
        if (trimmed.length() <= maxChars) return trimmed;

        // Filter out noisy progress lines (e.g. Maven download progress)
        String[] lines = trimmed.split("\n");
        StringBuilder relevant = new StringBuilder();
        for (String line : lines) {
            String l = line.toLowerCase();
            if (l.contains("downloading") || l.contains("progress") || l.startsWith("---") || l.startsWith("[info] downloading")) {
                continue;
            }
            if (l.contains("fail") || l.contains("error") || l.contains("assert") ||
                l.contains("traceback") || l.contains("passed") || l.contains("tests run") ||
                l.contains("ran ") || l.contains("exception") || l.contains("syntaxerror") ||
                l.contains("build success") || l.contains("build failure")) {
                relevant.append(line).append("\n");
                if (relevant.length() > maxChars) break;
            }
        }
        if (relevant.length() > 0) {
            return relevant.length() > maxChars ? relevant.substring(0, maxChars) + "..." : relevant.toString().trim();
        }

        // Fall back to tail
        return "..." + trimmed.substring(trimmed.length() - maxChars);
    }

    /**
     * Extracts meaningful search keywords from the user's task prompt.
     */
    public Set<String> extractKeywords(String taskText) {
        if (taskText == null) return Collections.emptySet();

        String cleaned = taskText.replaceAll("[^a-zA-Z0-9_\\-\\s]", " ").toLowerCase();
        String[] tokens = cleaned.split("\\s+");

        Set<String> keywords = new LinkedHashSet<>();
        for (String token : tokens) {
            String t = token.trim();
            if (t.length() >= 3 && !STOP_WORDS.contains(t)) {
                keywords.add(t);
            }
        }
        return keywords;
    }

    public RelevantFiles findRelevantFilesCached(Path root, Set<String> keywords) {
        String cacheKey = root.toString() + ":" + String.join(",", keywords);
        return relevantFilesCache.computeIfAbsent(cacheKey, k -> findRelevantFiles(root, keywords));
    }

    /**
     * Scans repository and ranks files by relevance to the extracted task keywords.
     */
    public RelevantFiles findRelevantFiles(Path root, Set<String> keywords) {
        List<String> sources = new ArrayList<>();
        List<String> tests = new ArrayList<>();
        List<String> configs = new ArrayList<>();

        if (!Files.exists(root)) {
            return new RelevantFiles(sources, tests, configs);
        }

        try (Stream<Path> stream = Files.walk(root, 6)) {
            List<Path> files = stream.filter(Files::isRegularFile)
                    .filter(p -> !isIgnored(p))
                    .toList();

            for (Path file : files) {
                String relPath = root.relativize(file).toString();
                String lower = relPath.toLowerCase();

                // Configuration files
                if (lower.endsWith("pom.xml") || lower.endsWith("package.json") || lower.endsWith("build.gradle") ||
                        lower.endsWith("application.yml") || lower.endsWith("application.properties")) {
                    if (configs.size() < 1) configs.add(relPath);
                    continue;
                }

                // Match against task keywords
                boolean isMatch = keywords.stream().anyMatch(lower::contains);

                if (isMatch) {
                    if (lower.contains("test") || lower.contains("spec")) {
                        if (tests.size() < 2) tests.add(relPath);
                    } else {
                        if (sources.size() < 2) sources.add(relPath);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed scanning relevant files: {}", e.getMessage());
        }

        return new RelevantFiles(sources, tests, configs);
    }

    private String sampleFileContent(Path root, String relPath, int maxLines) {
        try {
            Path file = root.resolve(relPath);
            if (!Files.exists(file)) return "(File not found)";

            List<String> lines = Files.readAllLines(file);
            int count = Math.min(lines.size(), maxLines);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                sb.append(i + 1).append(" | ").append(lines.get(i)).append("\n");
            }
            if (lines.size() > maxLines) {
                sb.append("... [").append(lines.size() - maxLines).append(" more lines omitted]\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "(Unable to read content: " + e.getMessage() + ")";
        }
    }

    private String readReadme(Path root) {
        for (String name : List.of("README.md", "readme.md", "README.txt", "README")) {
            Path file = root.resolve(name);
            if (Files.exists(file)) {
                try {
                    String content = Files.readString(file).trim();
                    return content.length() > 300 ? content.substring(0, 300) + "..." : content;
                } catch (IOException ignored) {
                }
            }
        }
        return "No README file found.";
    }

    private List<String> findSampleFiles(Path root) {
        List<String> sample = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root, 3)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> !isIgnored(p))
                    .limit(8)
                    .forEach(p -> sample.add(root.relativize(p).toString()));
        } catch (IOException ignored) {
        }
        return sample;
    }

    private boolean isIgnored(Path p) {
        String str = p.toString();
        return str.contains("/.git") ||
                str.contains("/target") ||
                str.contains("/node_modules") ||
                str.contains("/bin") ||
                str.contains("/obj") ||
                str.contains("/dist") ||
                str.contains("/__pycache__") ||
                str.contains("/.idea");
    }

    public void cleanupTask(String taskId) {
        if (taskId != null) {
            sentFilesPerTask.remove(taskId);
        }
    }

    private void ensureGitInitialized(File rootDir) {
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
            return;
        }
        if (new File(rootDir, ".git").exists()) {
            return;
        }
        try {
            log.info("Workspace at {} is not a git repository. Initializing baseline git repository...", rootDir);
            new ProcessBuilder("git", "init").directory(rootDir).start().waitFor(5, TimeUnit.SECONDS);
            new ProcessBuilder("git", "add", "-A").directory(rootDir).start().waitFor(5, TimeUnit.SECONDS);
            new ProcessBuilder("git", "commit", "-m", "initial baseline", "--allow-empty").directory(rootDir).start().waitFor(5, TimeUnit.SECONDS);
            log.info("Successfully initialized baseline git repository at {}", rootDir);
        } catch (Exception e) {
            log.warn("Could not initialize git baseline at {}: {}", rootDir, e.getMessage());
        }
    }

    public record RelevantFiles(List<String> sources, List<String> tests, List<String> configs) {}
}
