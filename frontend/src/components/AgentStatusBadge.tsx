import React from 'react';
import { AgentStatus } from '../types/agent';

interface Props {
  status: AgentStatus;
  attempts?: number;
  maxAttempts?: number;
}

export const AgentStatusBadge: React.FC<Props> = ({ status, attempts, maxAttempts }) => {
  const getBadgeStyle = (): { bg: string; text: string; border: string; label: string; pulse: boolean } => {
    switch (status) {
      case 'IDLE':
        return { bg: 'rgba(100, 116, 139, 0.15)', text: '#94a3b8', border: '#334155', label: 'IDLE / QUEUED', pulse: false };
      case 'ANALYZING':
        return { bg: 'rgba(168, 85, 247, 0.15)', text: '#c084fc', border: '#7e22ce', label: 'ANALYZING CODEBASE', pulse: true };
      case 'PLANNING':
        return { bg: 'rgba(234, 179, 8, 0.15)', text: '#fde047', border: '#a16207', label: 'SYNTHESIZING PLAN', pulse: true };
      case 'EXECUTING':
        return { bg: 'rgba(14, 165, 233, 0.15)', text: '#38bdf8', border: '#0369a1', label: 'EXECUTING ACTIONS', pulse: true };
      case 'TESTING':
        return { bg: 'rgba(59, 130, 246, 0.15)', text: '#60a5fa', border: '#2563eb', label: 'RUNNING TEST SUITE', pulse: true };
      case 'RECOVERING':
        return { bg: 'rgba(236, 72, 153, 0.15)', text: '#f472b6', border: '#be185d', label: 'RECOVERING & RETRYING', pulse: true };
      case 'VERIFYING':
        return { bg: 'rgba(249, 115, 22, 0.15)', text: '#fb923c', border: '#c2410c', label: 'VERIFYING RESULTS', pulse: true };
      case 'COMPLETED':
        return { bg: 'rgba(34, 197, 94, 0.15)', text: '#4ade80', border: '#15803d', label: 'COMPLETED & VERIFIED', pulse: false };
      case 'FAILED':
        return { bg: 'rgba(239, 68, 68, 0.15)', text: '#f87171', border: '#b91c1c', label: 'EXECUTION FAILED', pulse: false };
      case 'CANCELLED':
        return { bg: 'rgba(148, 163, 184, 0.15)', text: '#94a3b8', border: '#475569', label: 'CANCELLED', pulse: false };
      default:
        return { bg: 'rgba(100, 116, 139, 0.15)', text: '#cbd5e1', border: '#334155', label: status, pulse: false };
    }
  };

  const style = getBadgeStyle();

  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '6px',
          padding: '4px 10px',
          borderRadius: '9999px',
          fontSize: '11px',
          fontWeight: 700,
          letterSpacing: '0.05em',
          backgroundColor: style.bg,
          color: style.text,
          border: `1px solid ${style.border}`,
          boxShadow: style.pulse ? '0 0 10px rgba(56, 189, 248, 0.2)' : 'none',
        }}
      >
        <span
          style={{
            width: '6px',
            height: '6px',
            borderRadius: '50%',
            backgroundColor: style.text,
            animation: style.pulse ? 'pulse-glow 1.5s infinite' : 'none',
          }}
        />
        {style.label}
      </span>

      {attempts !== undefined && maxAttempts !== undefined && attempts > 0 && (
        <span
          style={{
            fontSize: '11px',
            color: '#94a3b8',
            fontFamily: 'monospace',
            backgroundColor: '#1e293b',
            padding: '3px 8px',
            borderRadius: '6px',
            border: '1px solid #334155',
          }}
        >
          Attempt {attempts}/{maxAttempts}
        </span>
      )}
    </div>
  );
};
