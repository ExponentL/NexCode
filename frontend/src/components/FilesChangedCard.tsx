import React from 'react';

interface Props {
  filesModified: string[];
  diff?: string;
  onViewDiff: () => void;
}

export const FilesChangedCard: React.FC<Props> = ({ filesModified, diff, onViewDiff }) => {
  const hasDiff = diff && diff.trim().length > 0;

  return (
    <div className="code-agent-card" style={{ padding: '14px 18px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
          </svg>
          <span style={{ fontSize: '13px', fontWeight: 600, color: '#f8fafc' }}>
            Files Changed
          </span>
        </div>

        {filesModified.length > 0 && (
          <span
            style={{
              padding: '2px 8px',
              borderRadius: '9999px',
              fontSize: '11px',
              fontWeight: 500,
              backgroundColor: '#162035',
              color: '#38bdf8',
            }}
          >
            {filesModified.length} file{filesModified.length > 1 ? 's' : ''}
          </span>
        )}
      </div>

      {/* Files List or Empty State */}
      {filesModified.length === 0 ? (
        <div style={{ padding: '10px 0', textAlign: 'center', color: '#64748b', fontSize: '12px' }}>
          No changes yet
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {filesModified.map((filePath, idx) => (
            <div
              key={idx}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                fontSize: '11px',
                gap: '6px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', overflow: 'hidden' }}>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2" style={{ flexShrink: 0 }}>
                  <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
                  <polyline points="13 2 13 9 20 9" />
                </svg>
                <span
                  style={{
                    color: '#94a3b8',
                    fontFamily: 'monospace',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                  title={filePath}
                >
                  {filePath}
                </span>
              </div>
              <span style={{ color: '#34d399', fontSize: '10px', fontWeight: 600 }}>MODIFIED</span>
            </div>
          ))}

          {/* View Diff Button */}
          <button
            type="button"
            onClick={onViewDiff}
            disabled={!hasDiff}
            style={{
              width: '100%',
              marginTop: '4px',
              padding: '7px 12px',
              borderRadius: '6px',
              backgroundColor: '#0c1322',
              border: '1px solid #192237',
              color: hasDiff ? '#38bdf8' : '#64748b',
              fontSize: '12px',
              fontWeight: 500,
              cursor: hasDiff ? 'pointer' : 'default',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px',
              transition: 'all 0.15s ease',
            }}
          >
            <span>View Git Diff</span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
              <polyline points="15 3 21 3 21 9" />
              <line x1="10" y1="14" x2="21" y2="3" />
            </svg>
          </button>
        </div>
      )}
    </div>
  );
};

export default FilesChangedCard;
