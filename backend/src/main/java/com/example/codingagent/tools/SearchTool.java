package com.example.codingagent.tools;

import com.example.codingagent.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class SearchTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(SearchTool.class);
    private static final int MAX_RESULTS = 50;

    private final SecurityGuard securityGuard;

    public SearchTool(SecurityGuard securityGuard) {
        this.securityGuard = securityGuard;
    }

    @Override
    public String getName() {
        return "search_tool";
    }

    @Override
    public String getDescription() {
        return "Search filenames and text content in repository. Actions: SEARCH_FILENAMES, SEARCH_CONTENT.";
    }

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
                getName(),
                getDescription(),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "action", Map.of("type", "string", "enum", List.of("SEARCH_FILENAMES", "SEARCH_CONTENT")),
                                "query", Map.of("type", "string", "description", "Filename pattern or text query"),
                                "pathPrefix", Map.of("type", "string", "description", "Optional subfolder to constrain search")
                        ),
                        "required", List.of("action", "query")
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, String workingDirectory) {
        String action = (String) arguments.get("action");
        String query = (String) arguments.get("query");
        String pathPrefix = (String) arguments.get("pathPrefix");

        if (action == null || query == null || query.isBlank()) {
            return ToolResult.failure("Both 'action' and 'query' arguments are required.");
        }

        Path searchRoot;
        try {
            if (pathPrefix != null && !pathPrefix.isBlank()) {
                searchRoot = securityGuard.validateAndResolvePath(workingDirectory, pathPrefix);
            } else {
                searchRoot = Paths.get(workingDirectory).toRealPath().normalize();
            }
        } catch (Exception e) {
            searchRoot = Paths.get(workingDirectory).toAbsolutePath().normalize();
        }

        try {
            return switch (action.toUpperCase()) {
                case "SEARCH_FILENAMES" -> searchFilenames(searchRoot, query);
                case "SEARCH_CONTENT" -> searchContent(searchRoot, query);
                default -> ToolResult.failure("Unsupported search action: " + action + ". Supported: SEARCH_FILENAMES, SEARCH_CONTENT.");
            };
        } catch (Exception e) {
            log.error("SearchTool failed: {}", e.getMessage());
            return ToolResult.failure("Search execution error: " + e.getMessage());
        }
    }

    private ToolResult searchFilenames(Path root, String query) throws IOException {
        List<String> matched = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        try (Stream<Path> stream = Files.walk(root, 10)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> !isIgnored(p))
                    .filter(p -> p.getFileName().toString().toLowerCase().contains(lowerQuery))
                    .limit(MAX_RESULTS)
                    .forEach(p -> matched.add(root.relativize(p).toString()));
        }

        if (matched.isEmpty()) {
            return ToolResult.success("No files matching pattern: " + query);
        }

        return ToolResult.success(String.join("\n", matched), Map.of("matchesCount", matched.size()));
    }

    private ToolResult searchContent(Path root, String query) throws IOException {
        List<String> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        try (Stream<Path> stream = Files.walk(root, 10)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !isIgnored(p))
                    .toList();

            for (Path file : files) {
                try {
                    List<String> lines = Files.readAllLines(file);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        if (line.toLowerCase().contains(lowerQuery)) {
                            Path rel = root.relativize(file);
                            results.add(rel + ":" + (i + 1) + ": " + line.trim());
                            if (results.size() >= MAX_RESULTS) break;
                        }
                    }
                } catch (Exception ignored) {
                    // Ignore binary files or files not encoded in UTF-8
                }
                if (results.size() >= MAX_RESULTS) break;
            }
        }

        if (results.isEmpty()) {
            return ToolResult.success("No occurrences found for: " + query);
        }

        return ToolResult.success(String.join("\n", results), Map.of("occurrencesCount", results.size()));
    }

    private boolean isIgnored(Path p) {
        String str = p.toString();
        return str.contains("/.git/") ||
                str.contains("/target/") ||
                str.contains("/node_modules/") ||
                str.contains("/bin/") ||
                str.contains("/obj/") ||
                str.contains("/.idea/") ||
                str.contains("/dist/") ||
                str.contains("/build/") ||
                str.endsWith(".class") ||
                str.endsWith(".jar") ||
                str.endsWith(".png") ||
                str.endsWith(".jpg");
    }
}
