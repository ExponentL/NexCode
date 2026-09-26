import React from 'react';
import { AgentState, AgentStatus } from '../types/agent';

interface TaskItem {
  id: string;
  title: string;
  repo: string;
  status: AgentStatus;
  timeAgo: string;
}

interface Props {
  tasks: AgentState[];
  activeTaskId?: string;
  onSelectTask: (taskId: string) => void;
}

export const RecentTasksCard: React.FC<Props> = ({ tasks, activeTaskId, onSelectTask }) => {
  const formatTimeAgo = (iso?: string) => {
    if (!iso) return 'just now';
    const diffMs = Date.now() - new Date(iso).getTime();
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    const diffHours = Math.floor(diffMins / 60);
    return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
  };

  const realTasks: TaskItem[] = tasks
    .filter((t) => !t.taskId.startsWith('mock-'))
    .map((t) => ({
      id: t.taskId,
      title: t.task,
      repo: t.repositoryPath?.split('/').pop() || 'workspace',
      status: t.status,
      timeAgo: formatTimeAgo(t.createdAt),
    }));

  const renderStatusBadge = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return (
          <span
            style={{
              padding: '2px 8px',
              borderRadius: '9999px',
              fontSize: '11px',
              fontWeight: 500,
              backgroundColor: 'rgba(16, 185, 129, 0.15)',
              color: '#34d399',
              border: '1px solid rgba(16, 185, 129, 0.3)',
            }}
          >
            Completed
          </span>
        );
      case 'EXECUTING':
      case 'RUNNING':
      case 'ANALYZING':
      case 'PLANNING':
      case 'TESTING':
      case 'VERIFYING':
        return (
          <span
            style={{
              padding: '2px 8px',
              borderRadius: '9999px',
              fontSize: '11px',
              fontWeight: 500,
              backgroundColor: 'rgba(37, 99, 235, 0.15)',
              color: '#60a5fa',
              border: '1px solid rgba(37, 99, 235, 0.3)',
            }}
          >
            Running
          </span>
        );
      case 'FAILED':
        return (
          <span
            style={{
              padding: '2px 8px',
              borderRadius: '9999px',
              fontSize: '11px',
              fontWeight: 500,
              backgroundColor: 'rgba(239, 68, 68, 0.15)',
              color: '#f87171',
              border: '1px solid rgba(239, 68, 68, 0.3)',
            }}
          >
            Failed
          </span>
        );
      default:
        return (
          <span
            style={{
              padding: '2px 8px',
              borderRadius: '9999px',
              fontSize: '11px',
              fontWeight: 500,
              backgroundColor: 'rgba(100, 116, 139, 0.15)',
              color: '#94a3b8',
              border: '1px solid rgba(100, 116, 139, 0.3)',
            }}
          >
            {status}
          </span>
        );
    }
  };

  return (
    <div className="code-agent-card" style={{ padding: '16px 18px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2">
          <path d="M12 20h9" />
          <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
        </svg>
        <h2 style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: '#f8fafc' }}>
          Recent Tasks
        </h2>
      </div>

      {/* Task List or Empty State */}
      {realTasks.length === 0 ? (
        <div style={{ padding: '16px 0', textAlign: 'center', color: '#64748b', fontSize: '12px' }}>
          No previous tasks
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {realTasks.map((item) => {
            const isSelected = activeTaskId === item.id;
            return (
              <div
                key={item.id}
                onClick={() => onSelectTask(item.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '10px 12px',
                  borderRadius: '8px',
                  backgroundColor: isSelected ? '#111a2e' : '#070b14',
                  border: `1px solid ${isSelected ? '#2563eb' : '#141c2e'}`,
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                }}
                onMouseEnter={(e) => {
                  if (!isSelected) e.currentTarget.style.borderColor = '#24324d';
                }}
                onMouseLeave={(e) => {
                  if (!isSelected) e.currentTarget.style.borderColor = '#141c2e';
                }}
              >
                <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0, flex: 1 }}>
                  <span
                    style={{
                      fontSize: '12px',
                      fontWeight: 600,
                      color: '#f8fafc',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                    }}
                    title={item.title}
                  >
                    {item.title}
                  </span>
                  <span style={{ fontSize: '11px', color: '#64748b' }}>
                    {item.repo}
                  </span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexShrink: 0 }}>
                  {renderStatusBadge(item.status)}
                  <span style={{ fontSize: '11px', color: '#64748b' }}>{item.timeAgo}</span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default RecentTasksCard;
