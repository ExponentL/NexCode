package com.example.codingagent.agent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AgentState {

    private String taskId;
    private String task;
    private String repositoryPath;
    private AgentStatus status = AgentStatus.IDLE;
    private String currentStep = "Awaiting execution";
    private int attempts = 0;
    private int maxAttempts = 5;
    private List<AgentStep> plan = new ArrayList<>();
    private List<String> filesInspected = new ArrayList<>();
    private List<String> filesModified = new ArrayList<>();
    private List<String> commandsExecuted = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private List<TestResultItem> testResults = new ArrayList<>();
    private List<ActionResult> actionResults = new ArrayList<>();
    private com.example.codingagent.testing.TestResult latestTestResult;
    private com.example.codingagent.verification.VerificationResult latestVerificationResult;
    private com.example.codingagent.verification.TaskResult finalTaskResult;
    private String diff = "";
    private String evidenceSummary = "";
    private String detectedTestCommand;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public AgentState() {}

    public AgentState(String taskId, String task, String repositoryPath, int maxAttempts) {
        this.taskId = taskId;
        this.task = task;
        this.repositoryPath = repositoryPath;
        this.maxAttempts = maxAttempts;
        this.status = AgentStatus.IDLE;
        this.currentStep = "Task registered";
    }

    public synchronized void recordFileInspected(String file) {
        if (!filesInspected.contains(file)) {
            filesInspected.add(file);
        }
        this.updatedAt = Instant.now();
    }

    public synchronized void recordFileModified(String file) {
        if (!filesModified.contains(file)) {
            filesModified.add(file);
        }
        this.updatedAt = Instant.now();
    }

    public synchronized void recordCommandExecuted(String cmd) {
        commandsExecuted.add(cmd);
        this.updatedAt = Instant.now();
    }

    public synchronized void recordError(String error) {
        errors.add(error);
        this.updatedAt = Instant.now();
    }

    public synchronized void recordTestResult(TestResultItem item) {
        testResults.add(item);
        this.updatedAt = Instant.now();
    }

    public synchronized void recordActionResult(ActionResult result) {
        actionResults.add(result);
        this.updatedAt = Instant.now();
    }

    public synchronized void addPlanStep(AgentStep step) {
        plan.add(step);
        this.updatedAt = Instant.now();
    }

    public synchronized AgentStep getStepByNumber(int stepNumber) {
        return plan.stream()
                .filter(s -> s.getStepNumber() == stepNumber)
                .findFirst()
                .orElse(null);
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
        this.updatedAt = Instant.now();
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
        this.updatedAt = Instant.now();
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public List<AgentStep> getPlan() {
        return plan;
    }

    public void setPlan(List<AgentStep> plan) {
        this.plan = plan;
        this.updatedAt = Instant.now();
    }

    public List<String> getFilesInspected() {
        return filesInspected;
    }

    public void setFilesInspected(List<String> filesInspected) {
        this.filesInspected = filesInspected;
    }

    public List<String> getFilesModified() {
        return filesModified;
    }

    public void setFilesModified(List<String> filesModified) {
        this.filesModified = filesModified;
    }

    public List<String> getCommandsExecuted() {
        return commandsExecuted;
    }

    public void setCommandsExecuted(List<String> commandsExecuted) {
        this.commandsExecuted = commandsExecuted;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<TestResultItem> getTestResults() {
        return testResults;
    }

    public void setTestResults(List<TestResultItem> testResults) {
        this.testResults = testResults;
    }

    public List<ActionResult> getActionResults() {
        return actionResults;
    }

    public void setActionResults(List<ActionResult> actionResults) {
        this.actionResults = actionResults;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
        this.updatedAt = Instant.now();
    }

    public String getEvidenceSummary() {
        return evidenceSummary;
    }

    public void setEvidenceSummary(String evidenceSummary) {
        this.evidenceSummary = evidenceSummary;
        this.updatedAt = Instant.now();
    }

    public com.example.codingagent.testing.TestResult getLatestTestResult() {
        return latestTestResult;
    }

    public void setLatestTestResult(com.example.codingagent.testing.TestResult latestTestResult) {
        this.latestTestResult = latestTestResult;
        this.updatedAt = Instant.now();
    }

    public com.example.codingagent.verification.VerificationResult getLatestVerificationResult() {
        return latestVerificationResult;
    }

    public void setLatestVerificationResult(com.example.codingagent.verification.VerificationResult latestVerificationResult) {
        this.latestVerificationResult = latestVerificationResult;
        this.updatedAt = Instant.now();
    }

    public com.example.codingagent.verification.TaskResult getFinalTaskResult() {
        return finalTaskResult;
    }

    public void setFinalTaskResult(com.example.codingagent.verification.TaskResult finalTaskResult) {
        this.finalTaskResult = finalTaskResult;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public synchronized String getDetectedTestCommand() {
        return detectedTestCommand;
    }

    public synchronized void setDetectedTestCommand(String detectedTestCommand) {
        this.detectedTestCommand = detectedTestCommand;
        this.updatedAt = Instant.now();
    }
}
