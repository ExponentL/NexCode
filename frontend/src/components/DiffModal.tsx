import React from 'react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  diff: string;
  filesModified?: string[];
}

export const DiffModal: React.FC<Props> = ({ isOpen, onClose, diff, filesModified = [] }) => {
  if (!isOpen) return null;

  const content = diff && diff.trim() ? diff.trim() : 'No git diff recorded for this task.';

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(4px)',
        zIndex: 100,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px',
      }}
      onClick={onClose}
    >
      <div
        className="code-agent-card"
        style={{
          width: '100%',
          maxWidth: '860px',
          maxHeight: '85vh',
          display: 'flex',
          flexDirection: 'column',
          backgroundColor: '#0b1120',
          border: '1px solid #1e293b',
          borderRadius: '10px',
          overflow: 'hidden',
          boxShadow: '0 20px 40px rgba(0, 0, 0, 0.6)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div
          style={{
            padding: '14px 20px',
            borderBottom: '1px solid #192237',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            backgroundColor: '#080c14',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
            </svg>
            <span style={{ fontSize: '14px', fontWeight: 600, color: '#f8fafc' }}>
              Git Diff Viewer {filesModified.length > 0 && `(${filesModified.length} files)`}
            </span>
          </div>

          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: '#94a3b8',
              fontSize: '18px',
              cursor: 'pointer',
              padding: '2px 6px',
            }}
          >
            ✕
          </button>
        </div>

        {/* Diff Code Container */}
        <div
          style={{
            padding: '16px 20px',
            overflowY: 'auto',
            backgroundColor: '#050811',
            fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
            fontSize: '12px',
            lineHeight: '1.5',
          }}
        >
          {content.split('\n').map((line, idx) => {
            let bgColor = 'transparent';
            let textColor = '#cbd5e1';

            if (line.startsWith('+') && !line.startsWith('+++')) {
              bgColor = 'rgba(16, 185, 129, 0.12)';
              textColor = '#34d399';
            } else if (line.startsWith('-') && !line.startsWith('---')) {
              bgColor = 'rgba(239, 68, 68, 0.12)';
              textColor = '#f87171';
            } else if (line.startsWith('@@')) {
              bgColor = 'rgba(37, 99, 235, 0.12)';
              textColor = '#60a5fa';
            } else if (line.startsWith('diff --git')) {
              bgColor = '#0f172a';
              textColor = '#f8fafc';
            }

            return (
              <div
                key={idx}
                style={{
                  backgroundColor: bgColor,
                  color: textColor,
                  padding: '1px 6px',
                  borderRadius: '2px',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-all',
                }}
              >
                {line || ' '}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default DiffModal;
