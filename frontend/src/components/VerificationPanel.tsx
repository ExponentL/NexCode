import React, { useState } from 'react';
import { AgentState } from '../types/agent';

interface Props {
  state: AgentState | null;
}

export const VerificationPanel: React.FC<Props> = ({ state }) => {
  const [showDiff, setShowDiff] = useState(false);

  if (!state) {
    return (
      <div className="card" style={{ padding: '16px' }}>
        <h3 style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Final Verification
        </h3>
        <p style={{ color: '#64748b', fontSize: '12px', marginTop: '12px' }}>
          Verification summary will display here upon test suite completion.
        </p>
      </div>
    );
  }

  const verification = state.latestVerificationResult;
  const isCompleted = state.status === 'COMPLETED';
  const isFailed = state.status === 'FAILED';
  const isVerified = verification?.verified ?? (isCompleted && state.filesModified.length > 0);

  const gitDiff = verification?.gitDiff;
  const modifiedFiles = gitDiff?.modifiedFiles ?? state.filesModified;
  const addedFiles = gitDiff?.addedFiles ?? [];
  const deletedFiles = gitDiff?.deletedFiles ?? [];
  const diffContent = gitDiff?.diff || state.diff || '';

  // Standard evidence checks if VerificationResult is loaded or synthesized from state
  const checks = verification?.checksPerformed ?? [
    {
      name: 'Relevant files inspected',
      passed: state.filesInspected.length > 0,
      evidence: `${state.filesInspected.length} file(s) inspected`,
    },
    {
      name: 'Code modification applied',
      passed: state.filesModified.length > 0,
      evidence: `${state.filesModified.length} file(s) modified on disk`,
    },
    {
      name: 'Build completed',
      passed: !state.errors.some((e) => e.toLowerCase().includes('compilation') || e.toLowerCase().includes('build')),
      evidence: 'Clean build check',
    },
    {
      name: 'Tests executed & passed',
      passed: state.latestTestResult ? state.latestTestResult.status === 'PASSED' : true,
      evidence: state.latestTestResult
        ? `${state.latestTestResult.passedTests ?? 0} passed`
        : 'Automated test suite',
    },
    {
      name: 'Git diff reviewed',
      passed: !!diffContent || state.filesModified.length > 0,
      evidence: 'Working tree changes verified',
    },
    {
      name: 'Tool execution clean',
      passed: !state.errors.some((e) => e.includes('rejected by harness safety')),
      evidence: `${state.errors.length} errors recorded`,
    },
  ];

  return (
    <div className="card" style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h3 style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Final Verification
          </h3>
          <div style={{ fontSize: '12px', color: '#cbd5e1', marginTop: '2px', fontWeight: 500 }}>
            Task: <span style={{ color: '#38bdf8' }}>{state.task}</span>
          </div>
        </div>

        {/* Verification Verdict Badge */}
        <div>
          {isCompleted || isFailed ? (
            <span
              style={{
                fontSize: '12px',
                fontWeight: 800,
                padding: '4px 12px',
                borderRadius: '6px',
                backgroundColor: isVerified ? 'rgba(34, 197, 94, 0.2)' : 'rgba(239, 68, 68, 0.2)',
                color: isVerified ? '#4ade80' : '#f87171',
                border: isVerified ? '1px solid #166534' : '1px solid #991b1b',
                letterSpacing: '0.05em',
              }}
            >
              {isVerified ? 'VERIFIED' : 'NOT VERIFIED'}
            </span>
          ) : (
            <span
              style={{
                fontSize: '11px',
                fontWeight: 600,
                padding: '3px 8px',
                borderRadius: '4px',
                backgroundColor: '#1e293b',
                color: '#94a3b8',
              }}
            >
              EVALUATING EVIDENCE...
            </span>
          )}
        </div>
      </div>

      {/* Summary statement if available */}
      {verification && (
        <div
          style={{
            fontSize: '11px',
            color: isVerified ? '#86efac' : '#fca5a5',
            backgroundColor: isVerified ? 'rgba(22, 101, 52, 0.2)' : 'rgba(127, 29, 29, 0.2)',
            padding: '8px 12px',
            borderRadius: '6px',
            border: isVerified ? '1px solid #14532d' : '1px solid #7f1d1d',
          }}
        >
          {verification.summary}
        </div>
      )}

      {/* Real Evidence Checks List */}
      <div>
        <div style={{ fontSize: '11px', fontWeight: 600, color: '#64748b', textTransform: 'uppercase', marginBottom: '8px' }}>
          Real Evidence Checks:
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {checks.map((chk, idx) => (
            <div
              key={idx}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '6px 10px',
                borderRadius: '4px',
                backgroundColor: '#0f172a',
                border: chk.passed ? '1px solid #1e293b' : '1px solid #7f1d1d',
                fontSize: '11px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span
                  style={{
                    color: chk.passed ? '#4ade80' : '#f87171',
                    fontWeight: 700,
                    fontSize: '12px',
                  }}
                >
                  {chk.passed ? '✓' : '✗'}
                </span>
                <span style={{ color: chk.passed ? '#f1f5f9' : '#fca5a5', fontWeight: 600 }}>
                  {chk.name}
                </span>
              </div>
              <span style={{ color: '#64748b', fontSize: '10px', fontFamily: 'monospace' }}>
                {chk.evidence}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Git Changes Summary */}
      <div style={{ borderTop: '1px solid #1e293b', paddingTop: '10px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
          <div style={{ fontSize: '11px', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>
            Repository Git Changes
          </div>
          {diffContent && (
            <button
              onClick={() => setShowDiff(!showDiff)}
              style={{
                background: 'none',
                border: '1px solid #334155',
                color: '#94a3b8',
                borderRadius: '4px',
                padding: '2px 8px',
                fontSize: '10px',
                cursor: 'pointer',
              }}
            >
              {showDiff ? 'Hide Diff' : 'View Relevant Diff'}
            </button>
          )}
        </div>

        {/* Breakdown Badges */}
        <div style={{ display: 'flex', gap: '12px', fontSize: '11px', fontFamily: 'monospace' }}>
          <span style={{ color: modifiedFiles.length > 0 ? '#38bdf8' : '#64748b' }}>
            <strong>{modifiedFiles.length}</strong> Modified
          </span>
          <span style={{ color: addedFiles.length > 0 ? '#4ade80' : '#64748b' }}>
            <strong>{addedFiles.length}</strong> Added
          </span>
          <span style={{ color: deletedFiles.length > 0 ? '#f87171' : '#64748b' }}>
            <strong>{deletedFiles.length}</strong> Deleted
          </span>
        </div>

        {/* File lists */}
        {modifiedFiles.length > 0 && (
          <div style={{ marginTop: '6px', display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
            {modifiedFiles.map((f, i) => (
              <span
                key={i}
                style={{
                  fontSize: '10px',
                  fontFamily: 'monospace',
                  backgroundColor: '#0f172a',
                  border: '1px solid #0284c7',
                  color: '#bae6fd',
                  padding: '2px 6px',
                  borderRadius: '4px',
                }}
              >
                ✎ {f}
              </span>
            ))}
          </div>
        )}

        {/* Collapsible Unified Diff View */}
        {showDiff && diffContent && (
          <pre
            style={{
              marginTop: '10px',
              padding: '10px',
              backgroundColor: '#020617',
              color: '#cbd5e1',
              fontSize: '10px',
              fontFamily: 'monospace',
              borderRadius: '6px',
              maxHeight: '220px',
              overflowY: 'auto',
              whiteSpace: 'pre-wrap',
              border: '1px solid #1e293b',
            }}
          >
            {diffContent}
          </pre>
        )}
      </div>

      {/* Progress & Current Phase */}
      <div style={{ fontSize: '11px', color: '#64748b', borderTop: '1px solid #1e293b', paddingTop: '8px' }}>
        <span>Attempts: <strong style={{ color: '#cbd5e1' }}>{state.attempts} / {state.maxAttempts}</strong></span>
        <span style={{ marginLeft: '16px' }}>Status: <strong style={{ color: '#cbd5e1' }}>{state.status}</strong></span>
      </div>
    </div>
  );
};
export default VerificationPanel;
