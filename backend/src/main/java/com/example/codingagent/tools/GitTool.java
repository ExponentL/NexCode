package com.example.codingagent.tools;

import com.example.codingagent.model.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GitTool implements AgentTool {

    private final TerminalTool terminalTool;

    public GitTool(TerminalTool terminalTool) {
        this.terminalTool = terminalTool;
    }

    @Override
    public String getName() {
        return "git_tool";
    }

    @Override
    public String getDescription() {
        return "Inspect repository git status, commit history, current branch, and inspect diffs. Actions: diff, status, log, branch.";
    }

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
                getName(),
                getDescription(),
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "action", Map.of("type", "string", "enum", List.of("diff", "status", "log", "branch")),
                                "extraArgs", Map.of("type", "string", "description", "Optional extra flags for git command")
                        ),
                        "required", List.of("action")
                )
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, String workingDirectory) {
        String action = (String) arguments.get("action");
        String extraArgs = arguments.get("extraArgs") != null ? (String) arguments.get("extraArgs") : "";

        if (action == null) {
            return ToolResult.failure("Action argument is required for git tool.");
        }

        String command = switch (action.toLowerCase()) {
            case "diff" -> "git diff " + extraArgs;
            case "status" -> "git status --short " + extraArgs;
            case "log" -> "git log -n 10 --oneline " + extraArgs;
            case "branch" -> "git branch --show-current";
            default -> null;
        };

        if (command == null) {
            return ToolResult.failure("Unknown git action: " + action + ". Supported: diff, status, log, branch.");
        }

        return terminalTool.execute(Map.of("command", command), workingDirectory);
    }
}
