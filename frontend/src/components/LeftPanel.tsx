import React, { useState } from 'react';
import { RepositoryInfo } from '../types/agent';

interface Props {
  repositories: RepositoryInfo[];
  selectedRepo: string;
  onSelectRepo: (path: string) => void;
  onRefreshRepos: () => void;
  onSubmitTask: (task: string) => void;
  onCancelTask: () => void;
  isRunning: boolean;
  disabled?: boolean;
}

export const LeftPanel: React.FC<Props> = ({
  repositories,
  selectedRepo,
  onSelectRepo,
  onRefreshRepos,
  onSubmitTask,
  onCancelTask,
  isRunning,
  disabled,
}) => {
  const [taskText, setTaskText] = useState('');

  const PRESETS = [
    {
      title: 'Fix Authentication & Test Suite',
      prompt: 'Locate user authentication and validation logic, identify test failures in the suite, fix the implementation, and verify tests pass.',
    },
    {
      title: 'Implement Health & Metrics Endpoint',
      prompt: 'Implement a new /api/health endpoint reporting system uptime, status, and memory stats. Add unit tests and verify build.',
    },
    {
      title: 'Fix Input Validation Vulnerability',
      prompt: 'Inspect request parsing for boundary and null validation issues. Add guards and verify all existing and new unit tests pass.',
    },
    {
      title: 'Refactor Service & Verify Clean Diff',
      prompt: 'Refactor the data access layer to remove deprecated calls, ensure clean compilation, and verify no regressions in the test runner.',
    },
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!taskText.trim() || isRunning) return;
    onSubmitTask(taskText.trim());
  };

  const selectedRepoObj = repositories.find((r) => r.path === selectedRepo);

  return (
    <aside style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
      {/* 1. Repository Selection Card */}
      <div className="card" style={{ padding: '14px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="2">
              <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
            </svg>
            <span className="panel-title">Repository Selection</span>
          </div>
          <button
            onClick={onRefreshRepos}
            disabled={disabled || isRunning}
            title="Scan workspace for repositories"
            style={{
              background: 'transparent',
              border: 'none',
              color: '#38bdf8',
              cursor: disabled || isRunning ? 'not-allowed' : 'pointer',
              fontSize: '11px',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              padding: '2px 6px',
              borderRadius: '4px',
            }}
          >
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
            </svg>
            Rescan
          </button>
        </div>

        <select
          value={selectedRepo}
          onChange={(e) => onSelectRepo(e.target.value)}
          disabled={disabled || isRunning}
          style={{
            width: '100%',
            padding: '9px 12px',
            backgroundColor: '#0f172a',
            border: '1px solid #334155',
            borderRadius: '6px',
            color: '#f8fafc',
            fontFamily: 'monospace',
            fontSize: '12px',
            cursor: disabled || isRunning ? 'not-allowed' : 'pointer',
            outline: 'none',
          }}
        >
          {repositories.map((repo) => (
            <option key={repo.path} value={repo.path}>
              {repo.name} ({repo.type})
            </option>
          ))}
          {repositories.length === 0 && <option value="">No repositories found</option>}
        </select>

        {selectedRepoObj && (
          <div
            style={{
              padding: '8px 10px',
              backgroundColor: '#090e1a',
              borderRadius: '4px',
              border: '1px solid #1e293b',
              fontSize: '11px',
              color: '#94a3b8',
              display: 'flex',
              flexDirection: 'column',
              gap: '4px',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>Project Type:</span>
              <strong style={{ color: '#38bdf8' }}>{selectedRepoObj.type}</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', overflow: 'hidden' }}>
              <span>Path:</span>
              <span
                style={{
                  fontFamily: 'monospace',
                  color: '#cbd5e1',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  maxWidth: '190px',
                }}
                title={selectedRepoObj.path}
              >
                {selectedRepoObj.path}
              </span>
            </div>
          </div>
        )}
      </div>

      {/* 2. Task Input & Controls Card */}
      <div className="card" style={{ padding: '14px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="2">
              <polygon points="12 2 2 7 12 12 22 7 12 2" />
              <polyline points="2 17 12 22 22 17" />
              <polyline points="2 12 12 17 22 12" />
            </svg>
            <span className="panel-title">Task Specification</span>
          </div>
          <span style={{ fontSize: '10px', color: '#64748b' }}>{taskText.length} chars</span>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <textarea
            rows={5}
            value={taskText}
            onChange={(e) => setTaskText(e.target.value)}
            disabled={isRunning || disabled}
            placeholder="Describe the software engineering task (e.g. 'Fix login validation tests in UserAuthTest and verify clean build')..."
            style={{
              width: '100%',
              padding: '10px 12px',
              backgroundColor: '#0f172a',
              border: '1px solid #334155',
              borderRadius: '6px',
              color: '#f8fafc',
              fontSize: '12px',
              lineHeight: '1.5',
              resize: 'vertical',
              outline: 'none',
              boxSizing: 'border-box',
              fontFamily: 'inherit',
            }}
          />

          {/* Quick Presets */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <span style={{ fontSize: '10px', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>
              Engineering Presets:
            </span>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
              {PRESETS.map((preset, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setTaskText(preset.prompt)}
                  disabled={isRunning || disabled}
                  style={{
                    fontSize: '11px',
                    backgroundColor: '#0f172a',
                    color: '#94a3b8',
                    border: '1px solid #1e293b',
                    borderRadius: '4px',
                    padding: '5px 8px',
                    cursor: isRunning || disabled ? 'not-allowed' : 'pointer',
                    textAlign: 'left',
                    transition: 'border-color 0.15s ease',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.borderColor = '#38bdf8')}
                  onMouseLeave={(e) => (e.currentTarget.style.borderColor = '#1e293b')}
                >
                  <strong style={{ color: '#cbd5e1' }}>{preset.title}</strong>
                </button>
              ))}
            </div>
          </div>

          {/* Start / Cancel Action Buttons */}
          <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
            {isRunning ? (
              <button
                type="button"
                onClick={onCancelTask}
                style={{
                  flex: 1,
                  backgroundColor: '#dc2626',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '6px',
                  padding: '10px 14px',
                  fontSize: '13px',
                  fontWeight: 700,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  boxShadow: '0 0 14px rgba(220, 38, 38, 0.4)',
                }}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                </svg>
                Cancel Task
              </button>
            ) : (
              <button
                type="submit"
                disabled={!taskText.trim() || disabled}
                style={{
                  flex: 1,
                  backgroundColor: !taskText.trim() || disabled ? '#1e293b' : '#2563eb',
                  color: !taskText.trim() || disabled ? '#64748b' : '#ffffff',
                  border: 'none',
                  borderRadius: '6px',
                  padding: '10px 14px',
                  fontSize: '13px',
                  fontWeight: 700,
                  cursor: !taskText.trim() || disabled ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  boxShadow: !taskText.trim() || disabled ? 'none' : '0 0 16px rgba(37, 99, 235, 0.4)',
                }}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <polygon points="5 3 19 12 5 21 5 3" />
                </svg>
                Start Task
              </button>
            )}
          </div>
        </form>
      </div>

      {/* 3. Harness Architecture Spec Card */}
      <div
        className="card"
        style={{
          padding: '12px 14px',
          display: 'flex',
          flexDirection: 'column',
          gap: '8px',
          fontSize: '11px',
          color: '#64748b',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', fontSize: '10px' }}>
            Harness Environment
          </span>
          <span style={{ color: '#22c55e', fontSize: '10px' }}>● Connected</span>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
          <div>Backend: <span style={{ color: '#cbd5e1' }}>Java 21 · Spring Boot</span></div>
          <div>Tools: <span style={{ color: '#cbd5e1' }}>Java NIO · Real ProcessBuilder</span></div>
          <div>Tests: <span style={{ color: '#cbd5e1' }}>Real TestRunner (Zero Mocking)</span></div>
          <div>Safety: <span style={{ color: '#cbd5e1' }}>Workspace Sandbox Guard</span></div>
        </div>
      </div>
    </aside>
  );
};
