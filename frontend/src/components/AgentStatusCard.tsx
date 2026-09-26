import React from 'react';
import { AgentState } from '../types/agent';

interface Props {
  state: AgentState | null;
}

export const AgentStatusCard: React.FC<Props> = ({ state }) => {
  const isRunning =
    state !== null &&
    state.status !== 'COMPLETED' &&
    state.status !== 'FAILED' &&
    state.status !== 'CANCELLED';

  const statusLabel = !state
    ? 'Idle'
    : isRunning
    ? 'Running'
    : state.status === 'COMPLETED'
    ? 'Completed'
    : state.status === 'FAILED'
    ? 'Failed'
    : state.status === 'CANCELLED'
    ? 'Cancelled'
    : state.status;

  const stepTitle = state ? state.currentStep : 'Waiting for task';
  const attempts = state ? state.attempts : 0;
  const maxAttempts = state ? state.maxAttempts : 5;

  return (
    <div className="code-agent-card" style={{ padding: '16px 18px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
            <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
            <line x1="8" y1="21" x2="16" y2="21" />
            <line x1="12" y1="17" x2="12" y2="21" />
          </svg>
          <span style={{ fontSize: '13px', fontWeight: 600, color: '#f8fafc' }}>
            Agent Status
          </span>
        </div>

        {/* Status Badge */}
        <span
          style={{
            padding: '2px 8px',
            borderRadius: '9999px',
            fontSize: '11px',
            fontWeight: 600,
            backgroundColor: isRunning
              ? 'rgba(37, 99, 235, 0.15)'
              : state?.status === 'COMPLETED'
              ? 'rgba(16, 185, 129, 0.15)'
              : state?.status === 'FAILED'
              ? 'rgba(239, 68, 68, 0.15)'
              : 'rgba(100, 116, 139, 0.15)',
            color: isRunning
              ? '#60a5fa'
              : state?.status === 'COMPLETED'
              ? '#34d399'
              : state?.status === 'FAILED'
              ? '#f87171'
              : '#94a3b8',
            border: `1px solid ${
              isRunning
                ? 'rgba(37, 99, 235, 0.3)'
                : state?.status === 'COMPLETED'
                ? 'rgba(16, 185, 129, 0.3)'
                : state?.status === 'FAILED'
                ? 'rgba(239, 68, 68, 0.3)'
                : '#334155'
            }`,
          }}
        >
          {statusLabel}
        </span>
      </div>

      {/* Main Status Display */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '2px' }}>
        <div
          style={{
            width: '32px',
            height: '32px',
            borderRadius: '50%',
            backgroundColor: isRunning ? '#1d4ed8' : state?.status === 'COMPLETED' ? '#059669' : '#1e293b',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <div
            style={{
              width: '10px',
              height: '10px',
              borderRadius: '50%',
              backgroundColor: isRunning ? '#ffffff' : state?.status === 'COMPLETED' ? '#34d399' : '#64748b',
            }}
          />
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0, flex: 1 }}>
          <span style={{ fontSize: '11px', color: '#64748b' }}>
            Current Action
          </span>
          <span
            style={{
              fontSize: '13px',
              fontWeight: 600,
              color: '#f8fafc',
              lineHeight: '1.3',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
            title={stepTitle}
          >
            {stepTitle}
          </span>
        </div>
      </div>

      {/* Attempts Row */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '11px', color: '#64748b' }}>
        <span>Attempts</span>
        <span style={{ fontFamily: 'monospace', color: '#cbd5e1' }}>
          {attempts}/{maxAttempts}
        </span>
      </div>
    </div>
  );
};

export default AgentStatusCard;
