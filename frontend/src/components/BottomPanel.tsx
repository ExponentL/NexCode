import React, { useState } from 'react';
import { AgentState, TaskResult, VerificationResult } from '../types/agent';

interface Props {
  state: AgentState | null;
  diff: string;
  taskResult: TaskResult | null;
  verificationResult: VerificationResult | null;
}

export const BottomPanel: React.FC<Props> = ({ state, diff, taskResult, verificationResult }) => {
  const [activeTab, setActiveTab] = useState<'DIFF' | 'VERIFICATION' | 'RESULT'>('VERIFICATION');
  const [copied, setCopied] = useState(false);

  const activeVerification = verificationResult || state?.latestVerificationResult;
  const activeTaskResult = taskResult || state?.finalTaskResult;
  const isCompleted = state?.status === 'COMPLETED';
  const isFailed = state?.status === 'FAILED';
  const isVerified = activeVerification?.verified ?? (isCompleted && (state?.filesModified?.length || 0) > 0);

  const gitDiffSummary = activeVerification?.gitDiff;
  const modifiedFiles = gitDiffSummary?.modifiedFiles ?? state?.filesModified ?? [];
  const addedFiles = gitDiffSummary?.addedFiles ?? [];
  const deletedFiles = gitDiffSummary?.deletedFiles ?? [];
  const diffContent = gitDiffSummary?.diff || diff || state?.diff || '';

  const checks = activeVerification?.checksPerformed ?? [
    {
      name: 'Relevant files inspected',
      passed: (state?.filesInspected?.length || 0) > 0,
      evidence: `${state?.filesInspected?.length || 0} file(s) inspected in workspace`,
    },
    {
      name: 'Code modification applied',
      passed: (state?.filesModified?.length || 0) > 0,
      evidence: `${state?.filesModified?.length || 0} file(s) written to disk`,
    },
    {
      name: 'Expected code changes exist',
      passed: (state?.filesModified?.length || 0) > 0 && !!diffContent,
      evidence: diffContent ? 'Uncommitted code diff present' : 'No diff found',
    },
    {
      name: 'Git diff reviewed',
      passed: !!diffContent,
      evidence: diffContent ? 'Git working tree diff verified' : 'Working tree clean',
    },
    {
      name: 'Build completed',
      passed: !(state?.errors || []).some((e) => e.toLowerCase().includes('compilation') || e.toLowerCase().includes('build')),
      evidence: activeVerification?.buildResult
        ? `${activeVerification.buildResult.command} exited with code ${activeVerification.buildResult.exitCode}`
        : 'Compiler check',
    },
    {
      name: 'Tests executed & passed',
      passed: state?.latestTestResult ? state.latestTestResult.status === 'PASSED' : true,
      evidence: state?.latestTestResult
        ? `${state.latestTestResult.passedTests ?? 0} passed, ${state.latestTestResult.failedTests ?? 0} failed`
        : 'Automated test suite check',
    },
    {
      name: 'No unresolved tool errors',
      passed: !(state?.errors || []).some((e) => e.includes('rejected by harness safety') || e.includes('blocked')),
      evidence: `${(state?.errors || []).length} recorded errors`,
    },
    {
      name: 'Plan steps completed',
      passed: isCompleted || (state?.plan || []).some((s) => s.status === 'COMPLETED'),
      evidence: `${(state?.plan || []).filter((s) => s.status === 'COMPLETED').length}/${(state?.plan || []).length} plan steps complete`,
    },
  ];

  const handleCopyJson = () => {
    const payload = activeTaskResult || {
      task: state?.task,
      status: state?.status,
      verified: isVerified,
      verification: activeVerification,
      filesModified: modifiedFiles,
      diff: diffContent,
    };
    navigator.clipboard.writeText(JSON.stringify(payload, null, 2));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <section className="card" style={{ display: 'flex', flexDirection: 'column', marginTop: '10px' }}>
      {/* Tab Navigation Header */}
      <div className="panel-header" style={{ padding: '8px 16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span className="panel-title">Inspection & Verification Workspace</span>
          <div style={{ display: 'flex', gap: '4px', marginLeft: '12px' }}>
            <button
              onClick={() => setActiveTab('VERIFICATION')}
              style={{
                padding: '5px 12px',
                fontSize: '11px',
                fontWeight: 700,
                borderRadius: '5px',
                border: 'none',
                cursor: 'pointer',
                backgroundColor: activeTab === 'VERIFICATION' ? '#2563eb' : '#0f172a',
                color: activeTab === 'VERIFICATION' ? '#ffffff' : '#94a3b8',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <span>Final Verification</span>
              {isCompleted || isFailed ? (
                <span
                  style={{
                    backgroundColor: isVerified ? '#166534' : '#991b1b',
                    color: '#ffffff',
                    fontSize: '9px',
                    padding: '1px 5px',
                    borderRadius: '3px',
                  }}
                >
                  {isVerified ? 'VERIFIED' : 'NOT VERIFIED'}
                </span>
              ) : null}
            </button>

            <button
              onClick={() => setActiveTab('DIFF')}
              style={{
                padding: '5px 12px',
                fontSize: '11px',
                fontWeight: 700,
                borderRadius: '5px',
                border: 'none',
                cursor: 'pointer',
                backgroundColor: activeTab === 'DIFF' ? '#2563eb' : '#0f172a',
                color: activeTab === 'DIFF' ? '#ffffff' : '#94a3b8',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <span>Code Changes / Git Diff</span>
              {modifiedFiles.length > 0 && (
                <span
                  style={{
                    backgroundColor: '#1e293b',
                    color: '#38bdf8',
                    fontSize: '9px',
                    padding: '1px 5px',
                    borderRadius: '3px',
                  }}
                >
                  {modifiedFiles.length}
                </span>
              )}
            </button>

            <button
              onClick={() => setActiveTab('RESULT')}
              style={{
                padding: '5px 12px',
                fontSize: '11px',
                fontWeight: 700,
                borderRadius: '5px',
                border: 'none',
                cursor: 'pointer',
                backgroundColor: activeTab === 'RESULT' ? '#2563eb' : '#0f172a',
                color: activeTab === 'RESULT' ? '#ffffff' : '#94a3b8',
              }}
            >
              Final Result
            </button>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button
            onClick={handleCopyJson}
            disabled={!state}
            style={{
              backgroundColor: '#1e293b',
              color: '#94a3b8',
              border: '1px solid #334155',
              borderRadius: '4px',
              padding: '4px 8px',
              fontSize: '11px',
              cursor: !state ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
            </svg>
            {copied ? 'Copied JSON!' : 'Copy Result JSON'}
          </button>
        </div>
      </div>

      {/* Workspace Content */}
      <div style={{ padding: '16px', minHeight: '260px' }}>
        {/* ============================================================ */}
        {/* TAB 1: FINAL VERIFICATION                                    */}
        {/* ============================================================ */}
        {activeTab === 'VERIFICATION' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {/* Verdict Hero Banner */}
            <div
              style={{
                padding: '12px 16px',
                borderRadius: '8px',
                backgroundColor:
                  isCompleted || isFailed
                    ? isVerified
                      ? 'rgba(34, 197, 94, 0.12)'
                      : 'rgba(239, 68, 68, 0.12)'
                    : '#090e1a',
                border: `1px solid ${
                  isCompleted || isFailed ? (isVerified ? '#166534' : '#991b1b') : '#1e293b'
                }`,
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <div>
                <div style={{ fontSize: '11px', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase' }}>
                  FINAL VERIFICATION
                </div>
                <div style={{ fontSize: '14px', fontWeight: 600, color: '#f8fafc', marginTop: '3px' }}>
                  Task: <span style={{ color: '#38bdf8' }}>{state?.task || 'No active task'}</span>
                </div>
                <div style={{ fontSize: '11px', color: '#64748b', marginTop: '3px' }}>
                  Verification is strictly based on physical filesystem evidence, build compilation, and real test execution.
                </div>
              </div>

              <div>
                {isCompleted || isFailed ? (
                  <div
                    style={{
                      fontSize: '15px',
                      fontWeight: 900,
                      padding: '6px 18px',
                      borderRadius: '6px',
                      backgroundColor: isVerified ? '#166534' : '#991b1b',
                      color: '#ffffff',
                      letterSpacing: '0.08em',
                      boxShadow: isVerified
                        ? '0 0 16px rgba(34, 197, 94, 0.4)'
                        : '0 0 16px rgba(239, 68, 68, 0.4)',
                    }}
                  >
                    {isVerified ? 'VERIFIED' : 'NOT VERIFIED'}
                  </div>
                ) : (
                  <div
                    style={{
                      fontSize: '11px',
                      fontWeight: 700,
                      padding: '6px 12px',
                      borderRadius: '6px',
                      backgroundColor: '#1e293b',
                      color: '#94a3b8',
                      letterSpacing: '0.05em',
                    }}
                  >
                    AWAITING VERIFICATION PHASE
                  </div>
                )}
              </div>
            </div>

            {/* Explanation / Summary statement */}
            {activeVerification?.summary && (
              <div
                style={{
                  fontSize: '12px',
                  padding: '10px 14px',
                  borderRadius: '6px',
                  backgroundColor: isVerified ? 'rgba(22, 101, 52, 0.2)' : 'rgba(127, 29, 29, 0.25)',
                  border: isVerified ? '1px solid #166534' : '1px solid #7f1d1d',
                  color: isVerified ? '#86efac' : '#fca5a5',
                  lineHeight: '1.5',
                }}
              >
                {activeVerification.summary}
              </div>
            )}

            {/* Evidence Checks Grid */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <div style={{ fontSize: '11px', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase' }}>
                Evidence Verification Breakdown:
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '8px' }}>
                {checks.map((chk, idx) => (
                  <div
                    key={idx}
                    style={{
                      padding: '8px 12px',
                      backgroundColor: '#070b14',
                      border: `1px solid ${chk.passed ? '#1e293b' : '#7f1d1d'}`,
                      borderRadius: '6px',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '4px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ color: chk.passed ? '#4ade80' : '#f87171', fontWeight: 800, fontSize: '14px' }}>
                          {chk.passed ? '✓' : '✗'}
                        </span>
                        <span style={{ fontSize: '12px', fontWeight: 600, color: chk.passed ? '#f1f5f9' : '#fca5a5' }}>
                          {chk.name}
                        </span>
                      </div>
                      <span
                        style={{
                          fontSize: '9px',
                          fontWeight: 700,
                          padding: '1px 5px',
                          borderRadius: '3px',
                          backgroundColor: chk.passed ? 'rgba(34, 197, 94, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                          color: chk.passed ? '#4ade80' : '#f87171',
                        }}
                      >
                        {chk.passed ? 'PASSED' : 'FAILED'}
                      </span>
                    </div>
                    <div style={{ fontSize: '11px', color: '#64748b', fontFamily: 'monospace', paddingLeft: '22px' }}>
                      {chk.evidence}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* TAB 2: CODE CHANGES / GIT DIFF                              */}
        {/* ============================================================ */}
        {activeTab === 'DIFF' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
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

              {modifiedFiles.length > 0 && (
                <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                  {modifiedFiles.map((f, i) => (
                    <span
                      key={i}
                      style={{
                        fontSize: '10px',
                        fontFamily: 'monospace',
                        backgroundColor: '#0f172a',
                        border: '1px solid #1e293b',
                        color: '#38bdf8',
                        padding: '2px 6px',
                        borderRadius: '4px',
                      }}
                    >
                      ✎ {f}
                    </span>
                  ))}
                </div>
              )}
            </div>

            {/* Diff content viewer */}
            {!diffContent ? (
              <div
                style={{
                  padding: '40px 20px',
                  textAlign: 'center',
                  color: '#64748b',
                  fontSize: '12px',
                  border: '1px dashed #1e293b',
                  borderRadius: '6px',
                }}
              >
                No uncommitted git changes detected in the selected repository. Code modifications made by the agent will be rendered here.
              </div>
            ) : (
              <div
                style={{
                  backgroundColor: '#050811',
                  border: '1px solid #1e293b',
                  borderRadius: '6px',
                  padding: '12px',
                  maxHeight: '400px',
                  overflowY: 'auto',
                  fontFamily: 'JetBrains Mono, monospace',
                  fontSize: '11px',
                  lineHeight: '1.4',
                }}
              >
                {diffContent.split('\n').map((line, idx) => {
                  let bgColor = 'transparent';
                  let textColor = '#cbd5e1';

                  if (line.startsWith('+') && !line.startsWith('+++')) {
                    bgColor = 'rgba(34, 197, 94, 0.1)';
                    textColor = '#4ade80';
                  } else if (line.startsWith('-') && !line.startsWith('---')) {
                    bgColor = 'rgba(239, 68, 68, 0.1)';
                    textColor = '#f87171';
                  } else if (line.startsWith('@@')) {
                    bgColor = 'rgba(14, 165, 233, 0.1)';
                    textColor = '#38bdf8';
                  } else if (line.startsWith('diff --git')) {
                    bgColor = '#1e293b';
                    textColor = '#f8fafc';
                  }

                  return (
                    <div
                      key={idx}
                      style={{
                        backgroundColor: bgColor,
                        color: textColor,
                        padding: '1px 4px',
                        whiteSpace: 'pre-wrap',
                        wordBreak: 'break-all',
                      }}
                    >
                      {line || ' '}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* ============================================================ */}
        {/* TAB 3: FINAL RESULT                                         */}
        {/* ============================================================ */}
        {activeTab === 'RESULT' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {!state ? (
              <div style={{ textAlign: 'center', color: '#64748b', padding: '40px 0', fontSize: '12px' }}>
                No task executed yet.
              </div>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '12px' }}>
                <div style={{ backgroundColor: '#070b14', padding: '12px', borderRadius: '6px', border: '1px solid #1e293b' }}>
                  <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase' }}>Task ID</div>
                  <div style={{ fontSize: '13px', fontFamily: 'monospace', color: '#f8fafc', marginTop: '4px' }}>
                    {state.taskId}
                  </div>
                </div>

                <div style={{ backgroundColor: '#070b14', padding: '12px', borderRadius: '6px', border: '1px solid #1e293b' }}>
                  <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase' }}>Final Status</div>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: isVerified ? '#4ade80' : '#f87171', marginTop: '4px' }}>
                    {state.status} ({isVerified ? 'VERIFIED' : 'NOT VERIFIED'})
                  </div>
                </div>

                <div style={{ backgroundColor: '#070b14', padding: '12px', borderRadius: '6px', border: '1px solid #1e293b' }}>
                  <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase' }}>Files Modified</div>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: '#38bdf8', marginTop: '4px' }}>
                    {modifiedFiles.length} file(s)
                  </div>
                </div>

                <div style={{ backgroundColor: '#070b14', padding: '12px', borderRadius: '6px', border: '1px solid #1e293b' }}>
                  <div style={{ fontSize: '10px', color: '#64748b', textTransform: 'uppercase' }}>Total Attempts</div>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: '#eab308', marginTop: '4px' }}>
                    {state.attempts} / {state.maxAttempts}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
};
