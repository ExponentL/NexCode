import { useEffect, useState } from 'react';
import { RepositoryAnalysis, RepositoryInfo } from '../types/agent';
import { agentApi } from '../services/api';

export function useRepositories() {
  const [repositories, setRepositories] = useState<RepositoryInfo[]>([]);
  const [selectedRepo, setSelectedRepo] = useState<string>('');
  const [repoAnalysis, setRepoAnalysis] = useState<RepositoryAnalysis | null>(null);
  const [loading, setLoading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [selectingDirectory, setSelectingDirectory] = useState(false);
  const [selectionMessage, setSelectionMessage] = useState<string | null>(null);

  const fetchRepos = async () => {
    setLoading(true);
    setError(null);
    try {
      // Check current active workspace on backend first
      try {
        const current = await agentApi.getCurrentWorkspace();
        if (current && current.exists && current.path) {
          setSelectedRepo(current.path);
          if (current.analysis) {
            setRepoAnalysis(current.analysis);
          }
        }
      } catch (ignored) {}

      const list = await agentApi.getRepositories();
      if (list && list.length > 0) {
        setRepositories(list);
        if (!selectedRepo) {
          const first = list[0].path;
          setSelectedRepo(first);
          analyzeRepo(first);
        }
      }
    } catch (err: any) {
      setError('Backend API is offline (port 8080). Launch the Spring Boot backend to enable live execution.');
    } finally {
      setLoading(false);
    }
  };

  const selectDirectory = async (): Promise<import('../types/agent').WorkspaceSelectionResult | null> => {
    setSelectingDirectory(true);
    setSelectionMessage('Opening native system folder chooser...');
    setError(null);
    try {
      const res = await agentApi.selectWorkspace();
      if (res.cancelled) {
        setSelectionMessage('No directory selected');
        return res;
      }
      if (res.exists && res.path) {
        const selectedPath = res.path;
        const selectedName = res.name || selectedPath.split('/').pop() || 'Selected Directory';
        const projectType = res.analysis?.projectType || 'Project';

        setSelectedRepo(selectedPath);
        if (res.analysis) {
          setRepoAnalysis(res.analysis);
        } else {
          analyzeRepo(selectedPath);
        }
        setSelectionMessage(`Selected: ${selectedName}`);
        // Ensure this repo is in repositories list
        setRepositories((prev) => {
          if (!prev.some((r) => r.path === selectedPath)) {
            return [
              {
                name: selectedName,
                path: selectedPath,
                type: projectType,
              },
              ...prev,
            ];
          }
          return prev;
        });
        return res;
      } else {
        setError(res.message || 'Selected directory is invalid or not accessible');
        setSelectionMessage(null);
        return res;
      }
    } catch (err: any) {
      setError(err.message || 'Failed to open directory chooser');
      setSelectionMessage(null);
      return null;
    } finally {
      setSelectingDirectory(false);
    }
  };

  const analyzeRepo = async (path: string): Promise<RepositoryAnalysis | null> => {
    if (!path || !path.trim()) {
      setRepoAnalysis(null);
      return null;
    }
    setAnalyzing(true);
    setError(null);
    try {
      const analysis = await agentApi.analyzeRepository(path.trim());
      setRepoAnalysis(analysis);
      return analysis;
    } catch (err: any) {
      console.warn('Repository analysis failed:', err);
      setRepoAnalysis({
        name: path.split('/').pop() || path,
        path: path,
        exists: false,
        isGit: false,
        gitBranch: null,
        gitStatus: 'Directory not accessible',
        projectType: 'Project type not automatically detected',
        detectedLanguages: [],
        sourceDirectories: [],
        testDirectories: [],
        buildSystem: 'None detected',
        availableCommands: [],
        totalFiles: 0,
        readmeSummary: null,
        errorMessage: err.message || 'Failed to inspect directory',
      });
      return null;
    } finally {
      setAnalyzing(false);
    }
  };

  const selectAndAnalyzeRepo = (path: string) => {
    setSelectedRepo(path);
    if (path) {
      analyzeRepo(path);
      // Notify backend of current workspace
      agentApi.setCurrentWorkspace(path).catch(() => {});
    } else {
      setRepoAnalysis(null);
    }
  };

  const [cloning, setCloning] = useState(false);
  const [cloneError, setCloneError] = useState<string | null>(null);

  const cloneGitHub = async (url: string): Promise<import('../types/agent').WorkspaceSelectionResult | null> => {
    if (!url || !url.trim()) {
      setCloneError('Please provide a valid GitHub repository URL');
      return null;
    }
    setCloning(true);
    setCloneError(null);
    try {
      const res = await agentApi.cloneGitHub(url.trim());
      if (res.exists && res.path) {
        const selectedPath = res.path;
        const selectedName = res.name || selectedPath.split('/').pop() || 'GitHub Repository';
        const projectType = res.analysis?.projectType || 'Project';

        setSelectedRepo(selectedPath);
        if (res.analysis) {
          setRepoAnalysis(res.analysis);
        } else {
          analyzeRepo(selectedPath);
        }
        setSelectionMessage(`Cloned & Activated: ${selectedName}`);
        setRepositories((prev) => {
          if (!prev.some((r) => r.path === selectedPath)) {
            return [
              {
                name: selectedName,
                path: selectedPath,
                type: projectType,
              },
              ...prev,
            ];
          }
          return prev;
        });
        return res;
      } else {
        setCloneError(res.message || 'Failed to clone repository');
        return res;
      }
    } catch (err: any) {
      setCloneError(err.message || 'Error communicating with backend during clone');
      return null;
    } finally {
      setCloning(false);
    }
  };

  useEffect(() => {
    fetchRepos();
  }, []);

  return {
    repositories,
    selectedRepo,
    setSelectedRepo: selectAndAnalyzeRepo,
    repoAnalysis,
    analyzeRepo,
    selectDirectory,
    selectingDirectory,
    selectionMessage,
    cloneGitHub,
    cloning,
    cloneError,
    loading,
    analyzing,
    error,
    refresh: fetchRepos,
  };
}
