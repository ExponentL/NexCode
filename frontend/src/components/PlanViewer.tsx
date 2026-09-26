import React from 'react';
import { PlanStep } from '../types/agent';

interface Props {
  plan: PlanStep[];
  currentStep: string;
}

export const PlanViewer: React.FC<Props> = ({ plan, currentStep }) => {
  const getStepIcon = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return <span style={{ color: '#4ade80' }}>✓</span>;
      case 'IN_PROGRESS':
        return <span style={{ color: '#38bdf8', animation: 'spin 1.5s linear infinite' }}>⟳</span>;
      case 'FAILED':
        return <span style={{ color: '#f87171' }}>✗</span>;
      default:
        return <span style={{ color: '#64748b' }}>○</span>;
    }
  };

  const getPhaseBadge = (action?: string) => {
    if (!action) return null;
    return (
      <span
        style={{
          fontSize: '9px',
          fontWeight: 700,
          backgroundColor: '#1e293b',
          color: '#94a3b8',
          padding: '2px 6px',
          borderRadius: '4px',
          fontFamily: 'monospace',
          border: '1px solid #334155',
        }}
      >
        {action}
      </span>
    );
  };

  return (
    <div className="card" style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3 style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Machine-Readable Implementation Plan
        </h3>
        <span style={{ fontSize: '11px', color: '#64748b', fontFamily: 'monospace' }}>
          {plan.length} phases structured
        </span>
      </div>

      {plan.length === 0 ? (
        <div style={{ padding: '24px', textAlign: 'center', color: '#64748b', fontSize: '12px', border: '1px dashed #334155', borderRadius: '6px' }}>
          Agent will synthesize an execution plan upon inspecting the codebase.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '360px', overflowY: 'auto' }}>
          {plan.map((step) => {
            const isCurrent = currentStep.toLowerCase().includes(step.title.toLowerCase());
            return (
              <div
                key={step.stepNumber}
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '10px',
                  padding: '10px 12px',
                  borderRadius: '6px',
                  backgroundColor: isCurrent ? 'rgba(56, 189, 248, 0.08)' : '#0f172a',
                  border: isCurrent ? '1px solid #0284c7' : '1px solid #1e293b',
                  transition: 'all 0.2s ease',
                }}
              >
                <div style={{ fontSize: '14px', lineHeight: '1.2' }}>{getStepIcon(step.status)}</div>
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                    <span style={{ fontSize: '11px', fontFamily: 'monospace', color: '#64748b', fontWeight: 600 }}>
                      Phase {step.stepNumber}
                    </span>
                    <span style={{ fontSize: '12px', fontWeight: 600, color: '#f1f5f9' }}>
                      {step.title}
                    </span>
                    {getPhaseBadge(step.action)}
                  </div>
                  {step.description && (
                    <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: '3px', lineHeight: '1.4' }}>
                      {step.description}
                    </div>
                  )}
                  {step.resultSummary && (
                    <div style={{ fontSize: '10px', color: '#38bdf8', marginTop: '3px', fontFamily: 'monospace' }}>
                      ↳ {step.resultSummary}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
