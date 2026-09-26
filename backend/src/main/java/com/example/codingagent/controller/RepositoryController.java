package com.example.codingagent.controller;

import com.example.codingagent.config.AgentProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

    private final AgentProperties properties;

    public RepositoryController(AgentProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, String>>> listRepositories() {
        List<Map<String, String>> repos = new ArrayList<>();

        Path rootPath = Paths.get(properties.getWorkspaceRoot()).toAbsolutePath().normalize();
        File rootDir = rootPath.toFile();

        Map<String, String> currentWorkspace = new HashMap<>();
        currentWorkspace.put("name", rootDir.getName() + " (Workspace Root)");
        currentWorkspace.put("path", rootDir.getAbsolutePath());
        currentWorkspace.put("type", detectType(rootDir));
        repos.add(currentWorkspace);

        if (rootDir.exists() && rootDir.isDirectory()) {
            try (Stream<Path> stream = Files.walk(rootPath, 2)) {
                stream.filter(Files::isDirectory)
                        .filter(p -> !p.equals(rootPath))
                        .filter(p -> !p.toString().contains("/.") && !p.toString().contains("/target") && !p.toString().contains("/node_modules"))
                        .filter(p -> hasProjectIndicators(p.toFile()))
                        .forEach(p -> {
                            Map<String, String> item = new HashMap<>();
                            item.put("name", p.getFileName().toString());
                            item.put("path", p.toAbsolutePath().toString());
                            item.put("type", detectType(p.toFile()));
                            repos.add(item);
                        });
            } catch (IOException ignored) {
            }
        }

        return ResponseEntity.ok(repos);
    }

    private boolean hasProjectIndicators(File dir) {
        return new File(dir, ".git").exists() ||
                new File(dir, "pom.xml").exists() ||
                new File(dir, "package.json").exists() ||
                new File(dir, "build.gradle").exists();
    }

    private String detectType(File dir) {
        if (new File(dir, "pom.xml").exists()) return "Java / Maven";
        if (new File(dir, "package.json").exists()) return "Node.js / React";
        if (new File(dir, "build.gradle").exists()) return "Java / Gradle";
        if (new File(dir, ".git").exists()) return "Git Repository";
        return "Directory";
    }
}

