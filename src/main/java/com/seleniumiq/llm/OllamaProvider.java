package com.seleniumiq.llm;

import com.seleniumiq.config.MonitorConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Ollama LLM provider implementation for local AI models
 */
public class OllamaProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(OllamaProvider.class);
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final MonitorConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String model;
    
    public OllamaProvider(MonitorConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        
        // Get Ollama configuration
        this.baseUrl = config.getOllamaBaseUrl();
        this.model = config.getOllamaModel();
        
        // Configure HTTP client with longer timeout for local models
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(config.getTimeout().toSeconds(), TimeUnit.SECONDS)
            .readTimeout(config.getTimeout().toSeconds(), TimeUnit.SECONDS)
            .writeTimeout(config.getTimeout().toSeconds(), TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
        
        logger.info("Ollama Provider initialized with model: {} at {}", model, baseUrl);
    }
    
    @Override
    public String analyze(String prompt, String systemPrompt) {
        try {
            // First, ensure the model is available
            if (!isModelAvailable()) {
                pullModel();
            }
            
            ObjectNode requestBody = createRequestBody(prompt, systemPrompt);
            
            Request request = new Request.Builder()
                .url(baseUrl + "/api/generate")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), JSON))
                .build();
            
            return executeRequestWithRetry(request);
            
        } catch (Exception e) {
            logger.error("Ollama analysis request failed", e);
            throw new RuntimeException("Ollama analysis failed", e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Check if Ollama is running
            Request healthCheck = new Request.Builder()
                .url(baseUrl + "/api/tags")
                .get()
                .build();
            
            try (Response response = httpClient.newCall(healthCheck).execute()) {
                return response.isSuccessful();
            }
            
        } catch (Exception e) {
            logger.debug("Ollama availability check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public void close() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
        logger.info("Ollama Provider closed");
    }
    
    /**
     * Check if the specified model is available locally
     */
    private boolean isModelAvailable() {
        try {
            Request request = new Request.Builder()
                .url(baseUrl + "/api/tags")
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonNode tagsResponse = objectMapper.readTree(responseBody);
                    JsonNode models = tagsResponse.get("models");
                    
                    if (models != null && models.isArray()) {
                        for (JsonNode modelNode : models) {
                            String modelName = modelNode.get("name").asText();
                            if (modelName.equals(model)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to check model availability: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Pull the model if it's not available locally
     */
    private void pullModel() {
        logger.info("Model {} not found locally. Attempting to pull...", model);
        
        try {
            ObjectNode pullRequest = objectMapper.createObjectNode();
            pullRequest.put("name", model);
            pullRequest.put("stream", false);
            
            Request request = new Request.Builder()
                .url(baseUrl + "/api/pull")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(pullRequest.toString(), JSON))
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Successfully pulled model: {}", model);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No error details";
                    logger.warn("Failed to pull model {}: HTTP {} - {}", model, response.code(), errorBody);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error pulling model {}: {}", model, e.getMessage());
        }
    }
    
    /**
     * Create the request body for Ollama generate API
     */
    private ObjectNode createRequestBody(String prompt, String systemPrompt) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("stream", false);
        requestBody.put("format", "json");
        
        // Combine system prompt and user prompt
        StringBuilder fullPrompt = new StringBuilder();
        
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            fullPrompt.append("SYSTEM: ").append(systemPrompt).append("\n\n");
        }
        
        fullPrompt.append("USER: ").append(prompt);
        
        requestBody.put("prompt", fullPrompt.toString());
        
        // Set options for better analysis
        ObjectNode options = requestBody.putObject("options");
        options.put("temperature", 0.3);
        options.put("top_p", 0.9);
        options.put("num_predict", 2000);
        
        return requestBody;
    }
    
    /**
     * Execute the request with retry logic
     */
    private String executeRequestWithRetry(Request request) throws IOException {
        int maxRetries = config.getMaxRetries();
        IOException lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        return extractContentFromResponse(responseBody);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "No error details";
                        logger.warn("Ollama request failed (attempt {}/{}): HTTP {} - {}", 
                                  attempt, maxRetries, response.code(), errorBody);
                        
                        // Don't retry on client errors (4xx)
                        if (response.code() >= 400 && response.code() < 500) {
                            throw new IOException("Client error: " + response.code() + " - " + errorBody);
                        }
                        
                        lastException = new IOException("HTTP " + response.code() + ": " + errorBody);
                    }
                }
            } catch (IOException e) {
                lastException = e;
                logger.warn("Ollama request attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());
                
                // Wait before retry (exponential backoff)
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Request interrupted", ie);
                    }
                }
            }
        }
        
        throw new IOException("All retry attempts failed", lastException);
    }
    
    /**
     * Extract the content from Ollama response
     */
    private String extractContentFromResponse(String responseBody) throws IOException {
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            
            if (response.has("error")) {
                String errorMessage = response.get("error").asText();
                throw new IOException("Ollama API error: " + errorMessage);
            }
            
            String content = response.get("response").asText();
            if (content == null || content.trim().isEmpty()) {
                throw new IOException("Empty content in Ollama response");
            }
            
            return content.trim();
            
        } catch (Exception e) {
            logger.error("Failed to parse Ollama response: {}", responseBody, e);
            throw new IOException("Failed to parse Ollama response", e);
        }
    }
} 