package com.example.codingagent.repository;

import com.example.codingagent.tools.TerminalTool;
import com.example.codingagent.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class RepositoryAnalyzerTest {

    private TerminalTool terminalTool;
    private RepositoryAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        terminalTool = Mockito.mock(TerminalTool.class);
        when(terminalTool.execute(any(), anyString()))
                .thenReturn(ToolResult.success("", Map.of("exitCode", 0)));
        analyzer = new RepositoryAnalyzer(terminalTool);
    }

    @Test
    void testAnalyzeNonExistentPath() {
        RepositoryAnalysis analysis = analyzer.analyze("/path/that/definitely/does/not/exist");
        assertFalse(analysis.exists());
        assertNotNull(analysis.errorMessage());
        assertEquals("Directory not accessible", analysis.gitStatus());
    }

    @Test
    void testAnalyzeMavenRepository(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");
        Files.createDirectories(tempDir.resolve("src/main/java/com/example"));
        Files.writeString(tempDir.resolve("src/main/java/com/example/App.java"), "public class App {}");
        Files.createDirectories(tempDir.resolve("src/test/java/com/example"));
        Files.writeString(tempDir.resolve("src/test/java/com/example/AppTest.java"), "public class AppTest {}");
        Files.writeString(tempDir.resolve("README.md"), "# Demo Project\nThis is a test readme.");

        RepositoryAnalysis analysis = analyzer.analyze(tempDir.toString());

        assertTrue(analysis.exists());
        assertEquals("Maven / Java", analysis.projectType());
        assertEquals("Maven", analysis.buildSystem());
        assertTrue(analysis.detectedLanguages().contains("Java"));
        assertTrue(analysis.availableCommands().contains("mvn test"));
        assertEquals("Demo Project", analysis.readmeSummary());
        assertTrue(analysis.totalFiles() >= 3);
    }

    @Test
    void testAnalyzeNodeRepository(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("package.json"), "{\"name\": \"my-app\"}");
        Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(tempDir.resolve("src/index.ts"), "console.log('hello');");

        RepositoryAnalysis analysis = analyzer.analyze(tempDir.toString());

        assertTrue(analysis.exists());
        assertEquals("Node.js", analysis.projectType());
        assertTrue(analysis.detectedLanguages().contains("TypeScript"));
    }

    @Test
    void testAnalyzeRustRepository(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("Cargo.toml"), "[package]\nname = \"rust-demo\"");
        Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(tempDir.resolve("src/main.rs"), "fn main() {}");

        RepositoryAnalysis analysis = analyzer.analyze(tempDir.toString());

        assertTrue(analysis.exists());
        assertEquals("Rust", analysis.projectType());
        assertEquals("Cargo", analysis.buildSystem());
        assertTrue(analysis.detectedLanguages().contains("Rust"));
        assertTrue(analysis.availableCommands().contains("cargo test"));
    }

    @Test
    void testAnalyzeUnrecognizedGenericRepository(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("notes.txt"), "Just some text notes");

        RepositoryAnalysis analysis = analyzer.analyze(tempDir.toString());

        assertTrue(analysis.exists());
        assertEquals("Project type not automatically detected", analysis.projectType());
        assertEquals("None detected", analysis.buildSystem());
        assertTrue(analysis.availableCommands().isEmpty());
    }
}
