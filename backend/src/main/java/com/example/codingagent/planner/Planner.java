package com.example.codingagent.planner;

import com.example.codingagent.context.RepositoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Planner {

    private static final Logger log = LoggerFactory.getLogger(Planner.class);

    /**
     * Synthesizes a structured, machine-readable execution plan aligned with the standard engineering workflow.
     */
    public StructuredPlan createPlan(String taskId, String taskDescription, RepositoryContext repoContext) {
        log.info("Planner generating structured plan for task: {}", taskId);

        List<StructuredPlanStep> steps = new ArrayList<>();
        int step = 1;

        // Phase 1: Understand task
        steps.add(new StructuredPlanStep(
                step++,
                PlanPhase.UNDERSTAND_TASK,
                "Understand Task Requirements",
                "Analyze user instructions: '" + sanitize(taskDescription) + "' and infer required engineering deliverables."
        ));

        // Phase 2: Locate relevant code
        steps.add(new StructuredPlanStep(
                step++,
                PlanPhase.LOCATE_CODE,
                "Locate Relevant Code",
                "Search codebase filenames and text content to isolate components directly affected by task."
        ));

        // Phase 3: Inspect implementation
        steps.add(new StructuredPlanStep(
                step++,
                PlanPhase.INSPECT_IMPLEMENTATION,
                "Inspect Implementation Files",
                "Read target source files to understand architecture, types, and logic."
        ));

        // Phase 4: Inspect tests
        steps.add(new StructuredPlanStep(
                step++,
                PlanPhase.INSPECT_TESTS,
                "Inspect Test Suites",
                "Locate and inspect relevant automated tests to understand expected behaviors and existing test coverage."
        ));

        // Phase 5: Identify problem
        steps.add(new StructuredPlanStep(
                step++,
                PlanPhase.IDENTIFY_PROBLEM,
                "Identify Defect or Feature Delta",
                "Synthesize findings to pinpoint bug origin or exact specification required for new feature."
        ));

        // Phase 6: Modify code
        steps.add(new StructuredPlanStep(
                step++,
                PlanPhase.MODIFY_CODE,
                "Modify Implementation Code",
                "Apply surgical modifications or file creations to implement requested solution."
        ));

        // Phase 7: Run tests
        steps.add(new StructuredPlanStep(
                step++,
                PlanPhase.RUN_TESTS,
                "Execute Automated Tests",
                "Run the repository's native test suite (" + repoContext.projectType() + ") and capture real outputs."
        ));

        // Phase 8: Analyze failures
        steps.add(new StructuredPlanStep(
                step++,
                PlanPhase.ANALYZE_FAILURES,
                "Analyze Test Failures",
                "Parse test output, error traces, and exit codes to evaluate correctness."
        ));

        // Phase 9: Fix if necessary
        steps.add(new StructuredPlanStep(
                step++,
                PlanPhase.REMEDIATE,
                "Remediate and Self-Repair",
                "If tests or checks fail, diagnose error traces and apply corrective fixes."
        ));

        // Phase 10: Verify result
        steps.add(new StructuredPlanStep(
                step++,
                PlanPhase.VERIFY_RESULT,
                "Verify Final Result",
                "Perform final git diff and test verification check to substantiate complete, verified delivery."
        ));

        return new StructuredPlan(taskId, taskDescription, steps);
    }

    private String sanitize(String text) {
        if (text == null) return "";
        return text.length() > 80 ? text.substring(0, 80) + "..." : text;
    }
}
