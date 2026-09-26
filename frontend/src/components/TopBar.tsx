import React, { useEffect, useState } from 'react';
import { AgentStatus, RepositoryInfo } from '../types/agent';
import { AgentStatusBadge } from './AgentStatusBadge';

interface Props {
  selectedRepo: string;
  repositories: RepositoryInfo[];
  status: AgentStatus;
  taskId?: string;
  attempts?: number;
  maxAttempts?: number;
  isRunning: boolean;
  onReset?: () => void;
}

export const TopBar: React.FC<Props> = ({
  selectedRepo,
  repositories,
  status,
  taskId,
  attempts,
  maxAttempts,
  isRunning,
  onReset,
}) => {
  const [elapsedSec, setElapsedSec] = useState(0);

  useEffect(() => {
    let timer: any;
    if (isRunning) {
      timer = setInterval(() => {
        setElapsedSec((prev) => prev + 1);
      }, 1000);
    } else {
      setElapsedSec(0);
    }
    return () => clearInterval(timer);
  }, [isRunning, taskId]);

  const formatElapsed = (sec: number) => {
    const mins = Math.floor(sec / 60);
    const remainingSec = sec % 60;
    return `${mins.toString().padStart(2, '0')}:${remainingSec.toString().padStart(2, '0')}`;
  };

  const currentRepoInfo = repositories.find((r) => r.path === selectedRepo);
  const repoName = currentRepoInfo?.name || selectedRepo.split('/').pop() || 'Workspace';
  const repoType = currentRepoInfo?.type || 'Repository';

  return (
    <header
      style={{
        borderBottom: '1px solid #1e293b',
        backgroundColor: '#070b14',
        padding: '10px 20px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        position: 'sticky',
        top: 0,
        zIndex: 50,
      }}
    >
      {/* Brand & Project Identification */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        <div
          style={{
            width: '32px',
            height: '32px',
            borderRadius: '6px',
            background: 'linear-gradient(135deg, #1d4ed8 0%, #0284c7 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#ffffff',
            fontWeight: 800,
            fontSize: '13px',
            letterSpacing: '0.05em',
            boxShadow: '0 0 16px rgba(2, 132, 199, 0.4)',
            border: '1px solid rgba(56, 189, 248, 0.3)',
          }}
        >
          NC
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '14px', fontWeight: 700, letterSpacing: '-0.01em', color: '#f8fafc' }}>
              NEXCODE — AUTONOMOUS AI CODING-AGENT HARNESS
            </span>
            <span
              style={{
                fontSize: '10px',
                fontFamily: 'monospace',
                backgroundColor: '#1e293b',
                color: '#38bdf8',
                padding: '1px 6px',
                borderRadius: '4px',
                border: '1px solid #334155',
              }}
            >
              v1.0-LIVE
            </span>
          </div>
          <div style={{ fontSize: '11px', color: '#64748b', display: 'flex', gap: '10px' }}>
            <span>Java 21 + Spring Boot</span>
            <span>•</span>
            <span>Foundation Model Harness</span>
            <span>•</span>
            <span>Automated Test Verification</span>
          </div>
        </div>
      </div>

      {/* Center: Repository Context Badge */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          backgroundColor: '#0d1322',
          padding: '6px 12px',
          borderRadius: '6px',
          border: '1px solid #1e293b',
          maxWidth: '380px',
        }}
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#38bdf8" strokeWidth="2">
          <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
        </svg>
        <div style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span
              style={{
                fontSize: '12px',
                fontWeight: 600,
                color: '#e2e8f0',
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
              title={selectedRepo}
            >
              {repoName}
            </span>
            <span
              style={{
                fontSize: '9px',
                fontWeight: 600,
                color: '#94a3b8',
                backgroundColor: '#1e293b',
                padding: '1px 5px',
                borderRadius: '3px',
              }}
            >
              {repoType}
            </span>
          </div>
          <span
            style={{
              fontSize: '10px',
              color: '#64748b',
              fontFamily: 'monospace',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
            title={selectedRepo}
          >
            {selectedRepo || 'No directory selected'}
          </span>
        </div>
      </div>

      {/* Right: Live Agent Status & Telemetry */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        {isRunning && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              backgroundColor: 'rgba(14, 165, 233, 0.1)',
              border: '1px solid rgba(14, 165, 233, 0.3)',
              padding: '4px 8px',
              borderRadius: '6px',
              fontSize: '11px',
              fontFamily: 'monospace',
              color: '#38bdf8',
            }}
          >
            <span
              style={{
                width: '6px',
                height: '6px',
                borderRadius: '50%',
                backgroundColor: '#38bdf8',
                animation: 'pulse-glow 1.5s infinite',
              }}
            />
            <span>{formatElapsed(elapsedSec)}</span>
          </div>
        )}

        {taskId && (
          <div style={{ fontSize: '11px', color: '#64748b', fontFamily: 'monospace' }}>
            ID: <strong style={{ color: '#cbd5e1' }}>{taskId.substring(0, 8)}</strong>
          </div>
        )}

        <AgentStatusBadge status={status} attempts={attempts} maxAttempts={maxAttempts} />

        {onReset && (
          <button
            onClick={onReset}
            disabled={isRunning}
            title="Reset dashboard and start a new task"
            style={{
              backgroundColor: '#1e293b',
              color: '#94a3b8',
              border: '1px solid #334155',
              borderRadius: '5px',
              padding: '5px 10px',
              fontSize: '11px',
              cursor: isRunning ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 5v14M5 12h14" />
            </svg>
            New Task
          </button>
        )}
      </div>
    </header>
  );
};
