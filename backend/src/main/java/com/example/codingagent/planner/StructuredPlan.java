package com.example.codingagent.planner;

import com.example.codingagent.agent.AgentStep;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructuredPlan {

    private String taskId;
    private String taskDescription;
    private List<StructuredPlanStep> steps = new ArrayList<>();
    private int currentStepIndex = 0;

    public StructuredPlan() {}

    public StructuredPlan(String taskId, String taskDescription, List<StructuredPlanStep> steps) {
        this.taskId = taskId;
        this.taskDescription = taskDescription;
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public StructuredPlanStep getCurrentStep() {
        if (currentStepIndex >= 0 && currentStepIndex < steps.size()) {
            return steps.get(currentStepIndex);
        }
        return null;
    }

    public void advanceStep() {
        if (currentStepIndex < steps.size() - 1) {
            currentStepIndex++;
        }
    }

    public void markCurrentStepCompleted(String notes) {
        StructuredPlanStep step = getCurrentStep();
        if (step != null) {
            step.complete(notes);
            advanceStep();
        }
    }

    public void markCurrentStepFailed(String error) {
        StructuredPlanStep step = getCurrentStep();
        if (step != null) {
            step.fail(error);
        }
    }

    public List<AgentStep> toAgentSteps() {
        List<AgentStep> result = new ArrayList<>();
        for (StructuredPlanStep sp : steps) {
            AgentStep as = new AgentStep(sp.getStepNumber(), sp.getTitle(), sp.getDescription(), sp.getPhase().name());
            as.setStatus(sp.getStatus());
            as.setResultSummary(sp.getNotes());
            result.add(as);
        }
        return result;
    }

    // Getters and setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public List<StructuredPlanStep> getSteps() {
        return steps;
    }

    public void setSteps(List<StructuredPlanStep> steps) {
        this.steps = steps;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public void setCurrentStepIndex(int currentStepIndex) {
        this.currentStepIndex = currentStepIndex;
    }
}
