import React, { useState } from 'react';

interface Props {
  filesInspected: string[];
  filesModified: string[];
  diff?: string;
}

export const FilesChangedViewer: React.FC<Props> = ({
  filesInspected,
  filesModified,
  diff,
}) => {
  const [activeTab, setActiveTab] = useState<'modified' | 'inspected' | 'diff'>('modified');

  return (
    <div className="card" style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3 style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Repository Changes & Inspection
        </h3>
        <div style={{ display: 'flex', gap: '4px' }}>
          <button
            onClick={() => setActiveTab('modified')}
            style={{
              padding: '4px 8px',
              fontSize: '11px',
              borderRadius: '4px',
              border: 'none',
              cursor: 'pointer',
              backgroundColor: activeTab === 'modified' ? '#2563eb' : '#1e293b',
              color: activeTab === 'modified' ? '#ffffff' : '#94a3b8',
            }}
          >
            Modified ({filesModified.length})
          </button>
          <button
            onClick={() => setActiveTab('inspected')}
            style={{
              padding: '4px 8px',
              fontSize: '11px',
              borderRadius: '4px',
              border: 'none',
              cursor: 'pointer',
              backgroundColor: activeTab === 'inspected' ? '#2563eb' : '#1e293b',
              color: activeTab === 'inspected' ? '#ffffff' : '#94a3b8',
            }}
          >
            Inspected ({filesInspected.length})
          </button>
          <button
            onClick={() => setActiveTab('diff')}
            style={{
              padding: '4px 8px',
              fontSize: '11px',
              borderRadius: '4px',
              border: 'none',
              cursor: 'pointer',
              backgroundColor: activeTab === 'diff' ? '#2563eb' : '#1e293b',
              color: activeTab === 'diff' ? '#ffffff' : '#94a3b8',
            }}
          >
            Git Diff
          </button>
        </div>
      </div>

      <div
        style={{
          maxHeight: '260px',
          overflowY: 'auto',
          backgroundColor: '#090d16',
          borderRadius: '6px',
          padding: '12px',
          fontFamily: 'monospace',
          fontSize: '12px',
          border: '1px solid #1e293b',
        }}
      >
        {activeTab === 'modified' && (
          <div>
            {filesModified.length === 0 ? (
              <div style={{ color: '#64748b', textAlign: 'center', padding: '20px' }}>
                No files modified yet.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {filesModified.map((f, i) => (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#4ade80' }}>
                    <span>✎</span>
                    <span>{f}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'inspected' && (
          <div>
            {filesInspected.length === 0 ? (
              <div style={{ color: '#64748b', textAlign: 'center', padding: '20px' }}>
                No files inspected yet.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {filesInspected.map((f, i) => (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#94a3b8' }}>
                    <span>👁</span>
                    <span>{f}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'diff' && (
          <div>
            {!diff ? (
              <div style={{ color: '#64748b', textAlign: 'center', padding: '20px' }}>
                No uncommitted git diff detected.
              </div>
            ) : (
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                {diff.split('\n').map((line, idx) => {
                  let color = '#cbd5e1';
                  if (line.startsWith('+')) color = '#4ade80';
                  else if (line.startsWith('-')) color = '#f87171';
                  else if (line.startsWith('@@')) color = '#38bdf8';
                  return (
                    <div key={idx} style={{ color }}>
                      {line}
                    </div>
                  );
                })}
              </pre>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
