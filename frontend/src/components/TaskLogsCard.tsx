import React from 'react';
import { AgentEvent } from '../types/agent';

interface Props {
  events: AgentEvent[];
}

export const TaskLogsCard: React.FC<Props> = ({ events }) => {
  return (
    <div className="code-agent-card" style={{ padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
          <polyline points="4 17 10 11 4 5" />
          <line x1="12" y1="19" x2="20" y2="19" />
        </svg>
        <span style={{ fontSize: '13px', fontWeight: 600, color: '#f8fafc' }}>
          Live Task Execution Log
        </span>
      </div>

      {/* Terminal View */}
      <div
        style={{
          backgroundColor: '#050811',
          border: '1px solid #141c2e',
          borderRadius: '8px',
          padding: '12px 14px',
          fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
          fontSize: '11.5px',
          lineHeight: '1.6',
          maxHeight: '180px',
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
          gap: '3px',
        }}
      >
        {events.length === 0 ? (
          <div style={{ color: '#475569', fontStyle: 'italic', padding: '12px 0' }}>
            No logs recorded yet. Execution output from tools and commands will stream here in real time.
          </div>
        ) : (
          events.map((e, idx) => {
            const isErr = e.type.includes('FAIL') || e.type.includes('ERROR');
            let time = '--:--:--';
            try {
              const d = new Date(e.timestamp);
              time = d.toTimeString().split(' ')[0];
            } catch {}

            return (
              <div key={idx} style={{ display: 'flex', alignItems: 'baseline', gap: '8px', whiteSpace: 'pre-wrap' }}>
                <span style={{ color: '#64748b' }}>[{time}]</span>
                <span
                  style={{
                    color: isErr ? '#ef4444' : '#38bdf8',
                    fontWeight: 600,
                    minWidth: '70px',
                  }}
                >
                  {e.type}
                </span>
                <span style={{ color: isErr ? '#fca5a5' : '#cbd5e1' }}>
                  {e.message}
                </span>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

export default TaskLogsCard;
