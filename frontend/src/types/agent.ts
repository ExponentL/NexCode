export type AgentStatus =
  | 'IDLE'
  | 'ANALYZING'
  | 'PLANNING'
  | 'EXECUTING'
  | 'TESTING'
  | 'RECOVERING'
  | 'VERIFYING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';

export type StepStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'SKIPPED';

export interface PlanStep {
  stepNumber: number;
  title: string;
  description: string;
  action?: string;
  status: StepStatus;
  resultSummary?: string;
}

export interface TestResultItem {
  suiteName: string;
  testName: string;
  passed: boolean;
  errorTrace?: string;
  durationMs: number;
}

export interface TestFailureDetail {
  suiteName: string;
  testName: string;
  message: string;
  errorTrace?: string;
}

export interface RealTestResult {
  command: string;
  status: 'PASSED' | 'FAILED' | 'ERROR' | 'NO_TESTS';
  totalTests?: number | null;
  passedTests?: number | null;
  failedTests?: number | null;
  skippedTests?: number | null;
  durationMs: number;
  stdout: string;
  stderr: string;
  exitCode: number;
  output: string;
  failures: TestFailureDetail[];
  hasParsedMetrics: boolean;
  projectType: string;
}

export interface VerificationCheck {
  name: string;
  passed: boolean;
  evidence: string;
}

export interface GitDiffSummary {
  modifiedFiles: string[];
  addedFiles: string[];
  deletedFiles: string[];
  diff: string;
  totalChanges: number;
}

export interface BuildResult {
  success: boolean;
  command: string;
  exitCode: number;
  output: string;
  durationMs: number;
  message: string;
}

export interface VerificationResult {
  verified: boolean;
  summary: string;
  checksPerformed: VerificationCheck[];
  checksPassed: string[];
  checksFailed: string[];
  testResult?: RealTestResult;
  buildResult?: BuildResult;
  gitDiff?: GitDiffSummary;
  remainingErrors: string[];
  testDetails: TestResultItem[];
}

export interface TaskResult {
  taskId: string;
  task: string;
  status: AgentStatus;
  verified: boolean;
  summary: string;
  verificationResult: VerificationResult;
  filesInspected: string[];
  filesModified: string[];
  plan: PlanStep[];
  gitDiffSummary?: GitDiffSummary;
  durationMs: number;
  createdAt: string;
  completedAt: string;
}

export interface AgentState {
  taskId: string;
  task: string;
  repositoryPath: string;
  status: AgentStatus;
  currentStep: string;
  attempts: number;
  maxAttempts: number;
  plan: PlanStep[];
  filesInspected: string[];
  filesModified: string[];
  commandsExecuted: string[];
  errors: string[];
  testResults: TestResultItem[];
  latestTestResult?: RealTestResult;
  latestVerificationResult?: VerificationResult;
  finalTaskResult?: TaskResult;
  diff?: string;
  evidenceSummary?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AgentEvent {
  eventId: string;
  taskId: string;
  type: string;
  message: string;
  details: Record<string, any>;
  timestamp: string;
}

export interface RepositoryInfo {
  name: string;
  path: string;
  type: string;
}

export interface CreateTaskPayload {
  task: string;
  repositoryPath?: string;
}

export interface RepositoryAnalysis {
  name: string;
  path: string;
  exists: boolean;
  isGit: boolean;
  gitBranch: string | null;
  gitStatus: string;
  projectType: string;
  detectedLanguages: string[];
  sourceDirectories: string[];
  testDirectories: string[];
  buildSystem: string;
  availableCommands: string[];
  totalFiles: number;
  readmeSummary: string | null;
  errorMessage: string | null;
}

export interface WorkspaceSelectionResult {
  path: string | null;
  name: string | null;
  exists: boolean;
  readable: boolean;
  cancelled: boolean;
  message: string;
  analysis: RepositoryAnalysis | null;
}
