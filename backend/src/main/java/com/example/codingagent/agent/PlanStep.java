package com.example.codingagent.agent;

public class PlanStep {
    private int stepNumber;
    private String title;
    private String description;
    private StepStatus status = StepStatus.PENDING;

    public enum StepStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        SKIPPED
    }

    public PlanStep() {}

    public PlanStep(int stepNumber, String title, String description) {
        this.stepNumber = stepNumber;
        this.title = title;
        this.description = description;
        this.status = StepStatus.PENDING;
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

    public StepStatus getStatus() {
        return status;
    }

    public void setStatus(StepStatus status) {
        this.status = status;
    }
}
