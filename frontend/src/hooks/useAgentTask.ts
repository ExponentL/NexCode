import { useCallback, useEffect, useRef, useState } from 'react';
import { AgentEvent, AgentState, CreateTaskPayload, TaskResult, VerificationResult } from '../types/agent';
import { agentApi } from '../services/api';

export function useAgentTask() {
  const [activeTask, setActiveTask] = useState<AgentState | null>(null);
  const [events, setEvents] = useState<AgentEvent[]>([]);
  const [diff, setDiff] = useState<string>('');
  const [taskResult, setTaskResult] = useState<TaskResult | null>(null);
  const [verificationResult, setVerificationResult] = useState<VerificationResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const activeTaskIdRef = useRef<string | null>(null);
  activeTaskIdRef.current = activeTask?.taskId ?? null;

  const refreshTaskState = useCallback(async (taskId: string) => {
    try {
      const state = await agentApi.getTask(taskId);
      setActiveTask(state);
      if (state.latestVerificationResult) {
        setVerificationResult(state.latestVerificationResult);
      }
      if (state.finalTaskResult) {
        setTaskResult(state.finalTaskResult);
      }
      if (state.status === 'COMPLETED' || state.status === 'VERIFYING' || state.status === 'FAILED') {
        const diffRes = await agentApi.getDiff(taskId).catch(() => ({ diff: '' }));
        if (diffRes.diff) {
          setDiff(diffRes.diff);
        }
        if (!state.finalTaskResult) {
          const res = await agentApi.getTaskResult(taskId).catch(() => null);
          if (res) setTaskResult(res);
        }
        if (!state.latestVerificationResult) {
          const ver = await agentApi.getVerificationResult(taskId).catch(() => null);
          if (ver) setVerificationResult(ver);
        }
      }
    } catch (e: any) {
      console.warn('Failed to poll task state', e);
    }
  }, []);

  const startTask = async (payload: CreateTaskPayload) => {
    setLoading(true);
    setError(null);
    setEvents([]);
    setDiff('');
    try {
      const state = await agentApi.createTask(payload);
      setActiveTask(state);
      return state;
    } catch (err: any) {
      setError(err.message || 'Failed to start task');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const cancelCurrentTask = async () => {
    if (!activeTask) return;
    try {
      await agentApi.cancelTask(activeTask.taskId);
      await refreshTaskState(activeTask.taskId);
    } catch (err: any) {
      setError(err.message || 'Failed to cancel task');
    }
  };

  // Subscribe to SSE events whenever active task changes
  useEffect(() => {
    if (!activeTask?.taskId) return;

    const taskId = activeTask.taskId;
    const unsubscribe = agentApi.subscribeToEvents(taskId, (event) => {
      setEvents((prev) => {
        // Prevent duplicates
        if (prev.some((e) => e.eventId === event.eventId)) return prev;
        return [...prev, event];
      });
      refreshTaskState(taskId);
    });

    // Also poll every 2.5 seconds as a fallback in case SSE drops
    const interval = setInterval(() => {
      if (
        activeTaskIdRef.current === taskId &&
        activeTask.status !== 'COMPLETED' &&
        activeTask.status !== 'FAILED' &&
        activeTask.status !== 'CANCELLED'
      ) {
        refreshTaskState(taskId);
      }
    }, 2500);

    return () => {
      unsubscribe();
      clearInterval(interval);
    };
  }, [activeTask?.taskId, refreshTaskState]);

  const resetTask = () => {
    setActiveTask(null);
    setEvents([]);
    setDiff('');
    setTaskResult(null);
    setVerificationResult(null);
    setError(null);
  };

  return {
    activeTask,
    events,
    diff,
    taskResult,
    verificationResult,
    loading,
    error,
    startTask,
    cancelCurrentTask,
    refreshTaskState,
    resetTask,
  };
}
