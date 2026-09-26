package com.example.codingagent.model.action;

import com.example.codingagent.model.ToolCall;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ModelActionParser {

    private static final Logger log = LoggerFactory.getLogger(ModelActionParser.class);
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public ModelActionParser() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Parses a ModelAction from raw LLM text output.
     */
    public ModelAction parseFromText(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            return null;
        }

        String jsonCandidate = extractJsonString(responseText);
        if (jsonCandidate == null) {
            log.debug("No JSON block found in response text");
            return null;
        }

        try {
            return objectMapper.readValue(jsonCandidate, ModelAction.class);
        } catch (Exception e) {
            log.warn("Failed to parse JSON string to ModelAction: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Translates a ToolCall into a ModelAction.
     */
    public ModelAction parseFromToolCall(ToolCall toolCall) {
        if (toolCall == null) return null;

        String name = toolCall.name();
        String argsJson = toolCall.argumentsJson();

        try {
            Map<String, Object> args = objectMapper.readValue(
                    argsJson != null && !argsJson.isBlank() ? argsJson : "{}",
                    new TypeReference<Map<String, Object>>() {}
            );

            return switch (name.toLowerCase()) {
                case "file_tool", "filesystem" -> {
                    String subAction = String.valueOf(args.getOrDefault("action", "READ_FILE")).toUpperCase();
                    String path = (String) args.get("path");
                    String content = (String) args.get("content");
                    String reason = (String) args.getOrDefault("reason", "File operation");
                    Integer startLine = args.get("startLine") instanceof Number n ? n.intValue() : null;
                    Integer endLine = args.get("endLine") instanceof Number n ? n.intValue() : null;

                    ModelAction action = new ModelAction();
                    action.setAction(ActionType.valueOf(subAction));
                    action.setPath(path);
                    action.setContent(content);
                    action.setReason(reason);
                    action.setStartLine(startLine);
                    action.setEndLine(endLine);
                    yield action;
                }
                case "search_tool", "search" -> {
                    String query = (String) args.get("query");
                    String reason = (String) args.getOrDefault("reason", "Search codebase");
                    yield ModelAction.search(query, reason);
                }
                case "terminal_tool", "terminal" -> {
                    String command = (String) args.get("command");
                    String reason = (String) args.getOrDefault("reason", "Execute shell command");
                    yield ModelAction.runCommand(command, reason);
                }
                case "test_tool", "test_runner" -> {
                    String command = (String) args.get("command");
                    String reason = (String) args.getOrDefault("reason", "Execute automated tests");
                    yield ModelAction.runTests(command, reason);
                }
                case "git_tool", "git" -> {
                    String subAction = (String) args.getOrDefault("action", "diff");
                    if ("status".equalsIgnoreCase(subAction)) {
                        yield ModelAction.gitStatus("Check git status");
                    } else {
                        yield ModelAction.gitDiff("Inspect git diff");
                    }
                }
                default -> null;
            };
        } catch (Exception e) {
            log.error("Failed to translate tool call {} to ModelAction: {}", name, e.getMessage());
            return null;
        }
    }

    private String extractJsonString(String text) {
        String trimmed = text.trim();
        // Direct JSON object
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        // Fenced markdown ```json ... ```
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Embedded JSON object between first { and last }
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace > firstBrace) {
            String candidate = text.substring(firstBrace, lastBrace + 1);
            if (candidate.contains("\"action\"")) {
                return candidate;
            }
        }

        return null;
    }
}
