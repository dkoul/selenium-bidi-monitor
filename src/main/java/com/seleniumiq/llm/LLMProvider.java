package com.seleniumiq.llm;

/**
 * Interface for LLM providers
 */
public interface LLMProvider {
    
    /**
     * Analyze the given prompt and return suggestions
     * 
     * @param prompt The analysis prompt
     * @param systemPrompt The system prompt for context
     * @return LLM response as JSON string
     */
    String analyze(String prompt, String systemPrompt);
    
    /**
     * Check if the LLM provider is available
     * 
     * @return true if available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Close any resources used by the provider
     */
    void close();
} 