package com.example.codingagent.agent;

import java.time.Instant;

public class AgentStep {
    private int stepNumber;
    private String title;
    private String description;
    private String action;
    private StepStatus status = StepStatus.PENDING;
    private String resultSummary;
    private Instant startedAt;
    private Instant completedAt;

    public enum StepStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        SKIPPED
    }

    public AgentStep() {}

    public AgentStep(int stepNumber, String title, String description) {
        this.stepNumber = stepNumber;
        this.title = title;
        this.description = description;
        this.status = StepStatus.PENDING;
    }

    public AgentStep(int stepNumber, String title, String description, String action) {
        this.stepNumber = stepNumber;
        this.title = title;
        this.description = description;
        this.action = action;
        this.status = StepStatus.PENDING;
    }

    public void start() {
        this.status = StepStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void complete(String summary) {
        this.status = StepStatus.COMPLETED;
        this.resultSummary = summary;
        this.completedAt = Instant.now();
    }

    public void fail(String error) {
        this.status = StepStatus.FAILED;
        this.resultSummary = error;
        this.completedAt = Instant.now();
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public StepStatus getStatus() {
        return status;
    }

    public void setStatus(StepStatus status) {
        this.status = status;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
