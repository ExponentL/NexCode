package com.example.codingagent.controller;

import com.example.codingagent.agent.AgentController;
import com.example.codingagent.agent.AgentState;
import com.example.codingagent.logging.AgentEvent;
import com.example.codingagent.logging.AgentEventPublisher;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final AgentController agentController;
    private final AgentEventPublisher eventPublisher;

    public TaskController(AgentController agentController, AgentEventPublisher eventPublisher) {
        this.agentController = agentController;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    public ResponseEntity<AgentState> createTask(@Valid @RequestBody CreateTaskRequest request) {
        AgentState state = agentController.createTask(request.task(), request.repositoryPath());
        return ResponseEntity.ok(state);
    }

    @GetMapping
    public ResponseEntity<List<AgentState>> listTasks() {
        return ResponseEntity.ok(agentController.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentState> getTask(@PathVariable("id") String id) {
        AgentState state = agentController.getTask(id);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state);
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable("id") String id) {
        return eventPublisher.registerEmitter(id);
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AgentEvent>> getEventsHistory(@PathVariable("id") String id) {
        return ResponseEntity.ok(eventPublisher.getEvents(id));
    }

    @GetMapping("/{id}/diff")
    public ResponseEntity<Map<String, String>> getDiff(@PathVariable("id") String id) {
        AgentState state = agentController.getTask(id);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        String diff = agentController.getDiff(id);
        return ResponseEntity.ok(Map.of("diff", diff));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable("id") String id) {
        boolean cancelled = agentController.cancelTask(id);
        AgentState state = agentController.getTask(id);
        return ResponseEntity.ok(Map.of(
                "cancelled", cancelled,
                "status", state != null ? state.getStatus() : "UNKNOWN"
        ));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<com.example.codingagent.verification.TaskResult> getTaskResult(@PathVariable("id") String id) {
        com.example.codingagent.verification.TaskResult result = agentController.getTaskResult(id);
        if (result == null) {
            AgentState state = agentController.getTask(id);
            if (state == null) return ResponseEntity.notFound().build();
            // Return empty/in-progress task result representation if not completed
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/verification")
    public ResponseEntity<com.example.codingagent.verification.VerificationResult> getVerification(@PathVariable("id") String id) {
        com.example.codingagent.verification.VerificationResult verification = agentController.getVerificationResult(id);
        if (verification == null) {
            AgentState state = agentController.getTask(id);
            if (state == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(verification);
    }
}
