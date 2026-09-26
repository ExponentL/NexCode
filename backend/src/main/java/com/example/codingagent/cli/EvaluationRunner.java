package com.example.codingagent.cli;

import com.example.codingagent.agent.AgentLoop;
import com.example.codingagent.agent.AgentState;
import com.example.codingagent.agent.AgentStatus;
import com.example.codingagent.config.AgentProperties;
import com.example.codingagent.logging.AgentEvent;
import com.example.codingagent.logging.AgentEventPublisher;
import com.example.codingagent.model.ModelProvider;
import com.example.codingagent.tools.GitTool;
import com.example.codingagent.tools.ToolResult;
import com.example.codingagent.verification.VerificationCheck;
import com.example.codingagent.verification.VerificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Non-interactive and interactive Evaluation Mode runner for the AI Harness Hackathon 2026.
 *
 * Evaluator flow:
 *   export AI_API_KEY="..."
 *   make setup
 *   make run
 *   Then provide a coding task/issue.
 *
 * Dynamically receives workspace and task, executes real file edits, runs real test commands,
 * verifies evidence on disk, and prints structured evaluation output with exit code 0 on success.
 */
@Component
public class EvaluationRunner implements CommandLineRunner, ExitCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(EvaluationRunner.class);

    private final AgentLoop agentLoop;
    private final AgentProperties properties;
    private final AgentEventPublisher eventPublisher;
    private final ModelProvider modelProvider;
    private final GitTool gitTool;

    private int exitCode = 0;

    public EvaluationRunner(
            AgentLoop agentLoop,
            AgentProperties properties,
            AgentEventPublisher eventPublisher,
            ModelProvider modelProvider,
            GitTool gitTool) {
        this.agentLoop = agentLoop;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.modelProvider = modelProvider;
        this.gitTool = gitTool;
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean isEval = Arrays.asList(args).contains("--eval")
                || "true".equalsIgnoreCase(System.getenv("EVAL_MODE"))
                || "true".equalsIgnoreCase(System.getProperty("eval.mode"));

        if (!isEval) {
            // Normal server / UI mode: let Spring Boot serve Web & SSE endpoints
            return;
        }

        System.out.println("================================================================================");
        System.out.println("🤖  NEXCODE — Autonomous AI Coding Agent (Evaluation Harness)");
        System.out.println("================================================================================");

        // 1. Verify Model Credential (AI_API_KEY)
        String apiKey = properties.getModel().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("\n❌ ERROR: Model credential not found.");
            System.err.println("   Please set the AI_API_KEY environment variable:");
            System.err.println("   export AI_API_KEY=\"your-api-key\"\n");
            this.exitCode = 1;
            System.exit(1);
            return;
        }

        // 2. Resolve Workspace Path dynamically
        String workspaceInput = extractArgument(args, "--workspace");
        if (workspaceInput == null || workspaceInput.isBlank()) {
            workspaceInput = getEnvAny("WORKSPACE_PATH", "WORKSPACE", "EVAL_WORKSPACE");
        }

        Scanner scanner = new Scanner(System.in);
        if (workspaceInput == null || workspaceInput.isBlank()) {
            if (System.console() != null) {
                System.out.print("\n📁 Enter target workspace/repository path [default: .]: ");
                System.out.flush();
            }
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                workspaceInput = line.isBlank() ? "." : line;
            } else {
                workspaceInput = ".";
            }
        }

        Path workspacePath = Paths.get(workspaceInput).toAbsolutePath().normalize();
        File workspaceDir = workspacePath.toFile();
        if (!workspaceDir.exists() || !workspaceDir.isDirectory()) {
            System.err.println("\n❌ ERROR: Workspace path does not exist or is not a directory: " + workspacePath);
            this.exitCode = 1;
            System.exit(1);
            return;
        }

        // 3. Resolve Coding Task dynamically
        String taskInput = extractArgument(args, "--task");
        if (taskInput == null || taskInput.isBlank()) {
            taskInput = getEnvAny("TASK", "TASK_DESCRIPTION", "AI_TASK", "EVAL_TASK");
        }

        if (taskInput == null || taskInput.isBlank()) {
            if (System.console() != null) {
                System.out.println("\n📝 Enter coding task / issue description (submit with Enter):");
                System.out.print("> ");
                System.out.flush();
            }
            StringBuilder taskSb = new StringBuilder();
            if (scanner.hasNextLine()) {
                taskSb.append(scanner.nextLine().trim());
            }
            // If input is piped (non-interactive stdin), read until EOF
            if (System.console() == null) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    taskSb.append("\n").append(line);
                }
            }
            taskInput = taskSb.toString().trim();
        }

        if (taskInput == null || taskInput.isBlank()) {
            System.err.println("\n❌ ERROR: No coding task provided. Evaluation cannot proceed without a task.");
            this.exitCode = 1;
            System.exit(1);
            return;
        }

        System.out.println("\n⚙️  CONFIGURATION:");
        System.out.println("   • Target Workspace: " + workspacePath);
        System.out.println("   • Model Provider:   " + properties.getModel().getProvider());
        System.out.println("   • Model Name:       " + properties.getModel().getName());
        System.out.println("   • Max Attempts:     " + properties.getMaxAttempts());
        System.out.println("\n🎯 TASK:");
        System.out.println("   " + taskInput.replace("\n", "\n   "));
        System.out.println("\n⚡ INITIATING AUTONOMOUS AGENT EXECUTION...\n");

        // 4. Subscribe real-time console logger to agent events
        eventPublisher.addGlobalListener(this::logAgentEvent);

        // 5. Execute Agent Loop on real workspace
        String taskId = "eval-" + System.currentTimeMillis();
        AgentState state = new AgentState(taskId, taskInput, workspacePath.toString(), properties.getMaxAttempts());
        AtomicBoolean cancelFlag = new AtomicBoolean(false);

        long startTime = System.currentTimeMillis();
        agentLoop.run(state, cancelFlag);
        long durationMs = System.currentTimeMillis() - startTime;

        // 6. Display Structured Evaluation Results
        System.out.println("\n================================================================================");
        System.out.println("📊 EVALUATION RESULTS & EVIDENCE SUMMARY");
        System.out.println("================================================================================");
        System.out.println("   • Status:          " + state.getStatus());
        System.out.println("   • Total Time:      " + (durationMs / 1000.0) + "s");
        System.out.println("   • Attempts Made:   " + state.getAttempts() + " of " + state.getMaxAttempts());
        System.out.println("   • Files Modified:  " + (state.getFilesModified().isEmpty() ? "None" : String.join(", ", state.getFilesModified())));

        // Verification Checks
        VerificationResult verification = state.getLatestVerificationResult();
        if (verification != null) {
            System.out.println("\n🔍 VERIFICATION CHECKS:");
            for (VerificationCheck check : verification.checksPerformed()) {
                String mark = check.passed() ? "✅ [PASS]" : "❌ [FAIL]";
                System.out.printf("   %s %-32s : %s\n", mark, check.name(), check.evidence());
            }
        }

        // Git Diff
        ToolResult diffRes = gitTool.execute(Map.of("action", "diff"), workspacePath.toString());
        String diff = (diffRes.success() && diffRes.output() != null && !diffRes.output().isBlank())
                ? diffRes.output()
                : (state.getDiff() != null ? state.getDiff() : "");

        if (!diff.isBlank()) {
            System.out.println("\n📝 REAL GIT DIFF FROM DISK:");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println(diff.trim());
            System.out.println("--------------------------------------------------------------------------------");
        } else {
            System.out.println("\n📝 GIT DIFF: (No uncommitted changes on disk)");
        }

        // Error log if any
        if (!state.getErrors().isEmpty()) {
            System.out.println("\n⚠️  RECORDED ERRORS:");
            for (String err : state.getErrors()) {
                System.out.println("   • " + err);
            }
        }

        // Final Outcome
        System.out.println("\n================================================================================");
        boolean success = state.getStatus() == AgentStatus.COMPLETED;
        if (success) {
            System.out.println("🎉 EVALUATION PASSED: Autonomous task verified with real disk and test evidence!");
            System.out.println("================================================================================");
            this.exitCode = 0;
            System.exit(0);
        } else {
            System.out.println("❌ EVALUATION FAILED: " + (state.getCurrentStep() != null ? state.getCurrentStep() : "Task incomplete"));
            System.out.println("================================================================================");
            this.exitCode = 1;
            System.exit(1);
        }
    }

    private void logAgentEvent(AgentEvent event) {
        String msg = event.message();
        switch (event.type()) {
            case REPOSITORY_SCANNED -> System.out.println("  [1/6 🔍 INSPECT]  " + msg);
            case PLAN_CREATED -> System.out.println("  [2/6 📋 PLAN]     " + msg);
            case MODEL_REQUEST -> System.out.println("  [3/6 🧠 REASON]   " + msg);
            case FILE_READ -> System.out.println("  [📂 READ]         " + msg);
            case FILE_MODIFIED -> System.out.println("  [✏️  MODIFY]       " + msg);
            case COMMAND_EXECUTED -> System.out.println("  [⚙️  EXECUTE]      " + msg);
            case TEST_STARTED -> System.out.println("  [4/6 🧪 TEST]     " + msg);
            case TEST_PASSED -> System.out.println("  [5/6 ✅ PASSED]   " + msg);
            case TEST_FAILED -> System.out.println("  [5/6 ❌ FAILED]   " + msg);
            case RECOVERY_STARTED -> System.out.println("  [🔄 RECOVER]      " + msg);
            case VERIFICATION_STARTED -> System.out.println("  [6/6 🛡️ VERIFY]    " + msg);
            case ERROR -> System.out.println("  [⚠️  ERROR]        " + msg);
            default -> {}
        }
    }

    private String extractArgument(String[] args, String prefix) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith(prefix + "=")) {
                return args[i].substring((prefix + "=").length()).trim();
            }
            if (args[i].equals(prefix) && i + 1 < args.length) {
                return args[i + 1].trim();
            }
        }
        return null;
    }

    private String getEnvAny(String... names) {
        for (String name : names) {
            String val = System.getenv(name);
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        return null;
    }
}
