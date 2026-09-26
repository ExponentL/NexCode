package com.example.codingagent.controller;

import com.example.codingagent.workspace.WorkspaceSelectionResult;
import com.example.codingagent.workspace.WorkspaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    /**
     * Opens the native OS directory chooser (or accepts a path) and sets the active workspace.
     */
    @PostMapping("/select")
    public ResponseEntity<WorkspaceSelectionResult> selectWorkspace(
            @RequestBody(required = false) Map<String, String> request) {
        if (request != null && request.containsKey("path") && !request.get("path").isBlank()) {
            WorkspaceSelectionResult result = workspaceService.setWorkspace(request.get("path"));
            return ResponseEntity.ok(result);
        }
        WorkspaceSelectionResult result = workspaceService.selectDirectoryViaNativeDialog();
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves the currently active workspace and its dynamic inspection analysis.
     */
    @GetMapping("/current")
    public ResponseEntity<WorkspaceSelectionResult> getCurrentWorkspace() {
        WorkspaceSelectionResult result = workspaceService.getCurrentWorkspace();
        return ResponseEntity.ok(result);
    }

    /**
     * Explicitly sets the current workspace path.
     */
    @PostMapping("/current")
    public ResponseEntity<WorkspaceSelectionResult> setCurrentWorkspace(
            @RequestBody Map<String, String> request) {
        String path = request.get("path");
        WorkspaceSelectionResult result = workspaceService.setWorkspace(path);
        return ResponseEntity.ok(result);
    }

    /**
     * Clones a remote GitHub repository and sets it as the active workspace.
     */
    @PostMapping("/clone-github")
    public ResponseEntity<WorkspaceSelectionResult> cloneGitHub(
            @RequestBody Map<String, String> request) {
        String url = request.get("url");
        WorkspaceSelectionResult result = workspaceService.cloneGitHubRepository(url);
        return ResponseEntity.ok(result);
    }
}
