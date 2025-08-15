package com.seleniumiq.llm;

import com.seleniumiq.config.MonitorConfig;

/**
 * Factory for creating LLM provider instances
 */
public class LLMProviderFactory {
    
    public static LLMProvider create(MonitorConfig config) {
        String provider = config.getProvider().toLowerCase();
        
        switch (provider) {
            case "openai":
                return new OpenAIProvider(config);
            case "ollama":
                return new OllamaProvider(config);
            case "anthropic":
                // TODO: Implement AnthropicProvider when needed
                throw new UnsupportedOperationException("Anthropic provider not yet implemented");
            case "azure-openai":
                // TODO: Implement AzureOpenAIProvider when needed
                throw new UnsupportedOperationException("Azure OpenAI provider not yet implemented");
            default:
                throw new IllegalArgumentException("Unsupported LLM provider: " + provider + 
                    ". Supported providers: openai, ollama");
        }
    }
} 