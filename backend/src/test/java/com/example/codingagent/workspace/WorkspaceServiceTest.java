package com.example.codingagent.workspace;

import com.example.codingagent.config.AgentProperties;
import com.example.codingagent.controller.WorkspaceController;
import com.example.codingagent.repository.RepositoryAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceServiceTest {

    @TempDir
    Path tempDir;

    private AgentProperties properties;
    private RepositoryAnalyzer analyzer;
    private WorkspaceService workspaceService;
    private WorkspaceController workspaceController;

    @BeforeEach
    void setUp() {
        properties = new AgentProperties();
        properties.setWorkspaceRoot(tempDir.toString());
        analyzer = new RepositoryAnalyzer();
        workspaceService = new WorkspaceService(properties, analyzer);
        workspaceController = new WorkspaceController(workspaceService);
    }

    @Test
    void testSetValidWorkspace() throws IOException {
        Path projectDir = Files.createDirectory(tempDir.resolve("my-web-app"));
        Files.writeString(projectDir.resolve("package.json"), "{\"name\": \"my-web-app\", \"version\": \"1.0.0\"}");
        Files.createDirectory(projectDir.resolve("src"));

        WorkspaceSelectionResult result = workspaceService.setWorkspace(projectDir.toString());

        assertNotNull(result);
        assertFalse(result.cancelled());
        assertTrue(result.exists());
        assertTrue(result.readable());
        assertEquals("my-web-app", result.name());
        assertNotNull(result.analysis());
        assertEquals("Node.js", result.analysis().projectType());
        assertEquals(projectDir.toRealPath().toString(), properties.getWorkspaceRoot());
    }

    @Test
    void testSetInvalidWorkspaceNonExistent() {
        WorkspaceSelectionResult result = workspaceService.setWorkspace(tempDir.resolve("does-not-exist").toString());

        assertNotNull(result);
        assertFalse(result.cancelled());
        assertFalse(result.exists());
        assertTrue(result.message().contains("does not exist"));
    }

    @Test
    void testSetInvalidWorkspaceFileInsteadOfDirectory() throws IOException {
        Path someFile = Files.createFile(tempDir.resolve("file.txt"));
        WorkspaceSelectionResult result = workspaceService.setWorkspace(someFile.toString());

        assertNotNull(result);
        assertFalse(result.cancelled());
        assertFalse(result.exists());
        assertTrue(result.message().contains("not a directory"));
    }

    @Test
    void testControllerSelectWorkspaceWithPath() throws IOException {
        Path projectDir = Files.createDirectory(tempDir.resolve("cargo-crate"));
        Files.writeString(projectDir.resolve("Cargo.toml"), "[package]\nname = \"crate\"\nversion = \"0.1.0\"");

        ResponseEntity<WorkspaceSelectionResult> response = workspaceController.selectWorkspace(
                Map.of("path", projectDir.toString())
        );

        assertEquals(200, response.getStatusCode().value());
        WorkspaceSelectionResult body = response.getBody();
        assertNotNull(body);
        assertTrue(body.exists());
        assertEquals("Rust", body.analysis().projectType());
        assertEquals("Cargo", body.analysis().buildSystem());
    }

    @Test
    void testControllerGetCurrentWorkspace() {
        ResponseEntity<WorkspaceSelectionResult> response = workspaceController.getCurrentWorkspace();
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void testCloneEmptyUrl() {
        WorkspaceSelectionResult result = workspaceService.cloneGitHubRepository("");
        assertNotNull(result);
        assertFalse(result.exists());
        assertTrue(result.message().contains("cannot be empty"));
    }

    @Test
    void testControllerCloneGitHubEmpty() {
        ResponseEntity<WorkspaceSelectionResult> response = workspaceController.cloneGitHub(Map.of("url", "   "));
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().exists());
    }
}
