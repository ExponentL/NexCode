import React from 'react';
import { VerificationResult } from '../types/agent';

interface Props {
  verificationResult?: VerificationResult | null;
}

export const FinalVerificationCard: React.FC<Props> = ({ verificationResult }) => {
  if (!verificationResult) {
    return (
      <div className="code-agent-card" style={{ padding: '14px 18px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
              <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
              <rect x="8" y="2" width="8" height="4" rx="1" ry="1" />
            </svg>
            <span style={{ fontSize: '13px', fontWeight: 600, color: '#f8fafc' }}>
              Final Verification
            </span>
          </div>

          <span
            style={{
              padding: '2px 8px',
              borderRadius: '9999px',
              fontSize: '11px',
              fontWeight: 500,
              backgroundColor: '#1e293b',
              color: '#94a3b8',
            }}
          >
            Waiting for task
          </span>
        </div>

        <div style={{ padding: '8px 0', textAlign: 'center', color: '#64748b', fontSize: '12px' }}>
          Verification checks will execute with real evidence once the agent finishes.
        </div>
      </div>
    );
  }

  const isVerified = verificationResult.verified;

  return (
    <div className="code-agent-card" style={{ padding: '14px 18px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
            <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
            <rect x="8" y="2" width="8" height="4" rx="1" ry="1" />
          </svg>
          <span style={{ fontSize: '13px', fontWeight: 600, color: '#f8fafc' }}>
            Final Verification
          </span>
        </div>

        {/* Badge */}
        <span
          style={{
            padding: '2px 8px',
            borderRadius: '9999px',
            fontSize: '11px',
            fontWeight: 600,
            backgroundColor: isVerified ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
            color: isVerified ? '#34d399' : '#f87171',
            border: `1px solid ${isVerified ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`,
          }}
        >
          {isVerified ? '✓ VERIFIED' : '✗ NOT VERIFIED'}
        </span>
      </div>

      {/* Summary message */}
      <div style={{ fontSize: '11.5px', color: '#cbd5e1', lineHeight: '1.4' }}>
        {verificationResult.summary}
      </div>

      {/* Real checklist items */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '4px' }}>
        {verificationResult.checksPerformed.map((c, i) => (
          <div key={i} style={{ display: 'flex', flexDirection: 'column', gap: '1px', fontSize: '11.5px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ color: c.passed ? '#34d399' : '#f87171', fontWeight: 700 }}>
                {c.passed ? '✓' : '✗'}
              </span>
              <span style={{ color: '#f8fafc', fontWeight: 500 }}>{c.name}</span>
            </div>
            {c.evidence && (
              <span style={{ color: '#64748b', fontSize: '10.5px', paddingLeft: '14px' }}>
                {c.evidence}
              </span>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default FinalVerificationCard;
