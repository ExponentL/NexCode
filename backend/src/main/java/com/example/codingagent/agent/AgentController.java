package com.example.codingagent.agent;

import com.example.codingagent.config.AgentProperties;
import com.example.codingagent.tools.GitTool;
import com.example.codingagent.tools.ToolResult;
import com.example.codingagent.verification.TaskResult;
import com.example.codingagent.verification.VerificationManager;
import com.example.codingagent.verification.VerificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final Map<String, AgentState> tasks = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> taskCancelFlags = new ConcurrentHashMap<>();

    private final AgentLoop agentLoop;
    private final AgentProperties properties;
    private final Executor taskExecutor;
    private final GitTool gitTool;
    private VerificationManager verificationManager;

    @Autowired
    public AgentController(
            AgentLoop agentLoop,
            AgentProperties properties,
            @Qualifier("agentTaskExecutor") Executor taskExecutor,
            GitTool gitTool,
            VerificationManager verificationManager) {
        this.agentLoop = agentLoop;
        this.properties = properties;
        this.taskExecutor = taskExecutor;
        this.gitTool = gitTool;
        this.verificationManager = verificationManager;
    }

    // Constructor for testing with mock without verificationManager
    public AgentController(
            AgentLoop agentLoop,
            AgentProperties properties,
            Executor taskExecutor,
            GitTool gitTool) {
        this.agentLoop = agentLoop;
        this.properties = properties;
        this.taskExecutor = taskExecutor;
        this.gitTool = gitTool;
    }

    public AgentState createTask(String taskPrompt, String repositoryPath) {
        String taskId = UUID.randomUUID().toString();
        String resolvedRepo = (repositoryPath != null && !repositoryPath.isBlank())
                ? repositoryPath
                : properties.getWorkspaceRoot();

        AgentState state = new AgentState(taskId, taskPrompt, resolvedRepo, properties.getMaxAttempts());
        tasks.put(taskId, state);
        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        taskCancelFlags.put(taskId, cancelFlag);

        log.info("Dispatching agent task {} for repo {}", taskId, resolvedRepo);
        taskExecutor.execute(() -> agentLoop.run(state, cancelFlag));
        return state;
    }

    public AgentState getTask(String taskId) {
        return tasks.get(taskId);
    }

    public List<AgentState> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public boolean cancelTask(String taskId) {
        AtomicBoolean flag = taskCancelFlags.get(taskId);
        if (flag != null) {
            flag.set(true);
            AgentState state = tasks.get(taskId);
            if (state != null && state.getStatus() != AgentStatus.COMPLETED && state.getStatus() != AgentStatus.FAILED) {
                state.setStatus(AgentStatus.CANCELLED);
                state.setCurrentStep("Cancelled by user");
            }
            return true;
        }
        return false;
    }

    public String getDiff(String taskId) {
        AgentState state = tasks.get(taskId);
        if (state == null) {
            return "";
        }
        if (state.getDiff() != null && !state.getDiff().isBlank()) {
            return state.getDiff();
        }
        ToolResult res = gitTool.execute(Map.of("action", "diff"), state.getRepositoryPath());
        if (res.success() && res.output() != null) {
            state.setDiff(res.output());
            return res.output();
        }
        return "";
    }

    public TaskResult getTaskResult(String taskId) {
        AgentState state = tasks.get(taskId);
        if (state == null) return null;
        if (state.getFinalTaskResult() != null) return state.getFinalTaskResult();
        if ((state.getStatus() == AgentStatus.COMPLETED || state.getStatus() == AgentStatus.FAILED) && verificationManager != null) {
            VerificationResult vr = getVerificationResult(taskId);
            TaskResult tr = verificationManager.generateTaskResult(state, vr);
            state.setFinalTaskResult(tr);
            return tr;
        }
        return null;
    }

    public VerificationResult getVerificationResult(String taskId) {
        AgentState state = tasks.get(taskId);
        if (state == null) return null;
        if (state.getLatestVerificationResult() != null) return state.getLatestVerificationResult();
        if (verificationManager != null) {
            VerificationResult vr = verificationManager.verify(state);
            state.setLatestVerificationResult(vr);
            return vr;
        }
        return null;
    }
}
