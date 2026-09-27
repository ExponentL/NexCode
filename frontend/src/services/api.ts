import { AgentEvent, AgentState, CreateTaskPayload, RepositoryInfo } from '../types/agent';

const getApiBase = (): string => {
  if (typeof window === 'undefined') return 'http://localhost:8080/api';
  const envUrl = (import.meta as any).env?.VITE_API_URL || (import.meta as any).env?.VITE_BACKEND_URL;
  if (envUrl) {
    return envUrl.endsWith('/api') ? envUrl : `${envUrl}/api`;
  }

  const hostname = window.location.hostname || 'localhost';
  const protocol = window.location.protocol || 'http:';

  // When frontend is on port 5173, dynamically route to port 8080 on the same host
  if (window.location.port === '5173') {
    return `${protocol}//${hostname}:8080/api`;
  }
  return '/api';
};

const API_BASE = getApiBase();

export const agentApi = {
  async createTask(payload: CreateTaskPayload): Promise<AgentState> {
    const res = await fetch(`${API_BASE}/tasks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const err = await res.text();
      throw new Error(`Failed to create task: ${err}`);
    }
    return res.json();
  },

  async getTask(id: string): Promise<AgentState> {
    const res = await fetch(`${API_BASE}/tasks/${id}`);
    if (!res.ok) {
      throw new Error(`Failed to fetch task: ${res.statusText}`);
    }
    return res.json();
  },

  async getTasks(): Promise<AgentState[]> {
    const res = await fetch(`${API_BASE}/tasks`);
    if (!res.ok) {
      return [];
    }
    return res.json();
  },

  async getDiff(id: string): Promise<{ diff: string }> {
    const res = await fetch(`${API_BASE}/tasks/${id}/diff`);
    if (!res.ok) {
      throw new Error(`Failed to fetch diff: ${res.statusText}`);
    }
    return res.json();
  },

  async cancelTask(id: string): Promise<{ cancelled: boolean; status: string }> {
    const res = await fetch(`${API_BASE}/tasks/${id}/cancel`, {
      method: 'POST',
    });
    if (!res.ok) {
      throw new Error(`Failed to cancel task: ${res.statusText}`);
    }
    return res.json();
  },

  async getRepositories(): Promise<RepositoryInfo[]> {
    const res = await fetch(`${API_BASE}/repositories`);
    if (!res.ok) {
      throw new Error(`Failed to fetch repositories: ${res.statusText}`);
    }
    return res.json();
  },

  async analyzeRepository(path: string): Promise<import('../types/agent').RepositoryAnalysis> {
    const res = await fetch(`${API_BASE}/repositories/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path }),
    });
    if (!res.ok) {
      const err = await res.text();
      throw new Error(`Failed to analyze repository: ${err}`);
    }
    return res.json();
  },

  async selectWorkspace(path?: string): Promise<import('../types/agent').WorkspaceSelectionResult> {
    const res = await fetch(`${API_BASE}/workspaces/select`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(path ? { path } : {}),
    });
    if (!res.ok) {
      const err = await res.text();
      throw new Error(`Failed to select directory: ${err}`);
    }
    return res.json();
  },

  async getCurrentWorkspace(): Promise<import('../types/agent').WorkspaceSelectionResult> {
    const res = await fetch(`${API_BASE}/workspaces/current`);
    if (!res.ok) {
      throw new Error(`Failed to fetch current workspace: ${res.statusText}`);
    }
    return res.json();
  },

  async setCurrentWorkspace(path: string): Promise<import('../types/agent').WorkspaceSelectionResult> {
    const res = await fetch(`${API_BASE}/workspaces/current`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path }),
    });
    if (!res.ok) {
      const err = await res.text();
      throw new Error(`Failed to set current workspace: ${err}`);
    }
    return res.json();
  },

  async cloneGitHub(url: string): Promise<import('../types/agent').WorkspaceSelectionResult> {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 60000);
    try {
      const res = await fetch(`${API_BASE}/workspaces/clone-github`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url }),
        signal: controller.signal,
      });
      if (!res.ok) {
        const err = await res.text();
        throw new Error(`Failed to clone GitHub repository: ${err}`);
      }
      return res.json();
    } catch (err: any) {
      if (err.name === 'AbortError') {
        throw new Error('Clone request timed out after 60 seconds.');
      }
      throw err;
    } finally {
      clearTimeout(timeoutId);
    }
  },

  async getTaskEvents(id: string): Promise<AgentEvent[]> {
    const res = await fetch(`${API_BASE}/tasks/${id}/events`, {
      headers: { Accept: 'application/json' },
    });
    if (!res.ok) {
      return [];
    }
    return res.json();
  },

  async getTaskResult(id: string): Promise<import('../types/agent').TaskResult | null> {
    const res = await fetch(`${API_BASE}/tasks/${id}/result`);
    if (!res.ok) {
      return null;
    }
    const text = await res.text();
    return text ? JSON.parse(text) : null;
  },

  async getVerificationResult(id: string): Promise<import('../types/agent').VerificationResult | null> {
    const res = await fetch(`${API_BASE}/tasks/${id}/verification`);
    if (!res.ok) {
      return null;
    }
    const text = await res.text();
    return text ? JSON.parse(text) : null;
  },

  subscribeToEvents(taskId: string, onEvent: (event: AgentEvent) => void): () => void {
    const eventSource = new EventSource(`${API_BASE}/tasks/${taskId}/events`);

    eventSource.onmessage = (e) => {
      try {
        const data: AgentEvent = JSON.parse(e.data);
        onEvent(data);
      } catch (err) {
        console.error('Error parsing SSE event', err);
      }
    };

    const eventTypes = [
      'TASK_RECEIVED',
      'REPOSITORY_SCANNED',
      'FILE_READ',
      'SEARCH_PERFORMED',
      'PLAN_CREATED',
      'MODEL_REQUEST',
      'MODEL_RESPONSE',
      'FILE_MODIFIED',
      'COMMAND_EXECUTED',
      'TEST_STARTED',
      'TEST_FAILED',
      'RECOVERY_STARTED',
      'RECOVERY_ATTEMPT',
      'TEST_PASSED',
      'VERIFICATION_STARTED',
      'TASK_COMPLETED',
      'TASK_FAILED',
      'TASK_CANCELLED',
      'ERROR'
    ];

    eventTypes.forEach((type) => {
      eventSource.addEventListener(type, (e: any) => {
        try {
          const data: AgentEvent = JSON.parse(e.data);
          onEvent(data);
        } catch (err) {
          console.error(`Error parsing SSE ${type}`, err);
        }
      });
    });

    return () => {
      eventSource.close();
    };
  },
};
