package com.example.codingagent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchToolTest {

    private SearchTool searchTool;

    @BeforeEach
    void setUp() {
        SecurityGuard guard = new SecurityGuard();
        searchTool = new SearchTool(guard);
    }

    @Test
    void testSearchFilenamesAndContent(@TempDir Path tempDir) throws IOException {
        String repoPath = tempDir.toString();

        // Create sample files
        Path sub = tempDir.resolve("src");
        Files.createDirectories(sub);
        Files.writeString(sub.resolve("UserService.java"), "public class UserService {\n    void saveUser() {}\n}");
        Files.writeString(sub.resolve("OrderService.java"), "public class OrderService {\n    void saveOrder() {}\n}");

        // 1. SEARCH_FILENAMES
        ToolResult findRes = searchTool.execute(Map.of(
                "action", "SEARCH_FILENAMES",
                "query", "User"
        ), repoPath);

        assertTrue(findRes.success());
        assertTrue(findRes.output().contains("UserService.java"));
        assertFalse(findRes.output().contains("OrderService.java"));

        // 2. SEARCH_CONTENT
        ToolResult grepRes = searchTool.execute(Map.of(
                "action", "SEARCH_CONTENT",
                "query", "saveOrder"
        ), repoPath);

        assertTrue(grepRes.success());
        assertTrue(grepRes.output().contains("OrderService.java:2:"));
        assertTrue(grepRes.output().contains("void saveOrder()"));
    }
}
