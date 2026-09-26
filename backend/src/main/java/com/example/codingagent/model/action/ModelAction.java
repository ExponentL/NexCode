package com.example.codingagent.model.action;

import com.example.codingagent.tools.SecurityGuard;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelAction {

    private ActionType action;
    private String path;
    private String content;
    private String query;
    private String command;
    private String reason;
    private Integer startLine;
    private Integer endLine;
    private Boolean allowDelete;

    public ModelAction() {}

    public ModelAction(ActionType action, String path, String reason) {
        this.action = action;
        this.path = path;
        this.reason = reason;
    }

    public static ModelAction readFile(String path, String reason) {
        return new ModelAction(ActionType.READ_FILE, path, reason);
    }

    public static ModelAction search(String query, String reason) {
        ModelAction action = new ModelAction(ActionType.SEARCH, null, reason);
        action.setQuery(query);
        return action;
    }

    public static ModelAction writeFile(String path, String content, String reason) {
        ModelAction action = new ModelAction(ActionType.WRITE_FILE, path, reason);
        action.setContent(content);
        return action;
    }

    public static ModelAction createFile(String path, String content, String reason) {
        ModelAction action = new ModelAction(ActionType.CREATE_FILE, path, reason);
        action.setContent(content);
        return action;
    }

    public static ModelAction runCommand(String command, String reason) {
        ModelAction action = new ModelAction(ActionType.RUN_COMMAND, null, reason);
        action.setCommand(command);
        return action;
    }

    public static ModelAction runTests(String command, String reason) {
        ModelAction action = new ModelAction(ActionType.RUN_TESTS, null, reason);
        action.setCommand(command);
        return action;
    }

    public static ModelAction gitStatus(String reason) {
        return new ModelAction(ActionType.GIT_STATUS, null, reason);
    }

    public static ModelAction gitDiff(String reason) {
        return new ModelAction(ActionType.GIT_DIFF, null, reason);
    }

    public static ModelAction finish(String reason) {
        return new ModelAction(ActionType.FINISH, null, reason);
    }

    /**
     * Validates the action before execution.
     * Ensures paths do not escape the workspace and commands do not violate security constraints.
     */
    public ActionValidationResult validate(String workingDirectory, SecurityGuard guard) {
        if (action == null) {
            return ActionValidationResult.invalid("Missing 'action' parameter.");
        }

        switch (action) {
            case READ_FILE:
            case WRITE_FILE:
            case CREATE_FILE:
                if (path == null || path.isBlank()) {
                    return ActionValidationResult.invalid("Action " + action + " requires a non-empty 'path' parameter.");
                }
                try {
                    Path resolved = guard.validateAndResolvePath(workingDirectory, path);
                    return ActionValidationResult.ok(resolved.toString());
                } catch (SecurityException se) {
                    return ActionValidationResult.invalid("Path traversal rejected: " + se.getMessage());
                } catch (Exception e) {
                    return ActionValidationResult.invalid("Invalid path specification: " + e.getMessage());
                }

            case SEARCH:
                if (query == null || query.isBlank()) {
                    return ActionValidationResult.invalid("Action SEARCH requires a non-empty 'query' parameter.");
                }
                return ActionValidationResult.ok();

            case RUN_COMMAND:
                if (command == null || command.isBlank()) {
                    return ActionValidationResult.invalid("Action RUN_COMMAND requires a non-empty 'command' parameter.");
                }
                try {
                    guard.validateCommand(command);
                    return ActionValidationResult.ok();
                } catch (SecurityException se) {
                    return ActionValidationResult.invalid("Dangerous command rejected: " + se.getMessage());
                }

            case RUN_TESTS:
                if (command != null && !command.isBlank()) {
                    try {
                        guard.validateCommand(command);
                    } catch (SecurityException se) {
                        return ActionValidationResult.invalid("Test command rejected: " + se.getMessage());
                    }
                }
                return ActionValidationResult.ok();

            case GIT_STATUS:
            case GIT_DIFF:
            case FINISH:
                return ActionValidationResult.ok();

            default:
                return ActionValidationResult.invalid("Unsupported action type: " + action);
        }
    }

    // Getters and setters
    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public Boolean getAllowDelete() {
        return allowDelete;
    }

    public void setAllowDelete(Boolean allowDelete) {
        this.allowDelete = allowDelete;
    }
}
