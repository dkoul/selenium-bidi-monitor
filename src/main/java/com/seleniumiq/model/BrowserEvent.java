package com.seleniumiq.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a browser event captured via WebDriver BiDi
 */
public class BrowserEvent {
    private final String id;
    private final String sessionId;
    private final String type;
    private final String level;
    private final String message;
    private final String details;
    private final Instant timestamp;
    private final String source;
    private final Map<String, Object> metadata;
    
    @JsonCreator
    public BrowserEvent(
            @JsonProperty("id") String id,
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("type") String type,
            @JsonProperty("level") String level,
            @JsonProperty("message") String message,
            @JsonProperty("details") String details,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("source") String source,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.id = id;
        this.sessionId = sessionId;
        this.type = type;
        this.level = level;
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
        this.source = source;
        this.metadata = metadata;
    }
    
    // Static factory methods for common event types
    public static BrowserEvent consoleLog(String sessionId, String level, String message, String source) {
        return new Builder()
            .sessionId(sessionId)
            .type("console")
            .level(level)
            .message(message)
            .source(source)
            .timestamp(Instant.now())
            .build();
    }
    
    public static BrowserEvent networkRequest(String sessionId, String url, int statusCode, long duration) {
        return new Builder()
            .sessionId(sessionId)
            .type("network")
            .level(statusCode >= 400 ? "ERROR" : "INFO")
            .message(String.format("Request to %s returned %d in %dms", url, statusCode, duration))
            .details(String.format("URL: %s, Status: %d, Duration: %dms", url, statusCode, duration))
            .timestamp(Instant.now())
            .build();
    }
    
    public static BrowserEvent javascriptException(String sessionId, String error, String stackTrace) {
        return new Builder()
            .sessionId(sessionId)
            .type("javascript-exception")
            .level("ERROR")
            .message(error)
            .details(stackTrace)
            .timestamp(Instant.now())
            .build();
    }
    
    public static BrowserEvent performanceMetric(String sessionId, String metric, Object value) {
        return new Builder()
            .sessionId(sessionId)
            .type("performance")
            .level("INFO")
            .message(String.format("Performance metric: %s = %s", metric, value))
            .details(String.format("Metric: %s, Value: %s", metric, value))
            .timestamp(Instant.now())
            .build();
    }
    
    // Getters
    public String getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getType() { return type; }
    public String getLevel() { return level; }
    public String getMessage() { return message; }
    public String getDetails() { return details; }
    public Instant getTimestamp() { return timestamp; }
    public String getSource() { return source; }
    public Map<String, Object> getMetadata() { return metadata; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrowserEvent that = (BrowserEvent) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("BrowserEvent{id='%s', type='%s', level='%s', message='%s', timestamp=%s}",
                id, type, level, message, timestamp);
    }
    
    /**
     * Builder for creating BrowserEvent instances
     */
    public static class Builder {
        private String id;
        private String sessionId;
        private String type;
        private String level;
        private String message;
        private String details;
        private Instant timestamp;
        private String source;
        private Map<String, Object> metadata;
        
        public Builder() {
            this.id = java.util.UUID.randomUUID().toString();
            this.timestamp = Instant.now();
        }
        
        public Builder id(String id) { this.id = id; return this; }
        public Builder sessionId(String sessionId) { this.sessionId = sessionId; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder level(String level) { this.level = level; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder source(String source) { this.source = source; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }
        
        public BrowserEvent build() {
            return new BrowserEvent(id, sessionId, type, level, message, details, timestamp, source, metadata);
        }
    }
} 