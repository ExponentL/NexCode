import React from 'react';
import { AgentEvent } from '../types/agent';

interface Props {
  events: AgentEvent[];
}

export const AgentActivityCard: React.FC<Props> = ({ events }) => {
  const formatTime = (ts?: string) => {
    if (!ts) return '--:--';
    try {
      const d = new Date(ts);
      return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}`;
    } catch {
      return '--:--';
    }
  };

  const getEventBadge = (type: string) => {
    if (type.includes('COMPLETED') || type.includes('PASSED')) {
      return { bg: 'rgba(16, 185, 129, 0.15)', border: '#10b981', color: '#34d399' };
    }
    if (type.includes('FAIL') || type.includes('ERROR')) {
      return { bg: 'rgba(239, 68, 68, 0.15)', border: '#ef4444', color: '#f87171' };
    }
    if (type.includes('RECOVERY')) {
      return { bg: 'rgba(245, 158, 11, 0.15)', border: '#f59e0b', color: '#fbbf24' };
    }
    return { bg: 'rgba(37, 99, 235, 0.15)', border: '#2563eb', color: '#60a5fa' };
  };

  return (
    <div className="code-agent-card" style={{ padding: '16px 20px', display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
            <circle cx="12" cy="12" r="10" />
            <path d="M12 16v-4M12 8h.01" />
          </svg>
          <h2 style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: '#f8fafc' }}>
            Agent Activity
          </h2>
        </div>

        {events.length > 0 && (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '5px',
              padding: '2px 8px',
              borderRadius: '9999px',
              fontSize: '11px',
              fontWeight: 600,
              backgroundColor: 'rgba(16, 185, 129, 0.15)',
              color: '#34d399',
              border: '1px solid rgba(16, 185, 129, 0.3)',
            }}
          >
            <span
              style={{
                width: '6px',
                height: '6px',
                borderRadius: '50%',
                backgroundColor: '#10b981',
              }}
            />
            {events.length} event{events.length > 1 ? 's' : ''}
          </span>
        )}
      </div>

      {/* Timeline Stream or Empty State */}
      {events.length === 0 ? (
        <div
          style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '40px 20px',
            color: '#64748b',
            textAlign: 'center',
            gap: '8px',
          }}
        >
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#334155" strokeWidth="1.5">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
          <span style={{ fontSize: '13px', fontWeight: 500, color: '#94a3b8' }}>No activity yet</span>
          <span style={{ fontSize: '12px', maxWidth: '280px', lineHeight: '1.4' }}>
            Live events, file inspections, and execution steps will appear here in real time when you start a task.
          </span>
        </div>
      ) : (
        <div
          style={{
            position: 'relative',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
            overflowY: 'auto',
            maxHeight: '520px',
            paddingRight: '6px',
          }}
        >
          {/* Continuous vertical timeline line */}
          <div
            style={{
              position: 'absolute',
              left: '70px',
              top: '12px',
              bottom: '16px',
              width: '2px',
              backgroundColor: '#162035',
              zIndex: 1,
            }}
          />

          {events.map((e, idx) => {
            const time = formatTime(e.timestamp);
            const badge = getEventBadge(e.type);
            const label = e.type
              .replace(/_/g, ' ')
              .toLowerCase()
              .replace(/\b\w/g, (c) => c.toUpperCase());

            return (
              <div key={idx} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', zIndex: 2 }}>
                <span
                  style={{
                    width: '56px',
                    fontSize: '11px',
                    color: '#64748b',
                    fontFamily: 'monospace',
                    paddingTop: '3px',
                    flexShrink: 0,
                    textAlign: 'right',
                  }}
                >
                  {time}
                </span>

                <div
                  style={{
                    width: '18px',
                    height: '18px',
                    borderRadius: '50%',
                    backgroundColor: badge.bg,
                    border: `1.5px solid ${badge.border}`,
                    color: badge.color,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                    marginTop: '2px',
                  }}
                >
                  <div style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: badge.color }} />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', flex: 1 }}>
                  <span style={{ fontSize: '12px', fontWeight: 600, color: '#f8fafc' }}>
                    {label}
                  </span>
                  <span style={{ fontSize: '11px', color: '#94a3b8', lineHeight: '1.4' }}>
                    {e.message}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default AgentActivityCard;
