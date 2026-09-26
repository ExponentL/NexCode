import React from 'react';
import { RealTestResult } from '../types/agent';

interface Props {
  latestTestResult?: RealTestResult;
}

export const TestResultsCard: React.FC<Props> = ({ latestTestResult }) => {
  if (!latestTestResult) {
    return (
      <div className="code-agent-card" style={{ padding: '14px 18px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
            <path d="M10 2v7.31L4.66 18.25A2 2 0 0 0 6.36 21h11.28a2 2 0 0 0 1.7-2.75L14 9.31V2" />
          </svg>
          <span style={{ fontSize: '13px', fontWeight: 600, color: '#f8fafc' }}>
            Test Results
          </span>
        </div>
        <div style={{ padding: '10px 0', textAlign: 'center', color: '#64748b', fontSize: '12px' }}>
          Tests have not been executed
        </div>
      </div>
    );
  }

  if (latestTestResult.status === 'NO_TESTS') {
    return (
      <div className="code-agent-card" style={{ padding: '14px 18px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2">
              <path d="M10 2v7.31L4.66 18.25A2 2 0 0 0 6.36 21h11.28a2 2 0 0 0 1.7-2.75L14 9.31V2" />
            </svg>
            <span style={{ fontSize: '13px', fontWeight: 600, color: '#f8fafc' }}>
              Test Results
            </span>
          </div>
          <span style={{ padding: '2px 8px', borderRadius: '9999px', fontSize: '11px', color: '#94a3b8', backgroundColor: '#1e293b' }}>
            No Tests
          </span>
        </div>
        <div style={{ fontSize: '12px', color: '#94a3b8' }}>
          No test suite detected in repository
        </div>
      </div>
    );
  }

  const isSuccess = latestTestResult.status === 'PASSED';
  const durationSec = (latestTestResult.durationMs / 1000).toFixed(2);

  return (
    <div className="code-agent-card" style={{ padding: '14px 18px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
            <path d="M10 2v7.31L4.66 18.25A2 2 0 0 0 6.36 21h11.28a2 2 0 0 0 1.7-2.75L14 9.31V2" />
          </svg>
          <span style={{ fontSize: '13px', fontWeight: 600, color: '#f8fafc' }}>
            Test Results
          </span>
        </div>

        {/* Badge */}
        <span
          style={{
            padding: '2px 8px',
            borderRadius: '9999px',
            fontSize: '11px',
            fontWeight: 500,
            backgroundColor: isSuccess ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
            color: isSuccess ? '#34d399' : '#f87171',
            border: `1px solid ${isSuccess ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`,
          }}
        >
          {isSuccess ? 'Passed' : 'Failed'}
        </span>
      </div>

      {/* Real details */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '12px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span style={{ color: '#64748b' }}>Command</span>
          <span style={{ color: '#f8fafc', fontFamily: 'monospace', fontSize: '11px' }}>
            {latestTestResult.command}
          </span>
        </div>

        {latestTestResult.hasParsedMetrics ? (
          <>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: '#64748b' }}>Metrics</span>
              <span style={{ color: '#f8fafc', fontFamily: 'monospace' }}>
                {latestTestResult.passedTests ?? 0} passed • {latestTestResult.failedTests ?? 0} failed
                {latestTestResult.skippedTests ? ` • ${latestTestResult.skippedTests} skipped` : ''}
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: '#64748b' }}>Total</span>
              <span style={{ color: '#f8fafc', fontFamily: 'monospace' }}>
                {latestTestResult.totalTests ?? 0} total
              </span>
            </div>
          </>
        ) : (
          <div style={{ fontSize: '11px', color: '#94a3b8' }}>
            Test command completed (exit code {latestTestResult.exitCode}). See output.
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span style={{ color: '#64748b' }}>Duration</span>
          <span style={{ color: '#64748b' }}>{durationSec}s</span>
        </div>
      </div>
    </div>
  );
};

export default TestResultsCard;
