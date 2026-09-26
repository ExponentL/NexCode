package com.example.codingagent.planner;

import com.example.codingagent.agent.AgentStep;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructuredPlanStep {

    private int stepNumber;
    private PlanPhase phase;
    private String title;
    private String description;
    private AgentStep.StepStatus status = AgentStep.StepStatus.PENDING;
    private String targetPath;
    private String notes;
    private Instant startedAt;
    private Instant completedAt;

    public StructuredPlanStep() {}

    public StructuredPlanStep(int stepNumber, PlanPhase phase, String title, String description) {
        this.stepNumber = stepNumber;
        this.phase = phase;
        this.title = title;
        this.description = description;
    }

    public StructuredPlanStep(int stepNumber, PlanPhase phase, String title, String description, String targetPath) {
        this.stepNumber = stepNumber;
        this.phase = phase;
        this.title = title;
        this.description = description;
        this.targetPath = targetPath;
    }

    public void start() {
        this.status = AgentStep.StepStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void complete(String notes) {
        this.status = AgentStep.StepStatus.COMPLETED;
        this.notes = notes;
        this.completedAt = Instant.now();
    }

    public void fail(String error) {
        this.status = AgentStep.StepStatus.FAILED;
        this.notes = error;
        this.completedAt = Instant.now();
    }

    // Getters and setters
    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public PlanPhase getPhase() {
        return phase;
    }

    public void setPhase(PlanPhase phase) {
        this.phase = phase;
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

    public AgentStep.StepStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStep.StepStatus status) {
        this.status = status;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
