package com.example.codingagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private String workspaceRoot = ".";
    private int maxAttempts = 5;
    private Model model = new Model();

    public String getWorkspaceRoot() {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public static class Model {
        private String provider = "openai";
        private String name = "gpt-4o";
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private int timeoutSeconds = 120;
        private double temperature = 0.1;

        public String getProvider() {
            String envProvider = System.getenv("AI_MODEL_PROVIDER");
            if (envProvider != null && !envProvider.isBlank()) {
                return envProvider.trim();
            }
            String envAgentProvider = System.getenv("AGENT_MODEL_PROVIDER");
            if (envAgentProvider != null && !envAgentProvider.isBlank()) {
                return envAgentProvider.trim();
            }
            String key = getApiKey();
            if (key != null && key.startsWith("sk-xt-")) {
                return "xkiro";
            }
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getName() {
            String envName = System.getenv("AI_MODEL_NAME");
            if (envName != null && !envName.isBlank()) {
                return envName.trim();
            }
            String envAgentName = System.getenv("AGENT_MODEL_NAME");
            if (envAgentName != null && !envAgentName.isBlank()) {
                return envAgentName.trim();
            }
            String key = getApiKey();
            if (key != null && key.startsWith("sk-xt-")) {
                return "qwen/qwen3.5-flash:free";
            }
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getApiKey() {
            String envAi = System.getenv("AI_API_KEY");
            if (envAi != null && !envAi.isBlank()) {
                return envAi.trim();
            }
            if (apiKey != null && !apiKey.isBlank()) {
                return apiKey.trim();
            }
            String envAgent = System.getenv("AGENT_MODEL_API_KEY");
            if (envAgent != null && !envAgent.isBlank()) {
                return envAgent.trim();
            }
            return "";
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            String envBase = System.getenv("AI_BASE_URL");
            if (envBase != null && !envBase.isBlank()) {
                return envBase.trim();
            }
            String envAgentBase = System.getenv("AGENT_MODEL_BASE_URL");
            if (envAgentBase != null && !envAgentBase.isBlank()) {
                return envAgentBase.trim();
            }
            String key = getApiKey();
            if (key != null && key.startsWith("sk-xt-")) {
                return "https://api.xkiro.com/v1";
            }
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }
}
