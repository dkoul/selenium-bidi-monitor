package com.seleniumiq.model;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Represents the result of LLM analysis of browser events
 */
public class AnalysisResult {
    private final String sessionId;
    private final String sessionName;
    private final Instant timestamp;
    private final String summary;
    private final Severity severity;
    private final List<Suggestion.Issue> issues;
    private final List<Suggestion.Recommendation> recommendations;
    private final boolean hasError;
    private final String errorMessage;
    
    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    private AnalysisResult(Builder builder) {
        this.sessionId = builder.sessionId;
        this.sessionName = builder.sessionName;
        this.timestamp = builder.timestamp;
        this.summary = builder.summary;
        this.severity = builder.severity;
        this.issues = new ArrayList<>(builder.issues);
        this.recommendations = new ArrayList<>(builder.recommendations);
        this.hasError = builder.hasError;
        this.errorMessage = builder.errorMessage;
    }
    
    // Static factory methods
    public static AnalysisResult empty(String message) {
        return new Builder()
            .summary(message)
            .severity(Severity.LOW)
            .timestamp(Instant.now())
            .build();
    }
    
    public static AnalysisResult error(String errorMessage) {
        return new Builder()
            .hasError(true)
            .errorMessage(errorMessage)
            .summary("Analysis failed")
            .severity(Severity.CRITICAL)
            .timestamp(Instant.now())
            .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public String getSessionName() { return sessionName; }
    public Instant getTimestamp() { return timestamp; }
    public String getSummary() { return summary; }
    public Severity getSeverity() { return severity; }
    public List<Suggestion.Issue> getIssues() { return new ArrayList<>(issues); }
    public List<Suggestion.Recommendation> getRecommendations() { return new ArrayList<>(recommendations); }
    public boolean hasError() { return hasError; }
    public String getErrorMessage() { return errorMessage; }
    
    // Convenience methods
    public boolean hasIssues() {
        return !issues.isEmpty();
    }
    
    public boolean hasRecommendations() {
        return !recommendations.isEmpty();
    }
    
    public boolean hasCriticalIssues() {
        return issues.stream().anyMatch(issue -> 
            issue.getPriority() == Suggestion.Priority.CRITICAL);
    }
    
    public long getCriticalIssueCount() {
        return issues.stream()
            .filter(issue -> issue.getPriority() == Suggestion.Priority.CRITICAL)
            .count();
    }
    
    public long getHighPriorityIssueCount() {
        return issues.stream()
            .filter(issue -> issue.getPriority() == Suggestion.Priority.HIGH)
            .count();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisResult that = (AnalysisResult) o;
        return Objects.equals(sessionId, that.sessionId) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sessionId, timestamp);
    }
    
    @Override
    public String toString() {
        return String.format("AnalysisResult{sessionId='%s', severity=%s, issues=%d, recommendations=%d, hasError=%s}",
                sessionId, severity, issues.size(), recommendations.size(), hasError);
    }
    
    /**
     * Builder for creating AnalysisResult instances
     */
    public static class Builder {
        private String sessionId;
        private String sessionName;
        private Instant timestamp = Instant.now();
        private String summary = "";
        private Severity severity = Severity.LOW;
        private List<Suggestion.Issue> issues = new ArrayList<>();
        private List<Suggestion.Recommendation> recommendations = new ArrayList<>();
        private boolean hasError = false;
        private String errorMessage;
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder sessionName(String sessionName) {
            this.sessionName = sessionName;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder addIssue(Suggestion.Issue issue) {
            this.issues.add(issue);
            return this;
        }
        
        public Builder issues(List<Suggestion.Issue> issues) {
            this.issues = new ArrayList<>(issues);
            return this;
        }
        
        public Builder addRecommendation(Suggestion.Recommendation recommendation) {
            this.recommendations.add(recommendation);
            return this;
        }
        
        public Builder recommendations(List<Suggestion.Recommendation> recommendations) {
            this.recommendations = new ArrayList<>(recommendations);
            return this;
        }
        
        public Builder hasError(boolean hasError) {
            this.hasError = hasError;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public AnalysisResult build() {
            return new AnalysisResult(this);
        }
    }
} 