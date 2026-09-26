import React from 'react';
import { RepositoryInfo } from '../types/agent';

interface Props {
  repositories: RepositoryInfo[];
  selectedRepo: string;
  onSelect: (path: string) => void;
  onRefresh: () => void;
  disabled?: boolean;
}

export const RepoSelector: React.FC<Props> = ({
  repositories,
  selectedRepo,
  onSelect,
  onRefresh,
  disabled,
}) => {
  return (
    <div className="card" style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <label style={{ fontSize: '12px', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Target Repository
        </label>
        <button
          onClick={onRefresh}
          disabled={disabled}
          title="Refresh repository list"
          style={{
            background: 'transparent',
            border: 'none',
            color: '#64748b',
            cursor: 'pointer',
            fontSize: '12px',
            display: 'flex',
            alignItems: 'center',
            gap: '4px',
            padding: '2px 6px',
            borderRadius: '4px',
          }}
        >
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67" />
          </svg>
          Refresh
        </button>
      </div>

      <div style={{ position: 'relative' }}>
        <select
          value={selectedRepo}
          onChange={(e) => onSelect(e.target.value)}
          disabled={disabled}
          style={{
            width: '100%',
            padding: '10px 12px',
            backgroundColor: '#0f172a',
            border: '1px solid #334155',
            borderRadius: '6px',
            color: '#f8fafc',
            fontFamily: 'monospace',
            fontSize: '13px',
            cursor: disabled ? 'not-allowed' : 'pointer',
            outline: 'none',
          }}
        >
          {repositories.map((repo) => (
            <option key={repo.path} value={repo.path}>
              {repo.name} [{repo.type}] - {repo.path}
            </option>
          ))}
          {repositories.length === 0 && (
            <option value="">No repositories found (using current directory)</option>
          )}
        </select>
      </div>

      {selectedRepo && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '11px', color: '#64748b' }}>
          <span style={{ color: '#38bdf8' }}>● Active:</span>
          <span style={{ fontFamily: 'monospace', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {selectedRepo}
          </span>
        </div>
      )}
    </div>
  );
};
