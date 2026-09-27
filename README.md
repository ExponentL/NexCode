# Nexcode — Autonomous AI Coding Agent Harness

**Nexcode** is an autonomous AI software engineering agent harness built for the **AI Harness Hackathon 2026**.

The harness dynamically receives any real repository and coding task, inspects the codebase, identifies relevant files, applies real code modifications using Java NIO, detects and executes real test/build systems, iteratively recovers from failures, validates changes using real evidence on disk and Git diffs, and returns a verified result.

---

## Required Setup & Execution

```bash
export AI_API_KEY="..."
make setup
make run
make test
```

### Clean Artifacts
```bash
make clean
```

---

## Evaluation Flow

When you run:
```bash
make setup
make run
```

1. **Dual Service Startup**: `make run` launches both the Spring Boot backend (`http://localhost:8080`) and the React + TypeScript frontend (`http://localhost:5173`), prints the URLs in the console, and automatically opens your browser to the web dashboard.
2. **Dynamic Repository Selection**: In the browser UI (under the **Repositories** tab), choose or provide any target repository on your system, or paste any remote GitHub repository URL to clone and detect it. The agent never assumes a fixed repository or project type.
3. **Task Submission**: On the Dashboard, provide the coding task or issue description and run the agent.
4. **Autonomous Agent Execution**:
   - **Inspects**: Dynamically inspects the workspace, detecting languages, frameworks, build systems, and test frameworks without assumptions.
   - **Modifies Real Files**: Plans and writes actual file modifications directly to disk using Java NIO, validating paths within the workspace.
   - **Re-Reads & Verifies**: Re-reads modified files directly from disk and computes live Git diffs.
   - **Runs Real Tests**: Executes native workspace test commands, capturing real terminal exit codes, stdout, and stderr.
   - **Iterative Recovery**: Analyzes failures, plans targeted fixes, and re-executes tests across recovery cycles.
   - **Evidence Verification**: Verifies passing test results and code changes, reporting structured verification results.

---

## Non-Interactive CLI Evaluation Mode

For headless evaluation or CI/CD pipelines, non-interactive evaluation mode is also supported:

### 1. Make Target
```bash
make eval WORKSPACE="/path/to/target-repo" TASK="Fix the divide-by-zero bug in calculator.py"
```

### 2. Direct Script
```bash
./run-evaluation.sh --workspace="/path/to/target-repo" --task="Fix the divide-by-zero bug in calculator.py"
```

### 3. Piped Input
```bash
echo "Fix the negative root bug in math_utils.py" | make eval WORKSPACE="/path/to/target-repo"
```
