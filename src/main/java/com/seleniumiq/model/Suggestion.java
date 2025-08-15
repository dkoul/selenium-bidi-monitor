package com.seleniumiq.model;

import java.util.Objects;

/**
 * Container class for suggestion-related model classes
 */
public class Suggestion {
    
    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Represents an issue detected during analysis
     */
    public static class Issue {
        private final String type;
        private final String title;
        private final String description;
        private final String suggestion;
        private final Priority priority;
        private final String impact;
        
        private Issue(Builder builder) {
            this.type = builder.type;
            this.title = builder.title;
            this.description = builder.description;
            this.suggestion = builder.suggestion;
            this.priority = builder.priority;
            this.impact = builder.impact;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getSuggestion() { return suggestion; }
        public Priority getPriority() { return priority; }
        public String getImpact() { return impact; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Issue issue = (Issue) o;
            return Objects.equals(type, issue.type) &&
                   Objects.equals(title, issue.title) &&
                   Objects.equals(description, issue.description);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(type, title, description);
        }
        
        @Override
        public String toString() {
            return String.format("Issue{type='%s', title='%s', priority=%s}", type, title, priority);
        }
        
        public static class Builder {
            private String type;
            private String title;
            private String description;
            private String suggestion;
            private Priority priority = Priority.MEDIUM;
            private String impact;
            
            public Builder type(String type) {
                this.type = type;
                return this;
            }
            
            public Builder title(String title) {
                this.title = title;
                return this;
            }
            
            public Builder description(String description) {
                this.description = description;
                return this;
            }
            
            public Builder suggestion(String suggestion) {
                this.suggestion = suggestion;
                return this;
            }
            
            public Builder priority(Priority priority) {
                this.priority = priority;
                return this;
            }
            
            public Builder impact(String impact) {
                this.impact = impact;
                return this;
            }
            
            public Issue build() {
                return new Issue(this);
            }
        }
    }
    
    /**
     * Represents a recommendation for improvement
     */
    public static class Recommendation {
        private final String category;
        private final String recommendation;
        private final String reasoning;
        
        private Recommendation(Builder builder) {
            this.category = builder.category;
            this.recommendation = builder.recommendation;
            this.reasoning = builder.reasoning;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public String getCategory() { return category; }
        public String getRecommendation() { return recommendation; }
        public String getReasoning() { return reasoning; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Recommendation that = (Recommendation) o;
            return Objects.equals(category, that.category) &&
                   Objects.equals(recommendation, that.recommendation);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(category, recommendation);
        }
        
        @Override
        public String toString() {
            return String.format("Recommendation{category='%s', recommendation='%s'}", category, recommendation);
        }
        
        public static class Builder {
            private String category;
            private String recommendation;
            private String reasoning;
            
            public Builder category(String category) {
                this.category = category;
                return this;
            }
            
            public Builder recommendation(String recommendation) {
                this.recommendation = recommendation;
                return this;
            }
            
            public Builder reasoning(String reasoning) {
                this.reasoning = reasoning;
                return this;
            }
            
            public Recommendation build() {
                return new Recommendation(this);
            }
        }
    }
} 