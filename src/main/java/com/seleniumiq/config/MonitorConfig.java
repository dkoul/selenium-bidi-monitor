package com.seleniumiq.config;

import java.time.Duration;

/**
 * Configuration class for SeleniumIQ
 */
public class MonitorConfig {
    private final boolean monitoringEnabled;
    private final String provider;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;
    private final int maxRetries;
    private final boolean realtimeSuggestionsEnabled;
    private final Duration analysisInterval;
    private final int batchSize;
    
    // Ollama specific configuration
    private final String ollamaBaseUrl;
    private final String ollamaModel;
    
    private MonitorConfig(Builder builder) {
        this.monitoringEnabled = builder.monitoringEnabled;
        this.provider = builder.provider;
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.model = builder.model;
        this.timeout = builder.timeout;
        this.maxRetries = builder.maxRetries;
        this.realtimeSuggestionsEnabled = builder.realtimeSuggestionsEnabled;
        this.analysisInterval = builder.analysisInterval;
        this.batchSize = builder.batchSize;
        this.ollamaBaseUrl = builder.ollamaBaseUrl;
        this.ollamaModel = builder.ollamaModel;
    }
    
    public static MonitorConfig load() {
        // Load configuration with Ollama as default provider
        String provider = System.getProperty("seleniumiq.llm.provider", "ollama");
        String ollamaUrl = System.getProperty("seleniumiq.ollama.url", "http://localhost:11434");
        String ollamaModel = System.getProperty("seleniumiq.ollama.model", "mistral:latest");
        
        return new Builder()
            .monitoringEnabled(true)
            .provider(provider)
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .baseUrl("https://api.openai.com/v1")
            .model("gpt-4")
            .timeout(Duration.ofSeconds(60)) // Longer timeout for local models
            .maxRetries(3)
            .realtimeSuggestionsEnabled(true)
            .analysisInterval(Duration.ofSeconds(30))
            .batchSize(10)
            .ollamaBaseUrl(ollamaUrl)
            .ollamaModel(ollamaModel)
            .build();
    }
    
    // Getters
    public boolean isMonitoringEnabled() { return monitoringEnabled; }
    public String getProvider() { return provider; }
    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public String getModel() { return model; }
    public Duration getTimeout() { return timeout; }
    public int getMaxRetries() { return maxRetries; }
    public boolean isRealtimeSuggestionsEnabled() { return realtimeSuggestionsEnabled; }
    public Duration getAnalysisInterval() { return analysisInterval; }
    public int getBatchSize() { return batchSize; }
    public String getOllamaBaseUrl() { return ollamaBaseUrl; }
    public String getOllamaModel() { return ollamaModel; }
    
    public static class Builder {
        private boolean monitoringEnabled = true;
        private String provider = "ollama";
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4";
        private Duration timeout = Duration.ofSeconds(60);
        private int maxRetries = 3;
        private boolean realtimeSuggestionsEnabled = true;
        private Duration analysisInterval = Duration.ofSeconds(30);
        private int batchSize = 10;
        private String ollamaBaseUrl = "http://localhost:11434";
        private String ollamaModel = "mistral:latest";
        
        public Builder monitoringEnabled(boolean enabled) { this.monitoringEnabled = enabled; return this; }
        public Builder provider(String provider) { this.provider = provider; return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }
        public Builder maxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }
        public Builder realtimeSuggestionsEnabled(boolean enabled) { this.realtimeSuggestionsEnabled = enabled; return this; }
        public Builder analysisInterval(Duration interval) { this.analysisInterval = interval; return this; }
        public Builder batchSize(int batchSize) { this.batchSize = batchSize; return this; }
        public Builder ollamaBaseUrl(String url) { this.ollamaBaseUrl = url; return this; }
        public Builder ollamaModel(String model) { this.ollamaModel = model; return this; }
        
        public MonitorConfig build() {
            return new MonitorConfig(this);
        }
    }
} 