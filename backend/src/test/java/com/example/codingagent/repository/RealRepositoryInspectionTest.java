package com.example.codingagent.repository;

import com.example.codingagent.tools.SecurityGuard;
import com.example.codingagent.tools.TerminalTool;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class RealRepositoryInspectionTest {

    @Test
    void testRealRepositoriesInspection() throws Exception {
        SecurityGuard guard = new SecurityGuard();
        TerminalTool terminalTool = new TerminalTool(guard);
        RepositoryAnalyzer analyzer = new RepositoryAnalyzer(terminalTool);

        // Project A: Real local Java / Maven repository (backend)
        String backendPath = new File("src/main/java").exists() ? "." : "backend";
        File backendDir = new File(backendPath).getCanonicalFile();

        RepositoryAnalysis analysisA = analyzer.analyze(backendDir.getAbsolutePath());
        System.out.println("=== PROJECT A (BACKEND) ANALYSIS ===");
        System.out.println("Path: " + analysisA.path());
        System.out.println("Project Type: " + analysisA.projectType());
        System.out.println("Build System: " + analysisA.buildSystem());
        System.out.println("Languages: " + analysisA.detectedLanguages());
        System.out.println("Commands: " + analysisA.availableCommands());
        System.out.println("Total Files: " + analysisA.totalFiles());

        assertTrue(analysisA.exists());
        assertEquals("Maven / Java", analysisA.projectType());
        assertEquals("Maven", analysisA.buildSystem());
        assertTrue(analysisA.detectedLanguages().contains("Java"));
        assertTrue(analysisA.availableCommands().contains("mvn test"));
        assertTrue(analysisA.totalFiles() > 20);

        // Project B: Real local Node.js / React repository (frontend)
        File frontendDir = new File(backendDir.getParentFile(), "frontend").getCanonicalFile();
        assertTrue(frontendDir.exists(), "frontend directory must exist at " + frontendDir);
            RepositoryAnalysis analysisB = analyzer.analyze(frontendDir.getAbsolutePath());
            System.out.println("\n=== PROJECT B (FRONTEND) ANALYSIS ===");
            System.out.println("Path: " + analysisB.path());
            System.out.println("Project Type: " + analysisB.projectType());
            System.out.println("Build System: " + analysisB.buildSystem());
            System.out.println("Languages: " + analysisB.detectedLanguages());
            System.out.println("Commands: " + analysisB.availableCommands());
            System.out.println("Total Files: " + analysisB.totalFiles());

            assertTrue(analysisB.exists());
            assertEquals("Node.js", analysisB.projectType());
            assertTrue(analysisB.detectedLanguages().contains("TypeScript"));
            assertTrue(analysisB.availableCommands().stream().anyMatch(c -> c.contains("test")));
            assertTrue(analysisB.totalFiles() > 10);

            // Proves Project A != Project B dynamically
            assertNotEquals(analysisA.projectType(), analysisB.projectType());
            assertNotEquals(analysisA.buildSystem(), analysisB.buildSystem());
    }
}
