import React, { useEffect, useRef } from 'react';
import { AgentEvent } from '../types/agent';

interface Props {
  events: AgentEvent[];
}

export const ActivityFeed: React.FC<Props> = ({ events }) => {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [events]);

  const getEventBadge = (type: string) => {
    if (type.includes('FAIL') || type === 'ERROR') {
      return { bg: 'rgba(239, 68, 68, 0.2)', text: '#f87171' };
    }
    if (type.includes('COMPLETED') || type.includes('PASSED')) {
      return { bg: 'rgba(34, 197, 94, 0.2)', text: '#4ade80' };
    }
    if (type.includes('STEP') || type.includes('COMMAND')) {
      return { bg: 'rgba(14, 165, 233, 0.2)', text: '#38bdf8' };
    }
    if (type.includes('RETRY')) {
      return { bg: 'rgba(236, 72, 153, 0.2)', text: '#f472b6' };
    }
    return { bg: 'rgba(148, 163, 184, 0.2)', text: '#cbd5e1' };
  };

  return (
    <div className="card" style={{ padding: '16px', display: 'flex', flexDirection: 'column', height: '100%', boxSizing: 'border-box' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
        <h3 style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Activity & Event Stream
        </h3>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#22c55e', display: 'inline-block' }} />
          <span style={{ fontSize: '11px', color: '#64748b', fontFamily: 'monospace' }}>Live SSE Feed</span>
        </div>
      </div>

      <div
        style={{
          flex: 1,
          overflowY: 'auto',
          backgroundColor: '#090d16',
          borderRadius: '6px',
          padding: '12px',
          fontFamily: 'JetBrains Mono, monospace',
          fontSize: '12px',
          display: 'flex',
          flexDirection: 'column',
          gap: '8px',
          maxHeight: '400px',
          border: '1px solid #1e293b',
        }}
      >
        {events.length === 0 ? (
          <div style={{ color: '#475569', textAlign: 'center', padding: '30px' }}>
            No agent events yet. Submit a task to begin autonomous harness telemetry.
          </div>
        ) : (
          events.map((evt, idx) => {
            const badge = getEventBadge(evt.type);
            const timeStr = new Date(evt.timestamp).toLocaleTimeString();
            return (
              <div
                key={evt.eventId || idx}
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '8px',
                  padding: '4px 0',
                  borderBottom: '1px solid rgba(255,255,255,0.03)',
                }}
              >
                <span style={{ color: '#475569', fontSize: '10px', minWidth: '60px' }}>{timeStr}</span>
                <span
                  style={{
                    backgroundColor: badge.bg,
                    color: badge.text,
                    padding: '1px 6px',
                    borderRadius: '4px',
                    fontSize: '10px',
                    fontWeight: 600,
                    minWidth: '95px',
                    textAlign: 'center',
                  }}
                >
                  {evt.type}
                </span>
                <span style={{ color: '#e2e8f0', wordBreak: 'break-word', flex: 1 }}>{evt.message}</span>
              </div>
            );
          })
        )}
        <div ref={bottomRef} />
      </div>
    </div>
  );
};
