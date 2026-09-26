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

## Evaluation Input Format

Nexcode supports both interactive and fully automated non-interactive evaluation modes:

### 1. Interactive Evaluation Mode (Default)
When you run:
```bash
make run
```
Nexcode prompts for:
1. **Target workspace/repository path**: Path to the repository under test (press Enter to default to current directory `.`).
2. **Task / Issue description**: The problem or requirement statement.

### 2. Make Parameters
Pass parameters directly to `make run`:
```bash
make run WORKSPACE="/path/to/target-repo" TASK="Fix the divide-by-zero bug in calculator.py"
```

### 3. CLI Arguments
Run the evaluation script with direct flags:
```bash
./run-evaluation.sh --workspace="/path/to/target-repo" --task="Fix the divide-by-zero bug in calculator.py"
```

### 4. Environment Variables
Export configuration variables before running:
```bash
export WORKSPACE_PATH="/path/to/target-repo"
export TASK="Fix the divide-by-zero bug in calculator.py"
make run
```

### 5. Piped / Non-Interactive STDIN
Pipe a task or issue description directly via standard input:
```bash
make run WORKSPACE="/path/to/target-repo" < issue_description.txt
```
or:
```bash
echo "Implement error handling for negative roots in math_utils.py" | make run WORKSPACE="/path/to/target-repo"
```

---

## Evaluation Flow

When `make run` executes, Nexcode autonomously performs:

1. **Workspace Inspection**: Dynamically analyzes project structure, detects language, framework, build tools (Maven, Gradle, npm, Python pytest/unittest, Make, Cargo, Go), and Git status without assumptions.
2. **Context & Planning**: Identifies task-relevant files and synthesizes a step-by-step modification plan.
3. **Real File Modification**: Validates target paths (preventing path traversal outside the workspace) and writes actual changes directly to disk using Java NIO.
4. **Real Command & Test Execution**: Executes the repository's native test commands from the workspace directory, capturing exit codes, stdout, and stderr.
5. **Failure Analysis & Recovery**: If tests fail, parses errors and compiler messages to plan and apply targeted fixes across recovery cycles.
6. **Evidence Verification**: Re-reads modified files from disk, runs Git diff, verifies passing test suites, and outputs structured evaluation metrics.
7. **Exit Code**: Exits with code `0` on successful verification or code `1` if verification fails.

---

## Optional: Web Dashboard Mode

For local interactive use, the existing web UI remains fully operational:

```bash
make ui
```
Opens the interactive React + TypeScript dashboard at [http://localhost:5173](http://localhost:5173) with live SSE event streaming.
