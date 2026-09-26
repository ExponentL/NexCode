package com.example.codingagent.tools;

import com.example.codingagent.model.ToolCall;
import com.example.codingagent.model.ToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolManagerTest {

    private ToolManager toolManager;

    @BeforeEach
    void setUp() {
        SecurityGuard guard = new SecurityGuard();
        FileTool fileTool = new FileTool(guard);
        SearchTool searchTool = new SearchTool(guard);
        RepositoryTool repositoryTool = new RepositoryTool(guard);
        TerminalTool terminalTool = new TerminalTool(guard);
        GitTool gitTool = new GitTool(terminalTool);
        TestTool testTool = new TestTool(terminalTool, repositoryTool);

        toolManager = new ToolManager(List.of(
                fileTool, searchTool, repositoryTool, terminalTool, gitTool, testTool
        ));
    }

    @Test
    void testRegisteredToolsAndAliases() {
        assertNotNull(toolManager.getTool("file_tool"));
        assertNotNull(toolManager.getTool("filesystem")); // alias

        assertNotNull(toolManager.getTool("search_tool"));
        assertNotNull(toolManager.getTool("search")); // alias

        assertNotNull(toolManager.getTool("repository_tool"));
        assertNotNull(toolManager.getTool("repository")); // alias

        assertNotNull(toolManager.getTool("terminal_tool"));
        assertNotNull(toolManager.getTool("terminal")); // alias

        assertNotNull(toolManager.getTool("git_tool"));
        assertNotNull(toolManager.getTool("git")); // alias

        assertNotNull(toolManager.getTool("test_tool"));
        assertNotNull(toolManager.getTool("test_runner")); // alias
    }

    @Test
    void testGetToolDefinitionsDeduplicated() {
        List<ToolDefinition> definitions = toolManager.getToolDefinitions();
        assertEquals(6, definitions.size());
    }

    @Test
    void testExecuteUnknownTool(@TempDir Path tempDir) {
        ToolCall unknown = new ToolCall("c-1", "unknown_magic_tool", "{}");
        ToolResult result = toolManager.executeTool(unknown, tempDir.toString());

        assertFalse(result.success());
        assertTrue(result.error().contains("Tool not found"));
    }
}
