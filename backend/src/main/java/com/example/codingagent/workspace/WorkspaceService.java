package com.example.codingagent.workspace;

import com.example.codingagent.config.AgentProperties;
import com.example.codingagent.repository.RepositoryAnalysis;
import com.example.codingagent.repository.RepositoryAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceService.class);

    private final AgentProperties properties;
    private final RepositoryAnalyzer repositoryAnalyzer;

    private volatile String currentWorkspacePath;
    private volatile RepositoryAnalysis currentAnalysis;

    public WorkspaceService(AgentProperties properties, RepositoryAnalyzer repositoryAnalyzer) {
        this.properties = properties;
        this.repositoryAnalyzer = repositoryAnalyzer;

        // Initialize with default workspace root if valid
        String initialRoot = properties.getWorkspaceRoot();
        if (initialRoot != null && !initialRoot.isBlank()) {
            File initialDir = new File(initialRoot);
            if (initialDir.exists() && initialDir.isDirectory()) {
                try {
                    this.currentWorkspacePath = initialDir.getCanonicalPath();
                    this.currentAnalysis = repositoryAnalyzer.analyze(this.currentWorkspacePath);
                } catch (Exception e) {
                    this.currentWorkspacePath = initialDir.getAbsolutePath();
                }
            }
        }
    }

    /**
     * Prompts the user with a native operating system folder chooser.
     * On macOS, invokes Apple Aqua choose folder dialog via osascript.
     * On other platforms or fallback, uses JFileChooser.
     */
    public WorkspaceSelectionResult selectDirectoryViaNativeDialog() {
        String os = System.getProperty("os.name", "").toLowerCase();
        log.info("Opening native directory chooser for OS: {}", os);

        if (os.contains("mac")) {
            return selectViaMacOsascript();
        } else {
            return selectViaJFileChooser();
        }
    }

    /**
     * Sets and validates an explicit directory path as the active workspace.
     */
    public WorkspaceSelectionResult setWorkspace(String path) {
        if (path == null || path.isBlank()) {
            return WorkspaceSelectionResult.error("Directory path cannot be empty");
        }

        try {
            Path targetPath = Paths.get(path).toAbsolutePath().normalize();
            File targetDir = targetPath.toFile();

            if (!targetDir.exists()) {
                return WorkspaceSelectionResult.error("Selected path does not exist: " + path);
            }
            if (!targetDir.isDirectory()) {
                return WorkspaceSelectionResult.error("Selected path is not a directory: " + path);
            }

            String canonicalPath;
            try {
                canonicalPath = targetDir.getCanonicalPath();
            } catch (Exception e) {
                canonicalPath = targetDir.getAbsolutePath();
            }

            RepositoryAnalysis analysis = repositoryAnalyzer.analyze(canonicalPath);

            this.currentWorkspacePath = canonicalPath;
            this.currentAnalysis = analysis;
            this.properties.setWorkspaceRoot(canonicalPath);

            log.info("Active workspace updated to: {}", canonicalPath);
            return WorkspaceSelectionResult.success(canonicalPath, targetDir.getName(), analysis);
        } catch (Exception e) {
            log.error("Failed to set workspace path: {}", path, e);
            return WorkspaceSelectionResult.error("Failed to access directory: " + e.getMessage());
        }
    }

    /**
     * Clones a remote Git / GitHub repository into a managed local workspace
     * and immediately analyzes the project structure.
     */
    public WorkspaceSelectionResult cloneGitHubRepository(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return WorkspaceSelectionResult.error("GitHub repository URL cannot be empty.");
        }

        String url = rawUrl.trim();
        String cleaned = url.replaceAll("/+$", "").replaceAll("\\.git$", "");
        String repoName = cleaned.substring(cleaned.lastIndexOf('/') + 1);
        if (repoName.isBlank()) {
            repoName = "repo-" + System.currentTimeMillis();
        }

        Path cloneBaseDir = Paths.get(System.getProperty("user.home"), ".codingagent", "cloned-repositories");
        try {
            Files.createDirectories(cloneBaseDir);
        } catch (IOException e) {
            return WorkspaceSelectionResult.error("Failed to create repositories directory: " + e.getMessage());
        }

        Path targetRepoDir = cloneBaseDir.resolve(repoName).toAbsolutePath().normalize();

        // If repository directory already exists on disk
        if (Files.exists(targetRepoDir)) {
            if (Files.exists(targetRepoDir.resolve(".git"))) {
                log.info("Repository already cloned at {}. Pulling latest changes...", targetRepoDir);
                try {
                    ProcessBuilder pullPb = new ProcessBuilder("git", "pull");
                    pullPb.directory(targetRepoDir.toFile());
                    pullPb.environment().put("GIT_TERMINAL_PROMPT", "0");
                    pullPb.environment().put("GIT_ASKPASS", "echo");
                    pullPb.redirectErrorStream(true);
                    Process pullProc = pullPb.start();

                    Thread pullReader = new Thread(() -> {
                        try (BufferedReader r = new BufferedReader(new InputStreamReader(pullProc.getInputStream()))) {
                            while (r.readLine() != null) {}
                        } catch (Exception ignored) {}
                    });
                    pullReader.start();

                    boolean pullFinished = pullProc.waitFor(15, TimeUnit.SECONDS);
                    pullReader.join(1000);
                    if (!pullFinished) {
                        pullProc.destroyForcibly();
                    }
                } catch (Exception e) {
                    log.warn("Git pull failed, using existing repository: {}", e.getMessage());
                }
                return setWorkspace(targetRepoDir.toString());
            } else {
                // Incomplete or corrupted clone directory exists, clean it up before re-cloning
                try {
                    log.info("Cleaning up incomplete repository directory at {}", targetRepoDir);
                    org.springframework.util.FileSystemUtils.deleteRecursively(targetRepoDir);
                } catch (Exception e) {
                    log.warn("Failed to delete incomplete repository directory {}: {}", targetRepoDir, e.getMessage());
                }
            }
        }

        log.info("Cloning repository {} to {}", url, targetRepoDir);
        try {
            ProcessBuilder clonePb = new ProcessBuilder("git", "clone", "--depth", "1", url, targetRepoDir.toString());
            clonePb.environment().put("GIT_TERMINAL_PROMPT", "0");
            clonePb.environment().put("GIT_ASKPASS", "echo");
            clonePb.redirectErrorStream(true);
            Process cloneProc = clonePb.start();

            StringBuilder output = new StringBuilder();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(cloneProc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (Exception ignored) {}
            });
            readerThread.start();

            boolean finished = cloneProc.waitFor(45, TimeUnit.SECONDS);
            readerThread.join(2000);

            if (!finished) {
                cloneProc.destroyForcibly();
                try {
                    org.springframework.util.FileSystemUtils.deleteRecursively(targetRepoDir);
                } catch (Exception ignored) {}
                return WorkspaceSelectionResult.error("Git clone timed out after 45 seconds.");
            }

            int exitCode = cloneProc.exitValue();
            if (exitCode != 0) {
                try {
                    org.springframework.util.FileSystemUtils.deleteRecursively(targetRepoDir);
                } catch (Exception ignored) {}
                String errMessage = output.toString().trim();
                log.error("Git clone failed (exit code {}): {}", exitCode, errMessage);
                if (errMessage.isBlank()) {
                    errMessage = "Git clone exited with code " + exitCode;
                }
                return WorkspaceSelectionResult.error("Failed to clone repository: " + errMessage);
            }

            log.info("Repository successfully cloned. Analyzing codebase...");
            return setWorkspace(targetRepoDir.toString());
        } catch (Exception e) {
            log.error("Error executing git clone: {}", e.getMessage(), e);
            try {
                org.springframework.util.FileSystemUtils.deleteRecursively(targetRepoDir);
            } catch (Exception ignored) {}
            return WorkspaceSelectionResult.error("Failed to execute git clone: " + e.getMessage());
        }
    }

    /**
     * Returns the currently active workspace and its dynamic analysis.
     */
    public WorkspaceSelectionResult getCurrentWorkspace() {
        if (currentWorkspacePath == null) {
            return WorkspaceSelectionResult.error("No active workspace selected");
        }
        File dir = new File(currentWorkspacePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return WorkspaceSelectionResult.error("Current workspace directory no longer exists on disk: " + currentWorkspacePath);
        }
        if (currentAnalysis == null) {
            currentAnalysis = repositoryAnalyzer.analyze(currentWorkspacePath);
        }
        return WorkspaceSelectionResult.success(currentWorkspacePath, dir.getName(), currentAnalysis);
    }

    private WorkspaceSelectionResult selectViaMacOsascript() {
        try {
            // AppleScript to show native Apple Aqua folder selection dialog
            String script = "POSIX path of (choose folder with prompt \"Select Project / Repository Directory\")";
            ProcessBuilder pb = new ProcessBuilder("osascript", "-e", script);
            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread outThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (Exception ignored) {}
            });

            Thread errThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (Exception ignored) {}
            });

            outThread.start();
            errThread.start();

            boolean finished = process.waitFor(180, TimeUnit.SECONDS);
            outThread.join(2000);
            errThread.join(2000);

            if (!finished) {
                process.destroyForcibly();
                log.warn("Native directory chooser timed out after 180 seconds");
                return WorkspaceSelectionResult.cancelled("Directory selection timed out");
            }

            int exitCode = process.exitValue();
            String output = stdout.toString().replaceAll("[\\r\\n]+$", "");
            String errorOutput = stderr.toString().trim();

            if (exitCode == 0 && !output.isBlank()) {
                // Trim trailing slash from osascript POSIX path if present
                if (output.endsWith("/")) {
                    output = output.substring(0, output.length() - 1);
                }
                log.info("Native macOS directory chooser returned path: {}", output);
                return setWorkspace(output);
            } else {
                if (errorOutput.contains("User canceled") || errorOutput.contains("-128") || exitCode == 1) {
                    log.info("User cancelled directory picker dialog");
                    return WorkspaceSelectionResult.cancelled("No directory selected");
                }
                log.warn("osascript failed with exit code {}: {}", exitCode, errorOutput);
                // Fallback to JFileChooser if osascript failed
                return selectViaJFileChooser();
            }
        } catch (Exception e) {
            log.warn("osascript invocation failed, falling back to JFileChooser", e);
            return selectViaJFileChooser();
        }
    }

    private WorkspaceSelectionResult selectViaJFileChooser() {
        if (GraphicsEnvironment.isHeadless()) {
            log.warn("System is headless, JFileChooser cannot be displayed");
            return WorkspaceSelectionResult.error("Native directory picker is not available in headless environment. Please input the directory path directly.");
        }

        try {
            AtomicReference<File> selectedFileRef = new AtomicReference<>();
            AtomicBoolean cancelledRef = new AtomicBoolean(false);
            AtomicReference<Exception> exceptionRef = new AtomicReference<>();

            Runnable swingTask = () -> {
                try {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception ignored) {}

                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Select Project / Repository Directory");
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    chooser.setAcceptAllFileFilterUsed(false);

                    if (currentWorkspacePath != null) {
                        File cur = new File(currentWorkspacePath);
                        if (cur.exists()) {
                            chooser.setCurrentDirectory(cur);
                        }
                    }

                    int result = chooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        selectedFileRef.set(chooser.getSelectedFile());
                    } else {
                        cancelledRef.set(true);
                    }
                } catch (Exception ex) {
                    exceptionRef.set(ex);
                }
            };

            if (SwingUtilities.isEventDispatchThread()) {
                swingTask.run();
            } else {
                SwingUtilities.invokeAndWait(swingTask);
            }

            if (exceptionRef.get() != null) {
                throw exceptionRef.get();
            }

            if (cancelledRef.get()) {
                return WorkspaceSelectionResult.cancelled("No directory selected");
            }

            File selectedDir = selectedFileRef.get();
            if (selectedDir != null) {
                return setWorkspace(selectedDir.getAbsolutePath());
            }

            return WorkspaceSelectionResult.cancelled("No directory selected");
        } catch (Exception e) {
            log.error("JFileChooser error", e);
            return WorkspaceSelectionResult.error("Native directory chooser failed: " + e.getMessage());
        }
    }
}
