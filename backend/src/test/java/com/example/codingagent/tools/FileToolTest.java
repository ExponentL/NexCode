package com.example.codingagent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileToolTest {

    private FileTool fileTool;

    @BeforeEach
    void setUp() {
        SecurityGuard guard = new SecurityGuard();
        fileTool = new FileTool(guard);
    }

    @Test
    void testCreateWriteAndReadFile(@TempDir Path tempDir) {
        String repoPath = tempDir.toString();

        // 1. CREATE_FILE
        ToolResult createRes = fileTool.execute(Map.of(
                "action", "CREATE_FILE",
                "path", "src/Main.java",
                "content", "public class Main {}"
        ), repoPath);

        assertTrue(createRes.success(), "CREATE_FILE should succeed: " + createRes.error());

        // 2. READ_FILE
        ToolResult readRes = fileTool.execute(Map.of(
                "action", "READ_FILE",
                "path", "src/Main.java"
        ), repoPath);

        assertTrue(readRes.success());
        assertTrue(readRes.output().contains("public class Main {}"));

        // 3. WRITE_FILE (overwrite)
        ToolResult writeRes = fileTool.execute(Map.of(
                "action", "WRITE_FILE",
                "path", "src/Main.java",
                "content", "public class Main { int x = 42; }"
        ), repoPath);

        assertTrue(writeRes.success());

        ToolResult readAgain = fileTool.execute(Map.of(
                "action", "READ_FILE",
                "path", "src/Main.java"
        ), repoPath);

        assertTrue(readAgain.output().contains("int x = 42;"));
    }

    @Test
    void testDeleteFileSecurity(@TempDir Path tempDir) {
        String repoPath = tempDir.toString();

        // Create file
        fileTool.execute(Map.of(
                "action", "CREATE_FILE",
                "path", "temp.txt",
                "content", "to be deleted"
        ), repoPath);

        // Delete without confirmation should fail
        ToolResult delFail = fileTool.execute(Map.of(
                "action", "DELETE_FILE",
                "path", "temp.txt"
        ), repoPath);

        assertFalse(delFail.success());
        assertTrue(delFail.error().contains("allowDelete: true"));

        // Delete with confirmation should succeed
        ToolResult delSuccess = fileTool.execute(Map.of(
                "action", "DELETE_FILE",
                "path", "temp.txt",
                "allowDelete", true
        ), repoPath);

        assertTrue(delSuccess.success());
    }

    @Test
    void testPathTraversalBlocked(@TempDir Path tempDir) {
        String repoPath = tempDir.toString();

        ToolResult res = fileTool.execute(Map.of(
                "action", "READ_FILE",
                "path", "../../etc/passwd"
        ), repoPath);

        assertFalse(res.success());
        assertTrue(res.error().contains("Security violation") || res.error().contains("Path traversal"));
    }
}
