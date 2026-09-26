import React, { useState } from 'react';
import { RealTestResult, TestResultItem } from '../types/agent';

interface Props {
  testResults: TestResultItem[];
  latestTestResult?: RealTestResult;
}

export const TestResultsViewer: React.FC<Props> = ({ testResults, latestTestResult }) => {
  const [showRawOutput, setShowRawOutput] = useState(false);

  const hasRealResult = !!latestTestResult;
  const hasLegacyResults = testResults && testResults.length > 0;

  if (!hasRealResult && !hasLegacyResults) {
    return (
      <div className="card" style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <h3 style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Automated Test Suite
        </h3>
        <div style={{ padding: '24px', textAlign: 'center', color: '#64748b', fontSize: '12px', border: '1px dashed #334155', borderRadius: '6px' }}>
          No test execution recorded yet. The agent executes real project tests during testing and verification phases.
        </div>
      </div>
    );
  }

  const status = latestTestResult?.status;
  const isPassed = status === 'PASSED';
  const isFailed = status === 'FAILED';
  const isNoTests = status === 'NO_TESTS';

  return (
    <div className="card" style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h3 style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Automated Test Execution
          </h3>
          {latestTestResult && (
            <div style={{ fontSize: '11px', color: '#64748b', marginTop: '2px', fontFamily: 'monospace' }}>
              Framework: <strong style={{ color: '#cbd5e1' }}>{latestTestResult.projectType}</strong> · Command:{' '}
              <code style={{ color: '#38bdf8' }}>{latestTestResult.command}</code>
            </div>
          )}
        </div>

        {latestTestResult && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span
              style={{
                fontSize: '11px',
                fontWeight: 700,
                padding: '2px 8px',
                borderRadius: '4px',
                backgroundColor: isPassed ? 'rgba(34, 197, 94, 0.2)' : isFailed ? 'rgba(239, 68, 68, 0.2)' : '#1e293b',
                color: isPassed ? '#4ade80' : isFailed ? '#f87171' : '#94a3b8',
              }}
            >
              {latestTestResult.status}
            </span>
            <span style={{ fontSize: '11px', color: '#64748b', fontFamily: 'monospace' }}>
              {(latestTestResult.durationMs / 1000).toFixed(2)}s
            </span>
          </div>
        )}
      </div>

      {/* Metrics Row (Derived exclusively from real execution) */}
      {latestTestResult && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '8px' }}>
          <div style={{ backgroundColor: '#0f172a', padding: '8px 12px', borderRadius: '6px', border: '1px solid #1e293b' }}>
            <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase' }}>Total Tests</div>
            <div style={{ fontSize: '16px', fontWeight: 700, color: '#f1f5f9', marginTop: '2px' }}>
              {latestTestResult.hasParsedMetrics ? (latestTestResult.totalTests ?? 0) : '—'}
            </div>
          </div>
          <div style={{ backgroundColor: '#0f172a', padding: '8px 12px', borderRadius: '6px', border: '1px solid #1e293b' }}>
            <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase' }}>Passed</div>
            <div style={{ fontSize: '16px', fontWeight: 700, color: '#4ade80', marginTop: '2px' }}>
              {latestTestResult.hasParsedMetrics ? (latestTestResult.passedTests ?? 0) : '—'}
            </div>
          </div>
          <div style={{ backgroundColor: '#0f172a', padding: '8px 12px', borderRadius: '6px', border: '1px solid #1e293b' }}>
            <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase' }}>Failed</div>
            <div style={{ fontSize: '16px', fontWeight: 700, color: (latestTestResult.failedTests ?? 0) > 0 ? '#f87171' : '#64748b', marginTop: '2px' }}>
              {latestTestResult.hasParsedMetrics ? (latestTestResult.failedTests ?? 0) : (latestTestResult.exitCode !== 0 ? 'Exit 1' : '0')}
            </div>
          </div>
          <div style={{ backgroundColor: '#0f172a', padding: '8px 12px', borderRadius: '6px', border: '1px solid #1e293b' }}>
            <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase' }}>Skipped</div>
            <div style={{ fontSize: '16px', fontWeight: 700, color: '#eab308', marginTop: '2px' }}>
              {latestTestResult.hasParsedMetrics ? (latestTestResult.skippedTests ?? 0) : '—'}
            </div>
          </div>
        </div>
      )}

      {/* Unparseable raw output notice */}
      {latestTestResult && !latestTestResult.hasParsedMetrics && !isNoTests && (
        <div style={{ backgroundColor: '#1e293b', padding: '8px 12px', borderRadius: '6px', fontSize: '11px', color: '#94a3b8' }}>
          ℹ️ Real test command completed with exit code <strong style={{ color: isPassed ? '#4ade80' : '#f87171' }}>{latestTestResult.exitCode}</strong>.
          Raw execution output preserved without synthetic estimates.
        </div>
      )}

      {/* Structured Failures Breakdown if any */}
      {latestTestResult && latestTestResult.failures && latestTestResult.failures.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <div style={{ fontSize: '11px', fontWeight: 600, color: '#f87171', textTransform: 'uppercase' }}>
            Failed Assertions ({latestTestResult.failures.length})
          </div>
          {latestTestResult.failures.map((fail, idx) => (
            <div
              key={idx}
              style={{
                backgroundColor: '#1a0d0d',
                border: '1px solid #7f1d1d',
                borderRadius: '6px',
                padding: '8px 12px',
                fontSize: '11px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', color: '#fca5a5', fontWeight: 600 }}>
                <span>{fail.suiteName} › {fail.testName}</span>
              </div>
              <div style={{ color: '#fee2e2', marginTop: '4px', fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}>
                {fail.message}
              </div>
              {fail.errorTrace && (
                <pre
                  style={{
                    marginTop: '6px',
                    padding: '6px',
                    backgroundColor: '#0f0505',
                    color: '#f87171',
                    fontSize: '10px',
                    borderRadius: '4px',
                    overflowX: 'auto',
                    whiteSpace: 'pre-wrap',
                  }}
                >
                  {fail.errorTrace}
                </pre>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Toggle raw stdout/stderr */}
      {latestTestResult && latestTestResult.output && (
        <div>
          <button
            onClick={() => setShowRawOutput(!showRawOutput)}
            style={{
              background: 'none',
              border: '1px solid #334155',
              borderRadius: '4px',
              color: '#94a3b8',
              padding: '4px 10px',
              fontSize: '11px',
              cursor: 'pointer',
            }}
          >
            {showRawOutput ? 'Hide Process Output' : 'View Full Process Output'}
          </button>

          {showRawOutput && (
            <pre
              style={{
                marginTop: '8px',
                padding: '10px',
                backgroundColor: '#020617',
                color: '#cbd5e1',
                fontSize: '10px',
                fontFamily: 'monospace',
                borderRadius: '6px',
                maxHeight: '260px',
                overflowY: 'auto',
                whiteSpace: 'pre-wrap',
                border: '1px solid #1e293b',
              }}
            >
              {latestTestResult.output}
            </pre>
          )}
        </div>
      )}

      {/* Legacy/Specific Test Suite Items */}
      {hasLegacyResults && (!latestTestResult || latestTestResult.failures.length === 0) && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {testResults.map((t, idx) => (
            <div
              key={idx}
              style={{
                padding: '6px 10px',
                borderRadius: '4px',
                backgroundColor: '#0f172a',
                border: t.passed ? '1px solid #166534' : '1px solid #991b1b',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                fontSize: '11px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span style={{ color: t.passed ? '#4ade80' : '#f87171', fontWeight: 700 }}>
                  {t.passed ? '✓ PASS' : '✗ FAIL'}
                </span>
                <span style={{ color: '#f1f5f9', fontFamily: 'monospace' }}>{t.testName}</span>
              </div>
              {t.durationMs > 0 && <span style={{ color: '#64748b' }}>{t.durationMs}ms</span>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
export default TestResultsViewer;
