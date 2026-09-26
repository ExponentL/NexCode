import React from 'react';

interface Props {
  activeTab: 'Dashboard' | 'Repositories';
  onTabChange: (tab: 'Dashboard' | 'Repositories') => void;
}

export const CodeAgentSidebar: React.FC<Props> = ({ activeTab, onTabChange }) => {
  const navItems = [
    {
      id: 'Dashboard' as const,
      label: 'Dashboard',
      icon: (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          <polyline points="9 22 9 12 15 12 15 22" />
        </svg>
      ),
    },
    {
      id: 'Repositories' as const,
      label: 'Repositories',
      icon: (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
        </svg>
      ),
    },
  ];

  return (
    <nav
      style={{
        width: '180px',
        flexShrink: 0,
        backgroundColor: '#080c14',
        borderRight: '1px solid #141c2e',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        padding: '16px 10px',
        boxSizing: 'border-box',
        minHeight: 'calc(100vh - 56px)',
      }}
    >
      {/* Top Nav List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
        {navItems.map((item) => {
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onTabChange(item.id)}
              style={{
                width: '100%',
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                padding: '10px 14px',
                borderRadius: '8px',
                border: 'none',
                backgroundColor: isActive ? '#1d4ed8' : 'transparent',
                color: isActive ? '#ffffff' : '#94a3b8',
                fontWeight: isActive ? 600 : 500,
                fontSize: '13px',
                cursor: 'pointer',
                textAlign: 'left',
                transition: 'all 0.15s ease',
              }}
              onMouseEnter={(e) => {
                if (!isActive) e.currentTarget.style.backgroundColor = '#0f172a';
              }}
              onMouseLeave={(e) => {
                if (!isActive) e.currentTarget.style.backgroundColor = 'transparent';
              }}
            >
              <span style={{ color: isActive ? '#ffffff' : '#64748b' }}>{item.icon}</span>
              <span>{item.label}</span>
            </button>
          );
        })}
      </div>

      {/* Bottom Status Pill */}
      <div
        style={{
          padding: '10px 12px',
          borderRadius: '8px',
          backgroundColor: '#0c1322',
          border: '1px solid #162035',
          display: 'flex',
          flexDirection: 'column',
          gap: '3px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span
            style={{
              width: '7px',
              height: '7px',
              borderRadius: '50%',
              backgroundColor: '#10b981',
              boxShadow: '0 0 8px rgba(16, 185, 129, 0.6)',
              display: 'inline-block',
            }}
          />
          <span style={{ fontSize: '11px', fontWeight: 600, color: '#e2e8f0' }}>
            System Online
          </span>
        </div>
        <span style={{ fontSize: '10px', color: '#64748b', paddingLeft: '13px', fontFamily: 'monospace' }}>
          v1.0.0
        </span>
      </div>
    </nav>
  );
};
