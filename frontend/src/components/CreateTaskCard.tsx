import React, { useState } from 'react';
import { RepositoryAnalysis, RepositoryInfo } from '../types/agent';

interface Props {
  repositories: RepositoryInfo[];
  selectedRepo: string;
  repoAnalysis: RepositoryAnalysis | null;
  onSelectRepo: (path: string) => void;
  onAnalyzePath: (path: string) => void;
  onSubmitTask: (task: string) => void;
  onSelectDirectory?: () => void;
  selectingDirectory?: boolean;
  selectionMessage?: string | null;
  error?: string | null;
  isRunning: boolean;
  disabled?: boolean;
}

export const CreateTaskCard: React.FC<Props> = ({
  repositories,
  selectedRepo,
  repoAnalysis,
  onSelectRepo,
  onAnalyzePath,
  onSubmitTask,
  onSelectDirectory,
  selectingDirectory = false,
  selectionMessage,
  error,
  isRunning,
  disabled,
}) => {
  const [taskText, setTaskText] = useState('');
  const [customPath, setCustomPath] = useState('');
  const [showPathInput, setShowPathInput] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!taskText.trim() || isRunning) return;
    onSubmitTask(taskText.trim());
  };

  const handleCustomPathSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (customPath.trim()) {
      onSelectRepo(customPath.trim());
      onAnalyzePath(customPath.trim());
      setShowPathInput(false);
    }
  };

  const folderName = repoAnalysis?.name || (selectedRepo ? selectedRepo.split('/').pop() : null);

  return (
    <div className="code-agent-card" style={{ padding: '18px 20px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
      {/* Header */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
            <line x1="16" y1="2" x2="16" y2="6" />
            <line x1="8" y1="2" x2="8" y2="6" />
            <line x1="3" y1="10" x2="21" y2="10" />
          </svg>
          <h2 style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: '#f8fafc' }}>
            Local Workspace & Task
          </h2>
        </div>
        <p style={{ margin: '6px 0 0 0', fontSize: '12px', color: '#64748b', lineHeight: '1.4' }}>
          Select any project directory on your local machine. The agent will analyze the codebase, plan code modifications, execute real builds/tests, and verify changes.
        </p>
      </div>

      {/* Directory Selection Section */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <label style={{ fontSize: '11px', color: '#94a3b8', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Target Repository / Workspace
          </label>
          <button
            type="button"
            onClick={() => setShowPathInput(!showPathInput)}
            style={{
              background: 'none',
              border: 'none',
              color: '#38bdf8',
              fontSize: '11px',
              cursor: 'pointer',
              padding: 0,
              textDecoration: 'underline',
            }}
          >
            {showPathInput ? 'Hide path input' : 'Enter path manually'}
          </button>
        </div>

        {/* Primary Native Directory Picker Button */}
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            type="button"
            onClick={onSelectDirectory}
            disabled={isRunning || selectingDirectory || disabled}
            style={{
              flex: 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              padding: '10px 14px',
              backgroundColor: '#1d4ed8',
              color: '#ffffff',
              border: '1px solid #3b82f6',
              borderRadius: '6px',
              fontSize: '13px',
              fontWeight: 600,
              cursor: (isRunning || selectingDirectory || disabled) ? 'not-allowed' : 'pointer',
              boxShadow: '0 0 14px rgba(29, 78, 216, 0.3)',
              transition: 'all 0.15s ease',
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
                <span>Opening Folder Picker...</span>
              </>
            ) : (
              <>
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
                </svg>
                <span>Select Directory</span>
              </>
            )}
          </button>

          {/* Quick select dropdown if detected projects exist */}
          {repositories.length > 0 && (
            <div style={{ position: 'relative', width: '42%' }}>
              <select
                value={selectedRepo}
                onChange={(e) => {
                  onSelectRepo(e.target.value);
                  onAnalyzePath(e.target.value);
                }}
                disabled={isRunning || disabled}
                style={{
                  width: '100%',
                  height: '100%',
                  padding: '8px 24px 8px 10px',
                  backgroundColor: '#070b14',
                  border: '1px solid #192237',
                  borderRadius: '6px',
                  color: '#94a3b8',
                  fontSize: '12px',
                  outline: 'none',
                  appearance: 'none',
                  cursor: isRunning ? 'not-allowed' : 'pointer',
                }}
              >
                <option value="">Recent / Detected...</option>
                {repositories.map((repo) => (
                  <option key={repo.path} value={repo.path}>
                    {repo.name} ({repo.type})
                  </option>
                ))}
              </select>
              <svg
                width="12"
                height="12"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#64748b"
                strokeWidth="2"
                style={{ position: 'absolute', right: '8px', top: '13px', pointerEvents: 'none' }}
              >
                <path d="M6 9l6 6 6-6" />
              </svg>
            </div>
          )}
        </div>

        {/* Manual path input option */}
        {showPathInput && (
          <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
            <input
              type="text"
              value={customPath}
              onChange={(e) => setCustomPath(e.target.value)}
              placeholder="/Users/username/Projects/my-app"
              disabled={isRunning || disabled}
              style={{
                flex: 1,
                padding: '8px 12px',
                backgroundColor: '#070b14',
                border: '1px solid #192237',
                borderRadius: '6px',
                color: '#f8fafc',
                fontSize: '12px',
                outline: 'none',
                fontFamily: 'monospace',
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  handleCustomPathSubmit(e);
                }
              }}
            />
            <button
              type="button"
              onClick={handleCustomPathSubmit}
              disabled={!customPath.trim() || isRunning}
              style={{
                padding: '8px 14px',
                backgroundColor: '#1e293b',
                border: '1px solid #334155',
                borderRadius: '6px',
                color: '#f8fafc',
                fontSize: '12px',
                fontWeight: 500,
                cursor: 'pointer',
              }}
            >
              Analyze
            </button>
          </div>
        )}

        {/* Selection Status & Error Feedback */}
        {selectionMessage && (
          <div style={{ fontSize: '11px', color: '#38bdf8', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span>ℹ</span>
            <span>{selectionMessage}</span>
          </div>
        )}
        {error && (
          <div style={{ fontSize: '11px', color: '#f87171', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span>⚠</span>
            <span>{error}</span>
          </div>
        )}

        {/* Active Directory Details Badge */}
        {selectedRepo ? (
          <div
            style={{
              padding: '10px 12px',
              backgroundColor: '#0b1329',
              border: '1px solid #1e3a8a',
              borderRadius: '6px',
              display: 'flex',
              flexDirection: 'column',
              gap: '4px',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: '#38bdf8' }} />
                <span style={{ fontSize: '11px', color: '#93c5fd', fontWeight: 600, textTransform: 'uppercase' }}>
                  Repository: {folderName}
                </span>
              </div>
              <span
                style={{
                  fontSize: '11px',
                  fontWeight: 600,
                  color: repoAnalysis?.exists ? '#34d399' : '#94a3b8',
                  backgroundColor: 'rgba(56, 189, 248, 0.1)',
                  padding: '1px 6px',
                  borderRadius: '4px',
                }}
              >
                {repoAnalysis?.projectType || 'Verified'}
              </span>
            </div>
            <div
              style={{
                fontSize: '11px',
                color: '#94a3b8',
                fontFamily: 'monospace',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
              title={selectedRepo}
            >
              Path: {selectedRepo}
            </div>
          </div>
        ) : (
          <div
            style={{
              padding: '8px 12px',
              backgroundColor: '#0f172a',
              border: '1px dashed #334155',
              borderRadius: '6px',
              fontSize: '12px',
              color: '#64748b',
              textAlign: 'center',
            }}
          >
            No repository selected
          </div>
        )}
      </div>

      {/* Task Input Form */}
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          <label style={{ fontSize: '11px', color: '#94a3b8', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Task Description
          </label>
          <textarea
            rows={3}
            value={taskText}
            onChange={(e) => setTaskText(e.target.value)}
            disabled={isRunning || disabled}
            placeholder="Enter a task (e.g. Inspect repository structure, fix failing tests, or add feature)..."
            style={{
              width: '100%',
              padding: '12px 14px',
              backgroundColor: '#070b14',
              border: '1px solid #192237',
              borderRadius: '8px',
              color: '#f8fafc',
              fontSize: '13px',
              lineHeight: '1.5',
              resize: 'none',
              outline: 'none',
              boxSizing: 'border-box',
              fontFamily: 'inherit',
            }}
            onFocus={(e) => (e.currentTarget.style.borderColor = '#2563eb')}
            onBlur={(e) => (e.currentTarget.style.borderColor = '#192237')}
          />
        </div>

        {/* Start Task Button */}
        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <button
            type="submit"
            disabled={!taskText.trim() || !selectedRepo || isRunning || disabled}
            style={{
              padding: '9px 20px',
              backgroundColor: !taskText.trim() || !selectedRepo || isRunning || disabled ? '#1e293b' : '#2563eb',
              color: !taskText.trim() || !selectedRepo || isRunning || disabled ? '#64748b' : '#ffffff',
              border: 'none',
              borderRadius: '6px',
              fontSize: '13px',
              fontWeight: 600,
              cursor: !taskText.trim() || !selectedRepo || isRunning || disabled ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              boxShadow: !taskText.trim() || !selectedRepo || isRunning ? 'none' : '0 0 16px rgba(37, 99, 235, 0.4)',
              transition: 'all 0.15s ease',
              height: '38px',
            }}
          >
            {isRunning ? (
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
                <span>Agent Running...</span>
              </>
            ) : (
              <>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                  <polygon points="5 3 19 12 5 21 5 3" />
                </svg>
                <span>Start Task</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CreateTaskCard;
