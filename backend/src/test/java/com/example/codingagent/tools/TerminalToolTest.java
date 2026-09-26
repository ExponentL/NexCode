package com.example.codingagent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TerminalToolTest {

    private TerminalTool terminalTool;

    @BeforeEach
    void setUp() {
        SecurityGuard guard = new SecurityGuard();
        terminalTool = new TerminalTool(guard);
    }

    @Test
    void testExecuteCommandCapturesOutputAndDuration(@TempDir Path tempDir) {
        ToolResult result = terminalTool.execute(
                Map.of("command", "echo 'hello harness'"),
                tempDir.toString()
        );

        assertTrue(result.success());
        assertTrue(result.output().contains("hello harness"));
        assertNotNull(result.metadata().get("exitCode"));
        assertEquals(0, result.metadata().get("exitCode"));
        assertTrue((Long) result.metadata().get("durationMs") >= 0);
    }

    @Test
    void testDangerousCommandBlocked(@TempDir Path tempDir) {
        ToolResult result = terminalTool.execute(
                Map.of("command", "rm -rf /"),
                tempDir.toString()
        );

        assertFalse(result.success());
        assertTrue(result.error().contains("Command rejected by security guard") || result.error().contains("safety restrictions"));
    }

    @Test
    void testFailingCommandCapturesExitCode(@TempDir Path tempDir) {
        ToolResult result = terminalTool.execute(
                Map.of("command", "ls /non_existent_folder_abc123"),
                tempDir.toString()
        );

        assertFalse(result.success());
        assertNotNull(result.error());
    }
}
