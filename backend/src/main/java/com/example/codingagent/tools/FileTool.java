package com.example.codingagent.tools;

import com.example.codingagent.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

@Component
public class FileTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(FileTool.class);

    private final SecurityGuard securityGuard;

    public FileTool(SecurityGuard securityGuard) {
        this.securityGuard = securityGuard;
    }

    @Override
    public String getName() {
        return "file_tool";
    }

    @Override
    public String getDescription() {
        return "Perform Java NIO file operations in repository. Actions: READ_FILE, WRITE_FILE, CREATE_FILE, DELETE_FILE.";
    }

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
                getName(),
                getDescription(),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "action", Map.of("type", "string", "enum", List.of("READ_FILE", "WRITE_FILE", "CREATE_FILE", "DELETE_FILE")),
                                "path", Map.of("type", "string", "description", "Relative path to target file"),
                                "content", Map.of("type", "string", "description", "Content to write or create"),
                                "startLine", Map.of("type", "integer", "description", "Optional 1-indexed start line for READ_FILE"),
                                "endLine", Map.of("type", "integer", "description", "Optional 1-indexed end line for READ_FILE"),
                                "allowDelete", Map.of("type", "boolean", "description", "Explicit confirmation required for DELETE_FILE")
                        ),
                        "required", List.of("action", "path")
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, String workingDirectory) {
        String relativePath = extractPath(arguments);
        if (relativePath == null || relativePath.isBlank()) {
            return ToolResult.failure("Argument 'path' is mandatory for file operations.");
        }

        String rawAction = arguments.get("action") != null ? String.valueOf(arguments.get("action")).trim() : "WRITE_FILE";
        String content = extractContent(arguments);

        Path target;
        try {
            target = securityGuard.validateAndResolvePath(workingDirectory, relativePath);
        } catch (SecurityException se) {
            return ToolResult.failure("Security violation: " + se.getMessage());
        }

        try {
            return switch (rawAction.toUpperCase()) {
                case "READ_FILE", "READ", "VIEW" -> readFile(target, arguments);
                case "WRITE_FILE", "WRITE", "EDIT", "MODIFY", "UPDATE", "PATCH" -> writeFile(target, content);
                case "CREATE_FILE", "CREATE", "NEW" -> createFile(target, content);
                case "DELETE_FILE", "DELETE" -> deleteFile(target, arguments.get("allowDelete"));
                default -> ToolResult.failure("Unsupported file action: " + rawAction + ". Supported: READ_FILE, WRITE_FILE, CREATE_FILE, DELETE_FILE.");
            };
        } catch (Exception e) {
            log.error("FileTool error on action {}: {}", rawAction, e.getMessage());
            return ToolResult.failure("File operation error: " + e.getMessage());
        }
    }

    private String extractPath(Map<String, Object> arguments) {
        if (arguments.get("path") != null) return String.valueOf(arguments.get("path")).trim();
        if (arguments.get("file") != null) return String.valueOf(arguments.get("file")).trim();
        if (arguments.get("filepath") != null) return String.valueOf(arguments.get("filepath")).trim();
        if (arguments.get("filename") != null) return String.valueOf(arguments.get("filename")).trim();
        if (arguments.get("target") != null) return String.valueOf(arguments.get("target")).trim();
        if (arguments.get("targetFile") != null) return String.valueOf(arguments.get("targetFile")).trim();
        return null;
    }

    private String extractContent(Map<String, Object> arguments) {
        if (arguments.get("content") != null) return String.valueOf(arguments.get("content"));
        if (arguments.get("code") != null) return String.valueOf(arguments.get("code"));
        if (arguments.get("newContent") != null) return String.valueOf(arguments.get("newContent"));
        if (arguments.get("text") != null) return String.valueOf(arguments.get("text"));
        if (arguments.get("body") != null) return String.valueOf(arguments.get("body"));
        return null;
    }

    private ToolResult readFile(Path path, Map<String, Object> arguments) throws IOException {
        if (!Files.exists(path)) {
            return ToolResult.failure("File not found: " + path.getFileName());
        }
        if (Files.isDirectory(path)) {
            return ToolResult.failure("Target path is a directory: " + path.getFileName());
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        int startLine = arguments.get("startLine") instanceof Number n ? n.intValue() : 1;
        int endLine = arguments.get("endLine") instanceof Number n ? n.intValue() : lines.size();

        startLine = Math.max(1, startLine);
        endLine = Math.min(lines.size(), endLine);

        if (startLine > lines.size()) {
            return ToolResult.success("");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = startLine - 1; i < endLine; i++) {
            sb.append(i + 1).append(" | ").append(lines.get(i)).append("\n");
        }

        return ToolResult.success(sb.toString(), Map.of(
                "totalLines", lines.size(),
                "file", path.toString(),
                "readLines", (endLine - startLine + 1)
        ));
    }

    private ToolResult writeFile(Path path, String content) throws IOException {
        if (content == null) {
            return ToolResult.failure("Write content cannot be null.");
        }
        if (path.getParent() != null && !Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        // Write new content to disk using Java NIO
        Files.writeString(path, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        // Verify the file exists and contains the new content
        if (!Files.exists(path)) {
            return ToolResult.failure("Verification failed: file does not exist on disk after write: " + path);
        }
        String written = Files.readString(path, StandardCharsets.UTF_8);
        if (!written.equals(content)) {
            return ToolResult.failure("Verification failed: written content verification mismatch on disk for " + path);
        }

        log.info("Successfully modified file on disk: {} ({} bytes)", path, content.length());
        return ToolResult.success("File written successfully: " + path.getFileName(), Map.of(
                "bytesWritten", content.length(),
                "file", path.toString()
        ));
    }

    private ToolResult createFile(Path path, String content) throws IOException {
        // Existing files must be allowed to be modified
        if (Files.exists(path)) {
            return writeFile(path, content);
        }
        if (path.getParent() != null && !Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        String initialContent = content != null ? content : "";
        Files.writeString(path, initialContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);

        // Verify the file exists and contains the new content
        if (!Files.exists(path)) {
            return ToolResult.failure("Verification failed: file does not exist on disk after creation: " + path);
        }
        String written = Files.readString(path, StandardCharsets.UTF_8);
        if (!written.equals(initialContent)) {
            return ToolResult.failure("Verification failed: created content verification mismatch on disk for " + path);
        }

        log.info("Successfully created file on disk: {} ({} bytes)", path, initialContent.length());
        return ToolResult.success("File created successfully: " + path.getFileName(), Map.of(
                "bytesWritten", initialContent.length(),
                "file", path.toString()
        ));
    }

    private ToolResult deleteFile(Path path, Object allowDelete) throws IOException {
        boolean confirmed = Boolean.TRUE.equals(allowDelete) || "true".equalsIgnoreCase(String.valueOf(allowDelete));
        if (!confirmed) {
            return ToolResult.failure("Deletion rejected: 'allowDelete: true' must be explicitly set to delete files.");
        }
        if (!Files.exists(path)) {
            return ToolResult.failure("File not found for deletion: " + path.getFileName());
        }
        if (Files.isDirectory(path)) {
            return ToolResult.failure("Deleting directories is not permitted.");
        }

        Files.delete(path);
        return ToolResult.success("File deleted successfully: " + path.getFileName(), Map.of("file", path.toString()));
    }
}

