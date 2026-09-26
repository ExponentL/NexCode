import React, { useEffect, useRef, useState } from 'react';
import { AgentEvent, AgentStatus } from '../types/agent';

interface Props {
  events: AgentEvent[];
  status: AgentStatus;
  currentStep: string;
  hasErrors: boolean;
  recoveryAttempts: number;
}

export const CenterPanel: React.FC<Props> = ({
  events,
  status,
  currentStep,
  hasErrors,
  recoveryAttempts,
}) => {
  const [filter, setFilter] = useState<'ALL' | 'ACTIONS' | 'TESTS' | 'RECOVERY'>('ALL');
  const [autoScroll, setAutoScroll] = useState(true);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (autoScroll && scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [events, autoScroll]);

  // Determine stage progression from real events and status
  const hasEvent = (typePrefix: string) => events.some((e) => e.type.startsWith(typePrefix));

  const pipelineStages = [
    {
      id: 'TASK_RECEIVED',
      label: 'TASK RECEIVED',
      active: status !== 'IDLE',
      done: events.length > 0,
    },
    {
      id: 'REPO_SCANNED',
      label: 'Repository scanned',
      active: status === 'ANALYZING',
      done: hasEvent('REPOSITORY_SCANNED') || hasEvent('SEARCH_') || hasEvent('FILE_READ'),
    },
    {
      id: 'FILES_FOUND',
      label: 'Relevant files found',
      active: status === 'ANALYZING' && hasEvent('SEARCH_PERFORMED'),
      done: hasEvent('FILE_READ') || hasEvent('PLAN_CREATED'),
    },
    {
      id: 'PLAN_CREATED',
      label: 'Plan created',
      active: status === 'PLANNING',
      done: hasEvent('PLAN_CREATED') || hasEvent('MODEL_ACTION') || hasEvent('COMMAND_EXECUTED'),
    },
    {
      id: 'MODEL_ACTION',
      label: 'Model action',
      active: status === 'EXECUTING',
      done: hasEvent('MODEL_ACTION') || hasEvent('MODEL_RESPONSE') || hasEvent('FILE_MODIFIED'),
    },
    {
      id: 'FILE_MODIFIED',
      label: 'File modified',
      active: status === 'EXECUTING' && hasEvent('FILE_MODIFIED'),
      done: hasEvent('FILE_MODIFIED') || hasEvent('TEST_'),
    },
    {
      id: 'TESTS_RUNNING',
      label: 'Tests running',
      active: status === 'TESTING',
      done: hasEvent('TEST_STARTED') || hasEvent('TEST_PASSED') || hasEvent('TEST_FAILED'),
    },
    {
      id: 'FAILURE_DETECTED',
      label: 'Failure detected',
      active: hasErrors || status === 'RECOVERING' || hasEvent('TEST_FAILED'),
      done: hasEvent('TEST_FAILED') || hasEvent('RECOVERY_STARTED'),
      isBranch: true,
      failed: hasErrors || hasEvent('TEST_FAILED'),
    },
    {
      id: 'RECOVERY_ATTEMPT',
      label: 'Recovery attempt',
      active: status === 'RECOVERING' || recoveryAttempts > 0,
      done: hasEvent('RECOVERY_ATTEMPT') || hasEvent('TEST_PASSED'),
      isBranch: true,
    },
    {
      id: 'TESTS_PASSED',
      label: 'Tests passed',
      active: status === 'VERIFYING' || (status === 'COMPLETED' && !hasErrors),
      done: hasEvent('TEST_PASSED') || status === 'COMPLETED',
    },
    {
      id: 'VERIFICATION',
      label: 'Verification',
      active: status === 'VERIFYING',
      done: status === 'COMPLETED' || status === 'FAILED',
    },
  ];

  const filteredEvents = events.filter((evt) => {
    if (filter === 'ALL') return true;
    if (filter === 'ACTIONS') {
      return (
        evt.type.includes('FILE') ||
        evt.type.includes('SEARCH') ||
        evt.type.includes('COMMAND') ||
        evt.type.includes('MODEL')
      );
    }
    if (filter === 'TESTS') {
      return evt.type.includes('TEST');
    }
    if (filter === 'RECOVERY') {
      return evt.type.includes('RECOVERY') || evt.type.includes('FAIL') || evt.type.includes('ERROR');
    }
    return true;
  });

  const getEventBadge = (type: string) => {
    if (type.includes('FAIL') || type.includes('ERROR')) {
      return { bg: 'rgba(239, 68, 68, 0.2)', text: '#f87171', border: '#b91c1c' };
    }
    if (type.includes('COMPLETED') || type.includes('PASSED')) {
      return { bg: 'rgba(34, 197, 94, 0.2)', text: '#4ade80', border: '#15803d' };
    }
    if (type.includes('RECOVERY') || type.includes('RETRY')) {
      return { bg: 'rgba(236, 72, 153, 0.2)', text: '#f472b6', border: '#be185d' };
    }
    if (type.includes('TEST')) {
      return { bg: 'rgba(59, 130, 246, 0.2)', text: '#60a5fa', border: '#1d4ed8' };
    }
    if (type.includes('FILE_MODIFIED')) {
      return { bg: 'rgba(16, 185, 129, 0.2)', text: '#34d399', border: '#059669' };
    }
    if (type.includes('STEP') || type.includes('COMMAND')) {
      return { bg: 'rgba(14, 165, 233, 0.2)', text: '#38bdf8', border: '#0284c7' };
    }
    return { bg: 'rgba(148, 163, 184, 0.15)', text: '#cbd5e1', border: '#334155' };
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', height: '100%' }}>
      {/* 1. Visual Pipeline Stage Stepper */}
      <div className="card" style={{ padding: '14px 16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#38bdf8" strokeWidth="2">
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
            </svg>
            <span className="panel-title">Autonomous Execution Pipeline</span>
          </div>
          <span style={{ fontSize: '11px', color: '#64748b', fontFamily: 'monospace' }}>
            Current: <strong style={{ color: '#cbd5e1' }}>{currentStep || status}</strong>
          </span>
        </div>

        {/* Pipeline Nodes in Sequence */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '4px',
            overflowX: 'auto',
            paddingBottom: '4px',
          }}
        >
          {pipelineStages.map((stage, idx) => {
            const isLast = idx === pipelineStages.length - 1;
            let bgColor = '#0f172a';
            let textColor = '#64748b';
            let borderColor = '#1e293b';

            if (stage.done) {
              bgColor = 'rgba(34, 197, 94, 0.1)';
              textColor = '#4ade80';
              borderColor = '#166534';
            }
            if (stage.active) {
              bgColor = 'rgba(14, 165, 233, 0.15)';
              textColor = '#38bdf8';
              borderColor = '#0284c7';
            }
            if (stage.isBranch && stage.failed) {
              bgColor = 'rgba(239, 68, 68, 0.15)';
              textColor = '#f87171';
              borderColor = '#991b1b';
            }

            return (
              <React.Fragment key={stage.id}>
                <div
                  className={`pipeline-node ${stage.active ? 'active' : ''}`}
                  style={{
                    backgroundColor: bgColor,
                    border: `1px solid ${borderColor}`,
                    color: textColor,
                    padding: '6px 10px',
                    borderRadius: '6px',
                    fontSize: '11px',
                    fontWeight: 600,
                    whiteSpace: 'nowrap',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '5px',
                  }}
                >
                  {stage.done ? (
                    <span style={{ color: '#4ade80', fontSize: '11px' }}>✓</span>
                  ) : stage.active ? (
                    <span
                      style={{
                        width: '6px',
                        height: '6px',
                        borderRadius: '50%',
                        backgroundColor: '#38bdf8',
                        animation: 'pulse-glow 1.2s infinite',
                      }}
                    />
                  ) : (
                    <span style={{ color: '#475569', fontSize: '9px' }}>○</span>
                  )}
                  <span>{stage.label}</span>
                </div>
                {!isLast && (
                  <span style={{ color: stage.done ? '#4ade80' : '#334155', fontSize: '11px', userSelect: 'none' }}>
                    →
                  </span>
                )}
              </React.Fragment>
            );
          })}
        </div>
      </div>

      {/* 2. Live Agent Activity Feed */}
      <div className="card" style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: '440px' }}>
        <div className="panel-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="panel-title">Agent Activity Feed</span>
            <span
              style={{
                fontSize: '10px',
                fontFamily: 'monospace',
                backgroundColor: '#1e293b',
                color: '#cbd5e1',
                padding: '1px 6px',
                borderRadius: '4px',
              }}
            >
              {events.length} events
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {/* Filter buttons */}
            <div style={{ display: 'flex', gap: '4px' }}>
              {(['ALL', 'ACTIONS', 'TESTS', 'RECOVERY'] as const).map((t) => (
                <button
                  key={t}
                  onClick={() => setFilter(t)}
                  style={{
                    fontSize: '10px',
                    padding: '2px 7px',
                    borderRadius: '4px',
                    border: 'none',
                    cursor: 'pointer',
                    backgroundColor: filter === t ? '#2563eb' : '#1e293b',
                    color: filter === t ? '#ffffff' : '#94a3b8',
                  }}
                >
                  {t}
                </button>
              ))}
            </div>

            <button
              onClick={() => setAutoScroll(!autoScroll)}
              style={{
                background: 'none',
                border: '1px solid #334155',
                borderRadius: '4px',
                padding: '2px 6px',
                fontSize: '10px',
                color: autoScroll ? '#38bdf8' : '#64748b',
                cursor: 'pointer',
              }}
            >
              {autoScroll ? 'Auto-scroll ON' : 'Auto-scroll OFF'}
            </button>
          </div>
        </div>

        {/* Scrollable event list */}
        <div
          ref={scrollRef}
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: '12px',
            backgroundColor: '#070b14',
            display: 'flex',
            flexDirection: 'column',
            gap: '8px',
            fontFamily: 'JetBrains Mono, monospace',
            fontSize: '11px',
            maxHeight: '520px',
          }}
        >
          {filteredEvents.length === 0 ? (
            <div
              style={{
                margin: 'auto',
                textAlign: 'center',
                color: '#475569',
                padding: '40px 20px',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '10px',
              }}
            >
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#334155" strokeWidth="1.5">
                <circle cx="12" cy="12" r="10" />
                <path d="M12 6v6l4 2" />
              </svg>
              <span>Autonomous agent harness is ready. Select a repository, enter a task, and click Start Task.</span>
            </div>
          ) : (
            filteredEvents.map((evt, idx) => {
              const badge = getEventBadge(evt.type);
              const timeStr = new Date(evt.timestamp).toLocaleTimeString();
              const hasDetails = evt.details && Object.keys(evt.details).length > 0;

              return (
                <div
                  key={evt.eventId || idx}
                  style={{
                    backgroundColor: '#0c111e',
                    border: '1px solid #1e293b',
                    borderRadius: '5px',
                    padding: '8px 10px',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '4px',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ color: '#64748b', fontSize: '10px' }}>{timeStr}</span>
                      <span
                        style={{
                          backgroundColor: badge.bg,
                          color: badge.text,
                          border: `1px solid ${badge.border}`,
                          padding: '1px 6px',
                          borderRadius: '4px',
                          fontSize: '10px',
                          fontWeight: 700,
                          letterSpacing: '0.04em',
                        }}
                      >
                        {evt.type}
                      </span>
                    </div>
                  </div>

                  {/* Concise action description without hidden chain of thought */}
                  <div style={{ color: '#e2e8f0', lineHeight: '1.4', fontSize: '11px', wordBreak: 'break-word' }}>
                    {evt.message}
                  </div>

                  {/* Concise details payload if relevant */}
                  {hasDetails && (
                    <div
                      style={{
                        marginTop: '2px',
                        fontSize: '10px',
                        color: '#94a3b8',
                        backgroundColor: '#080c16',
                        padding: '4px 6px',
                        borderRadius: '3px',
                        borderLeft: '2px solid #334155',
                        overflowX: 'auto',
                      }}
                    >
                      {Object.entries(evt.details)
                        .filter(([k]) => k !== 'hidden' && k !== 'chainOfThought')
                        .map(([k, v]) => (
                          <span key={k} style={{ marginRight: '10px' }}>
                            <strong style={{ color: '#64748b' }}>{k}:</strong>{' '}
                            <span style={{ color: '#cbd5e1' }}>
                              {typeof v === 'object' ? JSON.stringify(v) : String(v)}
                            </span>
                          </span>
                        ))}
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
