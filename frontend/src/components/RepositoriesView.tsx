import React, { useState } from 'react';
import { RepositoryAnalysis, RepositoryInfo } from '../types/agent';

interface Props {
  repositories: RepositoryInfo[];
  selectedRepo: string;
  repoAnalysis: RepositoryAnalysis | null;
  onSelectRepo: (path: string) => void;
  onSelectDirectory: () => void;
  selectingDirectory: boolean;
  onCloneGitHub: (url: string) => Promise<any>;
  cloning: boolean;
  cloneError: string | null;
  selectionMessage: string | null;
  onNavigateToDashboard: () => void;
}

export const RepositoriesView: React.FC<Props> = ({
  repositories,
  selectedRepo,
  repoAnalysis,
  onSelectRepo,
  onSelectDirectory,
  selectingDirectory,
  onCloneGitHub,
  cloning,
  cloneError,
  selectionMessage,
  onNavigateToDashboard,
}) => {
  const [gitUrl, setGitUrl] = useState('');
  const [cloneSuccess, setCloneSuccess] = useState<string | null>(null);

  const handleCloneSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!gitUrl.trim() || cloning) return;
    setCloneSuccess(null);
    const res = await onCloneGitHub(gitUrl.trim());
    if (res && res.exists) {
      setCloneSuccess(
        `Successfully cloned and detected: ${res.name || res.path} (${res.analysis?.projectType || 'Project'})`
      );
      setGitUrl('');
    }
  };

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '20px',
        maxWidth: '1200px',
        margin: '0 auto',
        width: '100%',
        paddingBottom: '40px',
      }}
    >
      {/* Top Banner */}
      <div
        className="code-agent-card"
        style={{
          padding: '20px 24px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: 'linear-gradient(180deg, #0e1626 0%, #080c14 100%)',
          border: '1px solid #1a263d',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '8px',
                backgroundColor: '#1d4ed8',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#ffffff',
              }}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
              </svg>
            </div>
            <div>
              <h1 style={{ margin: 0, fontSize: '18px', fontWeight: 700, color: '#f8fafc' }}>
                Repositories & Workspaces
              </h1>
              <p style={{ margin: '4px 0 0 0', fontSize: '13px', color: '#64748b' }}>
                Select local projects or clone remote GitHub repositories. The agent inspects codebases, identifies test suites, and implements autonomous modifications.
              </p>
            </div>
          </div>
        </div>

        {selectedRepo && (
          <button
            onClick={onNavigateToDashboard}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '10px 18px',
              backgroundColor: '#2563eb',
              color: '#ffffff',
              border: 'none',
              borderRadius: '6px',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
              boxShadow: '0 0 16px rgba(37, 99, 235, 0.4)',
              transition: 'all 0.15s ease',
            }}
          >
            <span>🚀 Open in Dashboard</span>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M5 12h14M12 5l7 7-7 7" />
            </svg>
          </button>
        )}
      </div>

      {/* Grid: GitHub Clone & Local Selection */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(480px, 1fr))',
          gap: '20px',
        }}
      >
        {/* Card 1: Remote GitHub Clone */}
        <div
          className="code-agent-card"
          style={{
            padding: '20px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="#38bdf8">
              <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
            </svg>
            <div>
              <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600, color: '#f8fafc' }}>
                Clone from GitHub
              </h2>
              <span style={{ fontSize: '12px', color: '#64748b' }}>
                Fetch any remote GitHub repository directly to your environment
              </span>
            </div>
          </div>

          <p style={{ margin: 0, fontSize: '12px', color: '#94a3b8', lineHeight: '1.5' }}>
            Enter a GitHub repository URL. The agent will clone the repository, index its directory structure, detect its build system, and prepare it for autonomous tasks.
          </p>

          <form onSubmit={handleCloneSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            <div style={{ display: 'flex', gap: '10px' }}>
              <input
                type="text"
                value={gitUrl}
                onChange={(e) => setGitUrl(e.target.value)}
                placeholder="https://github.com/owner/repository"
                disabled={cloning}
                style={{
                  flex: 1,
                  padding: '10px 14px',
                  backgroundColor: '#070b14',
                  border: '1px solid #1e293b',
                  borderRadius: '6px',
                  color: '#f8fafc',
                  fontSize: '13px',
                  outline: 'none',
                  fontFamily: 'monospace',
                }}
              />
              <button
                type="submit"
                disabled={!gitUrl.trim() || cloning}
                style={{
                  padding: '10px 18px',
                  backgroundColor: !gitUrl.trim() || cloning ? '#1e293b' : '#1d4ed8',
                  color: !gitUrl.trim() || cloning ? '#64748b' : '#ffffff',
                  border: '1px solid #3b82f6',
                  borderRadius: '6px',
                  fontSize: '13px',
                  fontWeight: 600,
                  cursor: !gitUrl.trim() || cloning ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  whiteSpace: 'nowrap',
                }}
              >
                {cloning ? (
                  <>
                    <svg
                      width="14"
                      height="14"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2.5"
                      style={{ animation: 'spin 1s linear infinite' }}
                    >
                      <circle cx="12" cy="12" r="10" strokeOpacity="0.25" />
                      <path d="M12 2a10 10 0 0 1 10 10" />
                    </svg>
                    <span>Cloning & Detecting...</span>
                  </>
                ) : (
                  <>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3" />
                    </svg>
                    <span>Clone & Detect</span>
                  </>
                )}
              </button>
            </div>
          </form>

          {cloneError && (
            <div
              style={{
                padding: '10px 14px',
                backgroundColor: 'rgba(239, 68, 68, 0.1)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                borderRadius: '6px',
                fontSize: '12px',
                color: '#f87171',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              <span>⚠</span>
              <span>{cloneError}</span>
            </div>
          )}

          {cloneSuccess && (
            <div
              style={{
                padding: '12px 16px',
                backgroundColor: 'rgba(16, 185, 129, 0.1)',
                border: '1px solid rgba(16, 185, 129, 0.3)',
                borderRadius: '6px',
                display: 'flex',
                flexDirection: 'column',
                gap: '8px',
              }}
            >
              <div style={{ fontSize: '12px', color: '#34d399', fontWeight: 600 }}>
                ✓ {cloneSuccess}
              </div>
              <button
                onClick={onNavigateToDashboard}
                style={{
                  alignSelf: 'flex-start',
                  padding: '6px 14px',
                  backgroundColor: '#059669',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '5px',
                  fontSize: '12px',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                Go to Dashboard & Start Task →
              </button>
            </div>
          )}
        </div>

        {/* Card 2: Local Filesystem Directory Selection */}
        <div
          className="code-agent-card"
          style={{
            padding: '20px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#60a5fa" strokeWidth="2">
              <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
            </svg>
            <div>
              <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600, color: '#f8fafc' }}>
                Local Project Directory
              </h2>
              <span style={{ fontSize: '12px', color: '#64748b' }}>
                Select any directory from your local operating system
              </span>
            </div>
          </div>

          <p style={{ margin: 0, fontSize: '12px', color: '#94a3b8', lineHeight: '1.5' }}>
            Open your operating system's native folder picker to select any codebase on your computer (Documents, Downloads, Desktop, or custom folder).
          </p>

          <button
            onClick={onSelectDirectory}
            disabled={selectingDirectory}
            style={{
              padding: '12px 18px',
              backgroundColor: '#1d4ed8',
              color: '#ffffff',
              border: '1px solid #3b82f6',
              borderRadius: '6px',
              fontSize: '13px',
              fontWeight: 600,
              cursor: selectingDirectory ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              boxShadow: '0 0 14px rgba(29, 78, 216, 0.3)',
            }}
          >
            {selectingDirectory ? (
              <>
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                  style={{ animation: 'spin 1s linear infinite' }}
                >
                  <circle cx="12" cy="12" r="10" strokeOpacity="0.25" />
                  <path d="M12 2a10 10 0 0 1 10 10" />
                </svg>
                <span>Opening System Picker...</span>
              </>
            ) : (
              <>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
                </svg>
                <span>Select Local Directory</span>
              </>
            )}
          </button>

          {selectionMessage && (
            <div style={{ fontSize: '12px', color: '#38bdf8' }}>
              ℹ {selectionMessage}
            </div>
          )}
        </div>
      </div>

      {/* Discovered Repositories List */}
      <div
        className="code-agent-card"
        style={{
          padding: '20px',
          display: 'flex',
          flexDirection: 'column',
          gap: '14px',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600, color: '#f8fafc' }}>
            Detected Repositories ({repositories.length})
          </h2>
          <span style={{ fontSize: '12px', color: '#64748b' }}>
            Click any repository to activate and view inspection
          </span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {repositories.map((repo) => {
            const isActive = selectedRepo === repo.path;
            return (
              <div
                key={repo.path}
                onClick={() => onSelectRepo(repo.path)}
                style={{
                  padding: '12px 16px',
                  backgroundColor: isActive ? '#0e172a' : '#070b14',
                  border: `1px solid ${isActive ? '#2563eb' : '#192237'}`,
                  borderRadius: '8px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <div
                    style={{
                      width: '8px',
                      height: '8px',
                      borderRadius: '50%',
                      backgroundColor: isActive ? '#10b981' : '#334155',
                    }}
                  />
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontSize: '13px', fontWeight: 600, color: '#f8fafc' }}>
                        {repo.name}
                      </span>
                      <span
                        style={{
                          fontSize: '11px',
                          color: '#38bdf8',
                          backgroundColor: 'rgba(56, 189, 248, 0.1)',
                          padding: '1px 6px',
                          borderRadius: '4px',
                        }}
                      >
                        {repo.type}
                      </span>
                      {isActive && (
                        <span
                          style={{
                            fontSize: '11px',
                            color: '#34d399',
                            backgroundColor: 'rgba(16, 185, 129, 0.1)',
                            padding: '1px 6px',
                            borderRadius: '4px',
                          }}
                        >
                          Active
                        </span>
                      )}
                    </div>
                    <div
                      style={{
                        fontSize: '11px',
                        color: '#64748b',
                        fontFamily: 'monospace',
                        marginTop: '3px',
                      }}
                    >
                      {repo.path}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelectRepo(repo.path);
                      onNavigateToDashboard();
                    }}
                    style={{
                      padding: '6px 12px',
                      backgroundColor: isActive ? '#2563eb' : '#1e293b',
                      color: '#ffffff',
                      border: 'none',
                      borderRadius: '5px',
                      fontSize: '12px',
                      fontWeight: 500,
                      cursor: 'pointer',
                    }}
                  >
                    Open in Dashboard →
                  </button>
                </div>
              </div>
            );
          })}

          {repositories.length === 0 && (
            <div style={{ padding: '24px', textAlign: 'center', color: '#64748b', fontSize: '13px' }}>
              No repositories detected yet. Select a local directory or clone a GitHub repository above.
            </div>
          )}
        </div>
      </div>

      {/* Active Repository Analysis Preview */}
      {repoAnalysis && (
        <div
          className="code-agent-card"
          style={{
            padding: '20px',
            display: 'flex',
            flexDirection: 'column',
            gap: '14px',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600, color: '#f8fafc' }}>
              Active Workspace Inspection: {repoAnalysis.name}
            </h2>
            <button
              onClick={onNavigateToDashboard}
              style={{
                padding: '7px 14px',
                backgroundColor: '#2563eb',
                color: '#ffffff',
                border: 'none',
                borderRadius: '6px',
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Start Task on this Repo →
            </button>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
              gap: '12px',
            }}
          >
            <div style={{ padding: '12px', backgroundColor: '#070b14', borderRadius: '6px', border: '1px solid #192237' }}>
              <div style={{ fontSize: '11px', color: '#64748b' }}>Project Type</div>
              <div style={{ fontSize: '14px', fontWeight: 600, color: '#38bdf8', marginTop: '2px' }}>
                {repoAnalysis.projectType}
              </div>
            </div>

            <div style={{ padding: '12px', backgroundColor: '#070b14', borderRadius: '6px', border: '1px solid #192237' }}>
              <div style={{ fontSize: '11px', color: '#64748b' }}>Build System</div>
              <div style={{ fontSize: '14px', fontWeight: 600, color: '#f8fafc', marginTop: '2px' }}>
                {repoAnalysis.buildSystem}
              </div>
            </div>

            <div style={{ padding: '12px', backgroundColor: '#070b14', borderRadius: '6px', border: '1px solid #192237' }}>
              <div style={{ fontSize: '11px', color: '#64748b' }}>Languages Detected</div>
              <div style={{ fontSize: '14px', fontWeight: 600, color: '#f8fafc', marginTop: '2px' }}>
                {repoAnalysis.detectedLanguages.join(', ') || 'None detected'}
              </div>
            </div>

            <div style={{ padding: '12px', backgroundColor: '#070b14', borderRadius: '6px', border: '1px solid #192237' }}>
              <div style={{ fontSize: '11px', color: '#64748b' }}>Total Files</div>
              <div style={{ fontSize: '14px', fontWeight: 600, color: '#f8fafc', marginTop: '2px' }}>
                {repoAnalysis.totalFiles}
              </div>
            </div>

            <div style={{ padding: '12px', backgroundColor: '#070b14', borderRadius: '6px', border: '1px solid #192237' }}>
              <div style={{ fontSize: '11px', color: '#64748b' }}>Git Branch / Status</div>
              <div style={{ fontSize: '13px', fontWeight: 500, color: repoAnalysis.isGit ? '#38bdf8' : '#64748b', marginTop: '2px' }}>
                {repoAnalysis.isGit ? `${repoAnalysis.gitBranch || 'detached'} • ${repoAnalysis.gitStatus}` : 'Not a Git repo'}
              </div>
            </div>

            <div style={{ padding: '12px', backgroundColor: '#070b14', borderRadius: '6px', border: '1px solid #192237' }}>
              <div style={{ fontSize: '11px', color: '#64748b' }}>Available Commands</div>
              <div style={{ fontSize: '12px', fontFamily: 'monospace', color: '#cbd5e1', marginTop: '2px' }}>
                {repoAnalysis.availableCommands.join(', ') || 'None detected'}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default RepositoriesView;
