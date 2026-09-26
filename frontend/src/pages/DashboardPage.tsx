import React, { useEffect, useState } from 'react';
import { useRepositories } from '../hooks/useRepositories';
import { useAgentTask } from '../hooks/useAgentTask';
import { agentApi } from '../services/api';
import { AgentState } from '../types/agent';
import { CodeAgentHeader } from '../components/CodeAgentHeader';
import { CodeAgentSidebar } from '../components/CodeAgentSidebar';
import { CreateTaskCard } from '../components/CreateTaskCard';
import { RecentTasksCard } from '../components/RecentTasksCard';
import { AgentActivityCard } from '../components/AgentActivityCard';
import { AgentStatusCard } from '../components/AgentStatusCard';
import { RepositoryInfoCard } from '../components/RepositoryInfoCard';
import { FilesChangedCard } from '../components/FilesChangedCard';
import { TestResultsCard } from '../components/TestResultsCard';
import { FinalVerificationCard } from '../components/FinalVerificationCard';
import { TaskLogsCard } from '../components/TaskLogsCard';
import { DiffModal } from '../components/DiffModal';
import { RepositoriesView } from '../components/RepositoriesView';

export const DashboardPage: React.FC = () => {
  const [navTab, setNavTab] = useState<'Dashboard' | 'Repositories'>('Dashboard');
  const [recentTasks, setRecentTasks] = useState<AgentState[]>([]);
  const [showDiffModal, setShowDiffModal] = useState(false);

  const {
    repositories,
    selectedRepo,
    setSelectedRepo,
    repoAnalysis,
    analyzeRepo,
    selectDirectory,
    selectingDirectory,
    selectionMessage,
    cloneGitHub,
    cloning,
    cloneError,
    error: repoError,
    analyzing,
  } = useRepositories();

  const {
    activeTask,
    events,
    diff,
    verificationResult,
    loading,
    startTask,
    refreshTaskState,
  } = useAgentTask();

  // Load real task history from backend
  useEffect(() => {
    agentApi
      .getTasks()
      .then((ts) => setRecentTasks(ts))
      .catch(() => {});
  }, [activeTask]);

  const isRunning =
    activeTask !== null &&
    activeTask.status !== 'COMPLETED' &&
    activeTask.status !== 'FAILED' &&
    activeTask.status !== 'CANCELLED';

  const handleRunAgent = async (taskText: string) => {
    try {
      await startTask({
        task: taskText,
        repositoryPath: selectedRepo,
      });
    } catch (e: any) {
      console.error('Failed to run agent:', e);
    }
  };

  const handleSelectRecentTask = (taskId: string) => {
    if (taskId.startsWith('mock-')) return;
    refreshTaskState(taskId);
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        backgroundColor: '#080c14',
        color: '#f8fafc',
        display: 'flex',
        flexDirection: 'column',
        fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif",
      }}
    >
      {/* 1. TOP HEADER */}
      <CodeAgentHeader />

      {/* 2. BODY: SIDEBAR + MAIN WORKSPACE */}
      <div style={{ display: 'flex', flex: 1, minHeight: 'calc(100vh - 56px)' }}>
        {/* Left Sidebar */}
        <CodeAgentSidebar activeTab={navTab} onTabChange={setNavTab} />

        {/* Main Content Workspace */}
        <main
          style={{
            flex: 1,
            padding: '18px 20px',
            backgroundColor: '#080c14',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
            overflowY: 'auto',
            boxSizing: 'border-box',
          }}
        >
          {navTab === 'Repositories' ? (
            <RepositoriesView
              repositories={repositories}
              selectedRepo={selectedRepo}
              repoAnalysis={repoAnalysis}
              onSelectRepo={setSelectedRepo}
              onSelectDirectory={selectDirectory}
              selectingDirectory={selectingDirectory}
              onCloneGitHub={cloneGitHub}
              cloning={cloning}
              cloneError={cloneError}
              selectionMessage={selectionMessage}
              onNavigateToDashboard={() => setNavTab('Dashboard')}
            />
          ) : (
            <>
              {/* Top 3-Columns Grid */}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'minmax(380px, 1.25fr) minmax(320px, 1.15fr) minmax(300px, 1fr)',
                  gap: '16px',
                  alignItems: 'start',
                }}
              >
                {/* COLUMN 1: Repository Selection, Task Input & Real Task History */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                  <CreateTaskCard
                    repositories={repositories}
                    selectedRepo={selectedRepo}
                    repoAnalysis={repoAnalysis}
                    onSelectRepo={setSelectedRepo}
                    onAnalyzePath={analyzeRepo}
                    onSubmitTask={handleRunAgent}
                    onSelectDirectory={selectDirectory}
                    selectingDirectory={selectingDirectory}
                    selectionMessage={selectionMessage}
                    error={repoError}
                    isRunning={isRunning}
                    disabled={loading}
                  />

                  <RecentTasksCard
                    tasks={recentTasks}
                    activeTaskId={activeTask?.taskId}
                    onSelectTask={handleSelectRecentTask}
                  />
                </div>

                {/* COLUMN 2: Real Agent Activity Stream */}
                <div style={{ height: '100%' }}>
                  <AgentActivityCard events={events} />
                </div>

                {/* COLUMN 3: Right Column Stats, Real Analysis & Evidence Verification */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                  <AgentStatusCard state={activeTask} />

                  <RepositoryInfoCard
                    analysis={repoAnalysis}
                    loading={analyzing}
                  />

                  <FilesChangedCard
                    filesModified={activeTask?.filesModified || []}
                    diff={diff}
                    onViewDiff={() => setShowDiffModal(true)}
                  />

                  <TestResultsCard latestTestResult={activeTask?.latestTestResult} />

                  <FinalVerificationCard
                    verificationResult={verificationResult || activeTask?.latestVerificationResult}
                  />
                </div>
              </div>

              {/* BOTTOM ROW: Live Task Output / Execution Logs */}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'minmax(0, 2.4fr) minmax(300px, 1fr)',
                  gap: '16px',
                }}
              >
                <TaskLogsCard events={events} />
                <div />
              </div>
            </>
          )}
        </main>
      </div>

      {/* Diff Modal */}
      <DiffModal
        isOpen={showDiffModal}
        onClose={() => setShowDiffModal(false)}
        diff={diff}
        filesModified={activeTask?.filesModified || []}
      />
    </div>
  );
};

export default DashboardPage;
