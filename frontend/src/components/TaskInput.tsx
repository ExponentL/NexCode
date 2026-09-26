import React, { useState } from 'react';

interface Props {
  onSubmit: (task: string) => void;
  onCancel: () => void;
  isRunning: boolean;
  disabled?: boolean;
}

export const TaskInput: React.FC<Props> = ({ onSubmit, onCancel, isRunning, disabled }) => {
  const [taskText, setTaskText] = useState('');

  const PRESETS = [
    'Inspect codebase, implement /api/health endpoint with system uptime, and verify via test runner',
    'Find test failures in the test suite, analyze the root cause, fix the code, and confirm tests pass',
    'Add input validation to user registration and create comprehensive unit tests',
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!taskText.trim() || isRunning) return;
    onSubmit(taskText.trim());
  };

  return (
    <div className="card" style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <label style={{ fontSize: '12px', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Autonomous Engineering Task
        </label>
        <span style={{ fontSize: '11px', color: '#64748b' }}>
          Autonomous Cycle: Inspect → Plan → Modify → Run Tests → Verify → Fix
        </span>
      </div>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        <textarea
          rows={3}
          value={taskText}
          onChange={(e) => setTaskText(e.target.value)}
          disabled={isRunning || disabled}
          placeholder="Describe the software engineering task in detail (e.g., 'Add a new endpoint, fix failing tests, refactor authentication')..."
          style={{
            width: '100%',
            padding: '12px',
            backgroundColor: '#0f172a',
            border: '1px solid #334155',
            borderRadius: '6px',
            color: '#f8fafc',
            fontSize: '13px',
            lineHeight: '1.5',
            resize: 'vertical',
            outline: 'none',
            boxSizing: 'border-box',
          }}
        />

        {/* Preset prompts */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', alignItems: 'center' }}>
          <span style={{ fontSize: '11px', color: '#64748b' }}>Quick Presets:</span>
          {PRESETS.map((preset, idx) => (
            <button
              key={idx}
              type="button"
              onClick={() => setTaskText(preset)}
              disabled={isRunning || disabled}
              style={{
                fontSize: '11px',
                backgroundColor: '#1e293b',
                color: '#94a3b8',
                border: '1px solid #334155',
                borderRadius: '4px',
                padding: '3px 8px',
                cursor: 'pointer',
                textAlign: 'left',
              }}
            >
              {preset.substring(0, 45)}...
            </button>
          ))}
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '4px' }}>
          {isRunning ? (
            <button
              type="button"
              onClick={onCancel}
              style={{
                backgroundColor: '#dc2626',
                color: '#ffffff',
                border: 'none',
                borderRadius: '6px',
                padding: '8px 18px',
                fontSize: '13px',
                fontWeight: 600,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              </svg>
              Cancel Autonomous Execution
            </button>
          ) : (
            <button
              type="submit"
              disabled={!taskText.trim() || disabled}
              style={{
                backgroundColor: !taskText.trim() || disabled ? '#334155' : '#2563eb',
                color: '#ffffff',
                border: 'none',
                borderRadius: '6px',
                padding: '8px 20px',
                fontSize: '13px',
                fontWeight: 600,
                cursor: !taskText.trim() || disabled ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                boxShadow: !taskText.trim() || disabled ? 'none' : '0 2px 10px rgba(37, 99, 235, 0.4)',
              }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <polygon points="5 3 19 12 5 21 5 3" />
              </svg>
              Dispatch Agent
            </button>
          )}
        </div>
      </form>
    </div>
  );
};
