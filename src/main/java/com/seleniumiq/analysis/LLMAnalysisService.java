package com.seleniumiq.analysis;

import com.seleniumiq.config.MonitorConfig;
import com.seleniumiq.model.BrowserEvent;
import com.seleniumiq.model.MonitoringSession;
import com.seleniumiq.model.AnalysisResult;
import com.seleniumiq.model.Suggestion;
import com.seleniumiq.llm.LLMProvider;
import com.seleniumiq.llm.LLMProviderFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Service responsible for analyzing browser events using LLM and generating intelligent suggestions
 */
public class LLMAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(LLMAnalysisService.class);
    
    private final MonitorConfig config;
    private final LLMProvider llmProvider;
    private final ObjectMapper objectMapper;
    private final ExecutorService analysisExecutor;
    private final BlockingQueue<AnalysisTask> analysisQueue;
    private final AtomicInteger queueSize = new AtomicInteger(0);
    
    // Analysis prompt templates
    private static final String SYSTEM_PROMPT = """
        You are an expert Selenium test automation engineer and web application performance analyst.
        Your role is to analyze browser events captured during test execution and provide actionable insights.
        
        Focus on:
        1. Performance optimization opportunities
        2. Error resolution strategies
        3. Test stability improvements
        4. Security vulnerabilities
        5. Best practices recommendations
        
        Provide specific, actionable suggestions with clear reasoning.
        Format your response as JSON with the following structure:
        {
          "summary": "Brief overview of findings",
          "severity": "LOW|MEDIUM|HIGH|CRITICAL",
          "issues": [
            {
              "type": "performance|error|security|stability|best-practice",
              "title": "Issue title",
              "description": "Detailed description",
              "suggestion": "Specific recommendation",
              "priority": "LOW|MEDIUM|HIGH|CRITICAL",
              "impact": "Potential impact description"
            }
          ],
          "recommendations": [
            {
              "category": "performance-optimization|error-resolution|test-stability|security-improvements|best-practices",
              "recommendation": "Specific recommendation",
              "reasoning": "Why this recommendation is important"
            }
          ]
        }
        """;
    
    private static final String ANALYSIS_PROMPT_TEMPLATE = """
        Analyze the following browser events from a Selenium test session:
        
        Session: %s
        Time Range: %s to %s
        Total Events: %d
        
        Events Summary:
        %s
        
        Event Details:
        %s
        
        Please analyze these events and provide insights on:
        1. Any errors or exceptions that occurred
        2. Performance issues (slow requests, high resource usage)
        3. Potential test stability problems
        4. Security concerns
        5. Opportunities for optimization
        
        Consider the context of automated testing and provide suggestions that would help improve test reliability and performance.
        """;
    
    public LLMAnalysisService(MonitorConfig config) {
        this.config = config;
        this.llmProvider = LLMProviderFactory.create(config);
        this.objectMapper = new ObjectMapper();
        this.analysisExecutor = Executors.newFixedThreadPool(2);
        this.analysisQueue = new LinkedBlockingQueue<>();
        
        logger.info("LLM Analysis Service initialized with provider: {}", config.getProvider());
    }
    
    /**
     * Analyze a list of browser events and generate suggestions
     * 
     * @param events List of browser events to analyze
     * @param session The monitoring session context
     * @return CompletableFuture containing analysis results
     */
    public CompletableFuture<AnalysisResult> analyzeEvents(List<BrowserEvent> events, MonitoringSession session) {
        if (events.isEmpty()) {
            return CompletableFuture.completedFuture(
                AnalysisResult.empty("No events to analyze")
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                queueSize.incrementAndGet();
                
                String analysisPrompt = buildAnalysisPrompt(events, session);
                String llmResponse = llmProvider.analyze(analysisPrompt, SYSTEM_PROMPT);
                
                AnalysisResult result = parseAnalysisResponse(llmResponse, session);
                
                logger.debug("Analysis completed for session: {} with {} events", 
                           session.getName(), events.size());
                
                return result;
                
            } catch (Exception e) {
                logger.error("Analysis failed for session: {}", session.getName(), e);
                return AnalysisResult.error("Analysis failed: " + e.getMessage());
            } finally {
                queueSize.decrementAndGet();
            }
        }, analysisExecutor);
    }
    
    /**
     * Get the current analysis queue size
     * 
     * @return Current queue size
     */
    public int getQueueSize() {
        return queueSize.get();
    }
    
    /**
     * Shutdown the analysis service
     */
    public void shutdown() {
        logger.info("Shutting down LLM Analysis Service...");
        
        analysisExecutor.shutdown();
        try {
            if (!analysisExecutor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                analysisExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            analysisExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        if (llmProvider != null) {
            llmProvider.close();
        }
        
        logger.info("LLM Analysis Service shutdown complete");
    }
    
    /**
     * Build the analysis prompt from events and session context
     */
    private String buildAnalysisPrompt(List<BrowserEvent> events, MonitoringSession session) {
        // Sort events by timestamp
        List<BrowserEvent> sortedEvents = events.stream()
            .sorted((e1, e2) -> e1.getTimestamp().compareTo(e2.getTimestamp()))
            .collect(Collectors.toList());
        
        Instant startTime = sortedEvents.get(0).getTimestamp();
        Instant endTime = sortedEvents.get(sortedEvents.size() - 1).getTimestamp();
        
        // Generate events summary
        Map<String, Long> eventTypeCounts = events.stream()
            .collect(Collectors.groupingBy(
                BrowserEvent::getType, 
                Collectors.counting()
            ));
        
        StringBuilder summaryBuilder = new StringBuilder();
        eventTypeCounts.forEach((type, count) -> 
            summaryBuilder.append(String.format("- %s: %d events\n", type, count))
        );
        
        // Generate detailed event information
        StringBuilder detailsBuilder = new StringBuilder();
        for (int i = 0; i < Math.min(sortedEvents.size(), 20); i++) {
            BrowserEvent event = sortedEvents.get(i);
            detailsBuilder.append(String.format(
                "[%s] %s: %s\n",
                DateTimeFormatter.ISO_INSTANT.format(event.getTimestamp()),
                event.getType(),
                event.getMessage()
            ));
            
            if (event.getDetails() != null && !event.getDetails().isEmpty()) {
                detailsBuilder.append("  Details: ").append(event.getDetails()).append("\n");
            }
            
            if (event.getLevel() != null) {
                detailsBuilder.append("  Level: ").append(event.getLevel()).append("\n");
            }
            
            detailsBuilder.append("\n");
        }
        
        if (sortedEvents.size() > 20) {
            detailsBuilder.append(String.format("... and %d more events\n", sortedEvents.size() - 20));
        }
        
        return String.format(
            ANALYSIS_PROMPT_TEMPLATE,
            session.getName(),
            DateTimeFormatter.ISO_INSTANT.format(startTime),
            DateTimeFormatter.ISO_INSTANT.format(endTime),
            events.size(),
            summaryBuilder.toString(),
            detailsBuilder.toString()
        );
    }
    
    /**
     * Parse the LLM response into an AnalysisResult
     */
    private AnalysisResult parseAnalysisResponse(String response, MonitoringSession session) {
        try {
            JsonNode jsonResponse = objectMapper.readTree(response);
            
            AnalysisResult.Builder resultBuilder = AnalysisResult.builder()
                .sessionId(session.getId())
                .sessionName(session.getName())
                .timestamp(Instant.now());
            
            // Parse summary and severity
            if (jsonResponse.has("summary")) {
                resultBuilder.summary(jsonResponse.get("summary").asText());
            }
            
            if (jsonResponse.has("severity")) {
                resultBuilder.severity(AnalysisResult.Severity.valueOf(
                    jsonResponse.get("severity").asText().toUpperCase()
                ));
            }
            
            // Parse issues
            if (jsonResponse.has("issues") && jsonResponse.get("issues").isArray()) {
                for (JsonNode issueNode : jsonResponse.get("issues")) {
                    Suggestion.Issue issue = parseIssue(issueNode);
                    resultBuilder.addIssue(issue);
                }
            }
            
            // Parse recommendations
            if (jsonResponse.has("recommendations") && jsonResponse.get("recommendations").isArray()) {
                for (JsonNode recNode : jsonResponse.get("recommendations")) {
                    Suggestion.Recommendation recommendation = parseRecommendation(recNode);
                    resultBuilder.addRecommendation(recommendation);
                }
            }
            
            return resultBuilder.build();
            
        } catch (Exception e) {
            logger.error("Failed to parse LLM response", e);
            return AnalysisResult.error("Failed to parse analysis response: " + e.getMessage());
        }
    }
    
    /**
     * Parse an issue from JSON node
     */
    private Suggestion.Issue parseIssue(JsonNode issueNode) {
        return Suggestion.Issue.builder()
            .type(issueNode.has("type") ? issueNode.get("type").asText() : "unknown")
            .title(issueNode.has("title") ? issueNode.get("title").asText() : "Unknown Issue")
            .description(issueNode.has("description") ? issueNode.get("description").asText() : "")
            .suggestion(issueNode.has("suggestion") ? issueNode.get("suggestion").asText() : "")
            .priority(issueNode.has("priority") ? 
                Suggestion.Priority.valueOf(issueNode.get("priority").asText().toUpperCase()) : 
                Suggestion.Priority.MEDIUM)
            .impact(issueNode.has("impact") ? issueNode.get("impact").asText() : "")
            .build();
    }
    
    /**
     * Parse a recommendation from JSON node
     */
    private Suggestion.Recommendation parseRecommendation(JsonNode recNode) {
        return Suggestion.Recommendation.builder()
            .category(recNode.has("category") ? recNode.get("category").asText() : "general")
            .recommendation(recNode.has("recommendation") ? recNode.get("recommendation").asText() : "")
            .reasoning(recNode.has("reasoning") ? recNode.get("reasoning").asText() : "")
            .build();
    }
    
    /**
     * Internal class for analysis tasks
     */
    private static class AnalysisTask {
        private final List<BrowserEvent> events;
        private final MonitoringSession session;
        private final CompletableFuture<AnalysisResult> future;
        
        public AnalysisTask(List<BrowserEvent> events, MonitoringSession session, CompletableFuture<AnalysisResult> future) {
            this.events = events;
            this.session = session;
            this.future = future;
        }
        
        public List<BrowserEvent> getEvents() { return events; }
        public MonitoringSession getSession() { return session; }
        public CompletableFuture<AnalysisResult> getFuture() { return future; }
    }
} 