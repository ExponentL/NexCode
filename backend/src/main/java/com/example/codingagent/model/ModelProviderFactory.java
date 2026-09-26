package com.example.codingagent.model;

import com.example.codingagent.config.AgentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class ModelProviderFactory {

    @Bean
    @Primary
    public ModelProvider primaryModelProvider(List<ModelProvider> providers, AgentProperties properties) {
        String configuredProvider = properties.getModel().getProvider();
        Map<String, ModelProvider> providerMap = providers.stream()
                .collect(Collectors.toMap(p -> p.getProviderName().toLowerCase(), p -> p));

        ModelProvider selected = providerMap.get(configuredProvider.toLowerCase());
        if (selected != null) {
            return selected;
        }

        // Support aliases for OpenAI-compatible providers such as xkiro
        if (("xkiro".equalsIgnoreCase(configuredProvider) || "openai-compatible".equalsIgnoreCase(configuredProvider))
                && providerMap.containsKey("openai")) {
            return providerMap.get("openai");
        }

        // Fallback or helpful exception
        throw new IllegalStateException("Configured model provider '" + configuredProvider +
                "' is not supported. Supported providers: " + providerMap.keySet());
    }
}
