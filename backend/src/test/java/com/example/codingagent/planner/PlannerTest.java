package com.example.codingagent.planner;

import com.example.codingagent.agent.AgentStep;
import com.example.codingagent.context.RepositoryContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlannerTest {

    private Planner planner;
    private RepositoryContext mockRepo;

    @BeforeEach
    void setUp() {
        planner = new Planner();
        mockRepo = new RepositoryContext("/repo", "test-project", "Java/Maven", "README", List.of("pom.xml"), "clean");
    }

    @Test
    void testStructuredPlanGeneratesAllRequiredPhases() {
        StructuredPlan plan = planner.createPlan("t-123", "Fix the login validation bug", mockRepo);

        assertNotNull(plan);
        assertEquals("t-123", plan.getTaskId());
        assertEquals("Fix the login validation bug", plan.getTaskDescription());
        assertEquals(10, plan.getSteps().size());

        // Verify sequence of phases
        assertEquals(PlanPhase.UNDERSTAND_TASK, plan.getSteps().get(0).getPhase());
        assertEquals(PlanPhase.LOCATE_CODE, plan.getSteps().get(1).getPhase());
        assertEquals(PlanPhase.INSPECT_IMPLEMENTATION, plan.getSteps().get(2).getPhase());
        assertEquals(PlanPhase.INSPECT_TESTS, plan.getSteps().get(3).getPhase());
        assertEquals(PlanPhase.IDENTIFY_PROBLEM, plan.getSteps().get(4).getPhase());
        assertEquals(PlanPhase.MODIFY_CODE, plan.getSteps().get(5).getPhase());
        assertEquals(PlanPhase.RUN_TESTS, plan.getSteps().get(6).getPhase());
        assertEquals(PlanPhase.ANALYZE_FAILURES, plan.getSteps().get(7).getPhase());
        assertEquals(PlanPhase.REMEDIATE, plan.getSteps().get(8).getPhase());
        assertEquals(PlanPhase.VERIFY_RESULT, plan.getSteps().get(9).getPhase());
    }

    @Test
    void testPlanStepProgression() {
        StructuredPlan plan = planner.createPlan("t-456", "Add logging", mockRepo);

        StructuredPlanStep current = plan.getCurrentStep();
        assertNotNull(current);
        assertEquals(1, current.getStepNumber());

        plan.markCurrentStepCompleted("Task understood completely");

        // Advanced to step 2
        assertEquals(2, plan.getCurrentStep().getStepNumber());
        assertEquals(AgentStep.StepStatus.COMPLETED, plan.getSteps().get(0).getStatus());
        assertEquals("Task understood completely", plan.getSteps().get(0).getNotes());
    }
}
