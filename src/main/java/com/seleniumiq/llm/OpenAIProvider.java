package com.seleniumiq.llm;

import com.seleniumiq.config.MonitorConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI LLM provider implementation
 */
public class OpenAIProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIProvider.class);
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final MonitorConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    
    public OpenAIProvider(MonitorConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        
        // Get configuration values
        this.apiKey = config.getApiKey();
        this.baseUrl = config.getBaseUrl();
        this.model = config.getModel();
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key is required. Set OPENAI_API_KEY environment variable.");
        }
        
        // Configure HTTP client with timeout and retry settings
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(config.getTimeout().toSeconds(), TimeUnit.SECONDS)
            .readTimeout(config.getTimeout().toSeconds(), TimeUnit.SECONDS)
            .writeTimeout(config.getTimeout().toSeconds(), TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
        
        logger.info("OpenAI Provider initialized with model: {} and base URL: {}", model, baseUrl);
    }
    
    @Override
    public String analyze(String prompt, String systemPrompt) {
        try {
            ObjectNode requestBody = createRequestBody(prompt, systemPrompt);
            
            Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), JSON))
                .build();
            
            return executeRequestWithRetry(request);
            
        } catch (Exception e) {
            logger.error("OpenAI analysis request failed", e);
            throw new RuntimeException("OpenAI analysis failed", e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Simple health check - try to make a minimal request
            ObjectNode testRequest = objectMapper.createObjectNode();
            testRequest.put("model", model);
            testRequest.put("max_tokens", 1);
            
            ArrayNode messages = testRequest.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", "test");
            
            Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(testRequest.toString(), JSON))
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
            
        } catch (Exception e) {
            logger.debug("OpenAI availability check failed", e);
            return false;
        }
    }
    
    @Override
    public void close() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
        logger.info("OpenAI Provider closed");
    }
    
    /**
     * Create the request body for OpenAI chat completions API
     */
    private ObjectNode createRequestBody(String prompt, String systemPrompt) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 2000);
        requestBody.put("temperature", 0.3);
        requestBody.put("top_p", 0.9);
        
        ArrayNode messages = requestBody.putArray("messages");
        
        // Add system message
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            ObjectNode systemMessage = messages.addObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
        }
        
        // Add user message
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        
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
                        logger.warn("OpenAI request failed (attempt {}/{}): HTTP {} - {}", 
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
                logger.warn("OpenAI request attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());
                
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
     * Extract the content from OpenAI response
     */
    private String extractContentFromResponse(String responseBody) throws IOException {
        try {
            JsonNode response = objectMapper.readTree(responseBody);
            
            if (response.has("error")) {
                JsonNode error = response.get("error");
                String errorMessage = error.has("message") ? error.get("message").asText() : "Unknown error";
                throw new IOException("OpenAI API error: " + errorMessage);
            }
            
            JsonNode choices = response.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                throw new IOException("No choices in OpenAI response");
            }
            
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            if (message == null) {
                throw new IOException("No message in OpenAI choice");
            }
            
            String content = message.get("content").asText();
            if (content == null || content.trim().isEmpty()) {
                throw new IOException("Empty content in OpenAI response");
            }
            
            return content.trim();
            
        } catch (Exception e) {
            logger.error("Failed to parse OpenAI response: {}", responseBody, e);
            throw new IOException("Failed to parse OpenAI response", e);
        }
    }
} 