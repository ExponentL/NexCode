import React, { useState } from 'react';
import { AgentState, RealTestResult } from '../types/agent';
import { AgentStatusBadge } from './AgentStatusBadge';

interface Props {
  state: AgentState | null;
}

export const RightPanel: React.FC<Props> = ({ state }) => {
  const [activeTab, setActiveTab] = useState<'FILES' | 'COMMANDS' | 'TESTS' | 'DIAGNOSTICS'>('TESTS');

  if (!state) {
    return (
      <aside style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        <div className="card" style={{ padding: '16px', textAlign: 'center', color: '#64748b', fontSize: '12px' }}>
          <div style={{ marginBottom: '8px', color: '#94a3b8', fontWeight: 600 }}>TELEMETRY STANDBY</div>
          Start a task to observe real-time agent status, commands, files, and test results.
        </div>
      </aside>
    );
  }

  const {
    status,
    currentStep,
    attempts,
    maxAttempts,
    filesModified,
    filesInspected,
    commandsExecuted,
    latestTestResult,
    errors,
  } = state;

  const testResult: RealTestResult | undefined = latestTestResult;
  const isTestPassed = testResult?.status === 'PASSED';
  const isTestFailed = testResult?.status === 'FAILED';

  return (
    <aside style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
      {/* 1. Agent Status & Current Step Card */}
      <div className="card" style={{ padding: '14px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span className="panel-title">Agent Status & Progress</span>
          <AgentStatusBadge status={status} attempts={attempts} maxAttempts={maxAttempts} />
        </div>

        <div
          style={{
            backgroundColor: '#070b14',
            padding: '10px',
            borderRadius: '6px',
            border: '1px solid #1e293b',
            display: 'flex',
            flexDirection: 'column',
            gap: '6px',
          }}
        >
          <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase', fontWeight: 600 }}>
            Current Step:
          </div>
          <div style={{ fontSize: '12px', fontWeight: 600, color: '#38bdf8', lineHeight: '1.4' }}>
            {currentStep || 'Awaiting instruction'}
          </div>
        </div>

        {/* Attempts & Recovery Progress Bar */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '10px', color: '#94a3b8' }}>
            <span>Execution Attempts</span>
            <span style={{ fontFamily: 'monospace' }}>
              {attempts} of {maxAttempts} max
            </span>
          </div>
          <div style={{ height: '6px', backgroundColor: '#1e293b', borderRadius: '3px', overflow: 'hidden' }}>
            <div
              style={{
                height: '100%',
                width: `${Math.min(100, (attempts / (maxAttempts || 5)) * 100)}%`,
                backgroundColor: attempts > 3 ? '#eab308' : '#38bdf8',
                transition: 'width 0.3s ease',
              }}
            />
          </div>
        </div>
      </div>

      {/* 2. Interactive Telemetry Tabs (Files / Commands / Tests / Diagnostics) */}
      <div className="card" style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: '360px' }}>
        <div className="panel-header" style={{ padding: '6px 10px' }}>
          <div style={{ display: 'flex', gap: '4px', overflowX: 'auto' }}>
            <button
              onClick={() => setActiveTab('TESTS')}
              style={{
                padding: '4px 8px',
                fontSize: '10px',
                borderRadius: '4px',
                border: 'none',
                cursor: 'pointer',
                backgroundColor: activeTab === 'TESTS' ? '#2563eb' : 'transparent',
                color: activeTab === 'TESTS' ? '#ffffff' : '#94a3b8',
                fontWeight: 600,
              }}
            >
              Tests {testResult ? `(${testResult.status})` : ''}
            </button>
            <button
              onClick={() => setActiveTab('FILES')}
              style={{
                padding: '4px 8px',
                fontSize: '10px',
                borderRadius: '4px',
                border: 'none',
                cursor: 'pointer',
                backgroundColor: activeTab === 'FILES' ? '#2563eb' : 'transparent',
                color: activeTab === 'FILES' ? '#ffffff' : '#94a3b8',
                fontWeight: 600,
              }}
            >
              Files ({filesModified.length})
            </button>
            <button
              onClick={() => setActiveTab('COMMANDS')}
              style={{
                padding: '4px 8px',
                fontSize: '10px',
                borderRadius: '4px',
                border: 'none',
                cursor: 'pointer',
                backgroundColor: activeTab === 'COMMANDS' ? '#2563eb' : 'transparent',
                color: activeTab === 'COMMANDS' ? '#ffffff' : '#94a3b8',
                fontWeight: 600,
              }}
            >
              Cmds ({commandsExecuted.length})
            </button>
            <button
              onClick={() => setActiveTab('DIAGNOSTICS')}
              style={{
                padding: '4px 8px',
                fontSize: '10px',
                borderRadius: '4px',
                border: 'none',
                cursor: 'pointer',
                backgroundColor: activeTab === 'DIAGNOSTICS' ? '#dc2626' : 'transparent',
                color: activeTab === 'DIAGNOSTICS' ? '#ffffff' : errors.length > 0 ? '#f87171' : '#94a3b8',
                fontWeight: 600,
              }}
            >
              Errors ({errors.length})
            </button>
          </div>
        </div>

        {/* Tab Content Area */}
        <div style={{ padding: '12px', flex: 1, overflowY: 'auto', maxHeight: '420px' }}>
          {/* TAB: TESTS */}
          {activeTab === 'TESTS' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {!testResult ? (
                <div style={{ textAlign: 'center', color: '#64748b', fontSize: '11px', padding: '30px 10px' }}>
                  No automated test results recorded yet. Test runner executes during testing/verification phases.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {/* Test Overview Banner */}
                  <div
                    style={{
                      padding: '8px 10px',
                      borderRadius: '6px',
                      backgroundColor: isTestPassed
                        ? 'rgba(34, 197, 94, 0.15)'
                        : isTestFailed
                        ? 'rgba(239, 68, 68, 0.15)'
                        : '#1e293b',
                      border: `1px solid ${isTestPassed ? '#166534' : isTestFailed ? '#991b1b' : '#334155'}`,
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <div>
                      <div
                        style={{
                          fontSize: '11px',
                          fontWeight: 700,
                          color: isTestPassed ? '#4ade80' : isTestFailed ? '#f87171' : '#cbd5e1',
                        }}
                      >
                        STATUS: {testResult.status}
                      </div>
                      <div style={{ fontSize: '10px', color: '#94a3b8', fontFamily: 'monospace' }}>
                        {testResult.projectType} · {(testResult.durationMs / 1000).toFixed(2)}s
                      </div>
                    </div>
                    <div style={{ fontSize: '10px', color: '#cbd5e1', fontFamily: 'monospace' }}>
                      Exit Code: <strong style={{ color: testResult.exitCode === 0 ? '#4ade80' : '#f87171' }}>{testResult.exitCode}</strong>
                    </div>
                  </div>

                  {/* Real Metrics Grid */}
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '6px' }}>
                    <div style={{ backgroundColor: '#070b14', padding: '6px', borderRadius: '4px', textAlign: 'center' }}>
                      <div style={{ fontSize: '9px', color: '#64748b' }}>TOTAL</div>
                      <div style={{ fontSize: '13px', fontWeight: 700, color: '#f8fafc' }}>
                        {testResult.hasParsedMetrics ? (testResult.totalTests ?? '—') : '—'}
                      </div>
                    </div>
                    <div style={{ backgroundColor: '#070b14', padding: '6px', borderRadius: '4px', textAlign: 'center' }}>
                      <div style={{ fontSize: '9px', color: '#64748b' }}>PASS</div>
                      <div style={{ fontSize: '13px', fontWeight: 700, color: '#4ade80' }}>
                        {testResult.hasParsedMetrics ? (testResult.passedTests ?? '—') : '—'}
                      </div>
                    </div>
                    <div style={{ backgroundColor: '#070b14', padding: '6px', borderRadius: '4px', textAlign: 'center' }}>
                      <div style={{ fontSize: '9px', color: '#64748b' }}>FAIL</div>
                      <div style={{ fontSize: '13px', fontWeight: 700, color: (testResult.failedTests ?? 0) > 0 ? '#f87171' : '#64748b' }}>
                        {testResult.hasParsedMetrics ? (testResult.failedTests ?? '0') : testResult.exitCode !== 0 ? '1' : '0'}
                      </div>
                    </div>
                    <div style={{ backgroundColor: '#070b14', padding: '6px', borderRadius: '4px', textAlign: 'center' }}>
                      <div style={{ fontSize: '9px', color: '#64748b' }}>SKIP</div>
                      <div style={{ fontSize: '13px', fontWeight: 700, color: '#eab308' }}>
                        {testResult.hasParsedMetrics ? (testResult.skippedTests ?? '0') : '0'}
                      </div>
                    </div>
                  </div>

                  {/* Command Line */}
                  <div style={{ fontSize: '10px', color: '#94a3b8' }}>
                    <span>Executed: </span>
                    <code style={{ color: '#38bdf8' }}>{testResult.command}</code>
                  </div>

                  {/* Failures list */}
                  {testResult.failures && testResult.failures.length > 0 && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                      <span style={{ fontSize: '10px', fontWeight: 600, color: '#f87171', textTransform: 'uppercase' }}>
                        Failures ({testResult.failures.length}):
                      </span>
                      {testResult.failures.map((f, i) => (
                        <div
                          key={i}
                          style={{
                            padding: '6px',
                            backgroundColor: '#1a0d0e',
                            border: '1px solid #7f1d1d',
                            borderRadius: '4px',
                            fontSize: '10px',
                          }}
                        >
                          <div style={{ color: '#fca5a5', fontWeight: 600 }}>{f.testName}</div>
                          <div style={{ color: '#fecaca', marginTop: '2px' }}>{f.message}</div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* TAB: FILES */}
          {activeTab === 'FILES' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <div>
                <div style={{ fontSize: '10px', fontWeight: 600, color: '#4ade80', textTransform: 'uppercase', marginBottom: '6px' }}>
                  Files Modified ({filesModified.length}):
                </div>
                {filesModified.length === 0 ? (
                  <div style={{ color: '#64748b', fontSize: '11px', fontStyle: 'italic' }}>No files modified yet</div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                    {filesModified.map((f, i) => (
                      <div
                        key={i}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '6px',
                          padding: '4px 6px',
                          backgroundColor: '#070b14',
                          border: '1px solid #1e293b',
                          borderRadius: '4px',
                          fontSize: '11px',
                          color: '#86efac',
                          fontFamily: 'monospace',
                        }}
                      >
                        <span>✎</span>
                        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={f}>
                          {f}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div>
                <div style={{ fontSize: '10px', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', marginBottom: '6px' }}>
                  Files Inspected ({filesInspected.length}):
                </div>
                {filesInspected.length === 0 ? (
                  <div style={{ color: '#64748b', fontSize: '11px', fontStyle: 'italic' }}>No files inspected yet</div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', maxHeight: '180px', overflowY: 'auto' }}>
                    {filesInspected.map((f, i) => (
                      <div
                        key={i}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '6px',
                          padding: '3px 6px',
                          fontSize: '10px',
                          color: '#94a3b8',
                          fontFamily: 'monospace',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                        title={f}
                      >
                        <span>👁</span>
                        <span>{f}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* TAB: COMMANDS */}
          {activeTab === 'COMMANDS' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <div style={{ fontSize: '10px', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase' }}>
                Terminal Commands Executed ({commandsExecuted.length}):
              </div>
              {commandsExecuted.length === 0 ? (
                <div style={{ color: '#64748b', fontSize: '11px', fontStyle: 'italic', padding: '20px 0' }}>
                  No terminal commands executed yet.
                </div>
              ) : (
                commandsExecuted.map((cmd, i) => (
                  <div
                    key={i}
                    style={{
                      padding: '6px 8px',
                      backgroundColor: '#070b14',
                      border: '1px solid #1e293b',
                      borderRadius: '4px',
                      fontFamily: 'monospace',
                      fontSize: '10px',
                      color: '#38bdf8',
                      wordBreak: 'break-all',
                    }}
                  >
                    <span style={{ color: '#64748b' }}>$ </span>
                    {cmd}
                  </div>
                ))
              )}
            </div>
          )}

          {/* TAB: DIAGNOSTICS */}
          {activeTab === 'DIAGNOSTICS' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <div style={{ fontSize: '10px', fontWeight: 600, color: '#f87171', textTransform: 'uppercase' }}>
                Error Diagnostics & Recovery ({errors.length}):
              </div>
              {errors.length === 0 ? (
                <div style={{ color: '#4ade80', fontSize: '11px', padding: '20px 0', textAlign: 'center' }}>
                  ✓ Zero runtime or harness errors recorded.
                </div>
              ) : (
                errors.map((err, i) => (
                  <div
                    key={i}
                    style={{
                      padding: '6px 8px',
                      backgroundColor: '#1f1315',
                      borderLeft: '3px solid #ef4444',
                      borderRadius: '4px',
                      color: '#fca5a5',
                      fontSize: '10px',
                      fontFamily: 'monospace',
                    }}
                  >
                    {err}
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </div>
    </aside>
  );
};
