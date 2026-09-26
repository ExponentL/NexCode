import React from 'react';
import { RepositoryAnalysis } from '../types/agent';

interface Props {
  analysis: RepositoryAnalysis | null;
  loading?: boolean;
}

export const RepositoryInfoCard: React.FC<Props> = ({ analysis, loading }) => {
  return (
    <div className="code-agent-card" style={{ padding: '14px 18px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
            <ellipse cx="12" cy="5" rx="9" ry="3" />
            <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3" />
            <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5" />
          </svg>
          <span style={{ fontSize: '13px', fontWeight: 600, color: '#f8fafc' }}>
            Repository Info
          </span>
        </div>

        {analysis && (
          <span
            style={{
              padding: '2px 8px',
              borderRadius: '9999px',
              fontSize: '11px',
              fontWeight: 500,
              backgroundColor: analysis.exists ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
              color: analysis.exists ? '#34d399' : '#f87171',
              border: `1px solid ${analysis.exists ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`,
            }}
          >
            {analysis.exists ? 'Verified on Disk' : 'Inaccessible'}
          </span>
        )}
      </div>

      {loading ? (
        <div style={{ padding: '12px 0', fontSize: '12px', color: '#64748b', textAlign: 'center' }}>
          Inspecting local filesystem...
        </div>
      ) : !analysis ? (
        <div style={{ padding: '12px 0', fontSize: '12px', color: '#64748b', textAlign: 'center' }}>
          No repository selected
        </div>
      ) : (
        /* Real Info Rows */
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '12px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ color: '#64748b' }}>Name</span>
            <span style={{ color: '#f8fafc', fontWeight: 500 }}>{analysis.name}</span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ color: '#64748b' }}>Type</span>
            <span style={{ color: '#f8fafc', fontWeight: 500 }}>{analysis.projectType}</span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ color: '#64748b' }}>Build System</span>
            <span style={{ color: '#f8fafc', fontWeight: 500 }}>{analysis.buildSystem}</span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ color: '#64748b' }}>Languages</span>
            <span style={{ color: '#cbd5e1' }}>
              {analysis.detectedLanguages.length > 0 ? analysis.detectedLanguages.join(', ') : 'None detected'}
            </span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ color: '#64748b' }}>Git Status</span>
            <span style={{ color: analysis.isGit ? '#38bdf8' : '#64748b', fontWeight: 500 }}>
              {analysis.isGit ? `${analysis.gitBranch ? analysis.gitBranch + ' • ' : ''}${analysis.gitStatus}` : 'Git repository not detected'}
            </span>
          </div>

          {analysis.sourceDirectories.length > 0 && (
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: '#64748b' }}>Source Dirs</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace', fontSize: '11px' }}>
                {analysis.sourceDirectories.join(', ')}
              </span>
            </div>
          )}

          {analysis.testDirectories.length > 0 && (
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: '#64748b' }}>Test Dirs</span>
              <span style={{ color: '#cbd5e1', fontFamily: 'monospace', fontSize: '11px' }}>
                {analysis.testDirectories.join(', ')}
              </span>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ color: '#64748b' }}>Total Files</span>
            <span style={{ color: '#f8fafc', fontFamily: 'monospace' }}>{analysis.totalFiles}</span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', overflow: 'hidden' }}>
            <span style={{ color: '#64748b', flexShrink: 0 }}>Path</span>
            <span
              style={{
                color: '#94a3b8',
                fontFamily: 'monospace',
                fontSize: '11px',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                maxWidth: '180px',
              }}
              title={analysis.path}
            >
              {analysis.path}
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

export default RepositoryInfoCard;
