package com.example.codingagent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryToolTest {

    private RepositoryTool repositoryTool;

    @BeforeEach
    void setUp() {
        SecurityGuard guard = new SecurityGuard();
        repositoryTool = new RepositoryTool(guard);
    }

    @Test
    void testDetectMaven(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");
        assertEquals("Java/Maven", repositoryTool.detectProjectType(tempDir.toFile()));
    }

    @Test
    void testDetectGradle(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("build.gradle"), "// gradle");
        assertEquals("Java/Gradle", repositoryTool.detectProjectType(tempDir.toFile()));
    }

    @Test
    void testDetectNode(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("package.json"), "{}");
        assertEquals("Node.js (npm/yarn/pnpm)", repositoryTool.detectProjectType(tempDir.toFile()));
    }

    @Test
    void testDetectDotNet(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("MyApp.csproj"), "<Project></Project>");
        assertEquals(".NET (C#)", repositoryTool.detectProjectType(tempDir.toFile()));
    }

    @Test
    void testDetectCpp(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("CMakeLists.txt"), "cmake_minimum_required()");
        assertEquals("C/C++ (CMake)", repositoryTool.detectProjectType(tempDir.toFile()));
    }

    @Test
    void testExecuteInspectionReport(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");
        Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(tempDir.resolve("src/App.java"), "class App {}");

        ToolResult result = repositoryTool.execute(Map.of("maxDepth", 3), tempDir.toString());

        assertTrue(result.success());
        assertTrue(result.output().contains("Detected Framework: Java/Maven"));
        assertTrue(result.output().contains("pom.xml"));
        assertEquals("Java/Maven", result.metadata().get("projectType"));
    }
}
