import React from 'react';

interface Props {
  errors: string[];
  attempts: number;
  maxAttempts: number;
}

export const ErrorRecoveryPanel: React.FC<Props> = ({ errors, attempts, maxAttempts }) => {
  if (errors.length === 0) {
    return null;
  }

  return (
    <div
      className="card"
      style={{
        padding: '16px',
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
        borderColor: '#991b1b',
        backgroundColor: 'rgba(239, 68, 68, 0.04)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ color: '#f87171', fontSize: '14px' }}>⚠️</span>
          <h3 style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: '#f87171', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Harness Errors & Recovery Diagnostics ({errors.length})
          </h3>
        </div>
        <span style={{ fontSize: '11px', color: '#fca5a5', fontFamily: 'monospace' }}>
          Retry Cycle: {attempts} / {maxAttempts}
        </span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', maxHeight: '180px', overflowY: 'auto' }}>
        {errors.map((err, idx) => (
          <div
            key={idx}
            style={{
              padding: '8px 10px',
              backgroundColor: '#1f1315',
              borderRadius: '4px',
              fontSize: '11px',
              fontFamily: 'JetBrains Mono, monospace',
              color: '#fca5a5',
              borderLeft: '3px solid #ef4444',
              wordBreak: 'break-word',
            }}
          >
            {err}
          </div>
        ))}
      </div>
    </div>
  );
};
