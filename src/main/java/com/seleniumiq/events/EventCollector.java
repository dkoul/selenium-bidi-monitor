package com.seleniumiq.events;

import com.seleniumiq.config.MonitorConfig;
import com.seleniumiq.model.BrowserEvent;
import com.seleniumiq.model.MonitoringSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Collects browser events via WebDriver BiDi
 */
public class EventCollector {
    private static final Logger logger = LoggerFactory.getLogger(EventCollector.class);
    
    private final MonitorConfig config;
    private final Map<String, List<BrowserEvent>> sessionEvents = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsCount = new AtomicLong(0);
    
    public EventCollector(MonitorConfig config) {
        this.config = config;
        logger.info("Event Collector initialized");
    }
    
    public void startCollecting(MonitoringSession session) {
        String sessionId = session.getId();
        sessionEvents.put(sessionId, new ArrayList<>());
        
        // Simulate collecting some browser events for demonstration
        simulateEventCollection(session);
        
        logger.info("Started event collection for session: {}", sessionId);
    }
    
    public void stopCollecting(MonitoringSession session) {
        String sessionId = session.getId();
        List<BrowserEvent> events = sessionEvents.get(sessionId);
        
        if (events != null) {
            logger.info("Stopped event collection for session: {} (collected {} events)", 
                       sessionId, events.size());
        }
    }
    
    public List<BrowserEvent> getRecentEvents(String sessionId, int limit) {
        List<BrowserEvent> events = sessionEvents.get(sessionId);
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Return the most recent events
        int fromIndex = Math.max(0, events.size() - limit);
        return new ArrayList<>(events.subList(fromIndex, events.size()));
    }
    
    public List<BrowserEvent> getAllEvents(String sessionId) {
        List<BrowserEvent> events = sessionEvents.get(sessionId);
        return events != null ? new ArrayList<>(events) : new ArrayList<>();
    }
    
    public long getTotalEventsCount() {
        return totalEventsCount.get();
    }
    
    /**
     * Simulate event collection for demonstration purposes
     */
    private void simulateEventCollection(MonitoringSession session) {
        String sessionId = session.getId();
        List<BrowserEvent> events = sessionEvents.get(sessionId);
        
        // Simulate some typical browser events
        events.add(BrowserEvent.consoleLog(sessionId, "INFO", "Page loaded successfully", "https://example.com"));
        events.add(BrowserEvent.networkRequest(sessionId, "https://example.com", 200, 250));
        events.add(BrowserEvent.performanceMetric(sessionId, "page-load-time", 1200));
        
        // Simulate some potential issues for the LLM to analyze
        events.add(BrowserEvent.consoleLog(sessionId, "WARN", "Deprecated API usage detected", "https://example.com"));
        events.add(BrowserEvent.networkRequest(sessionId, "https://api.example.com/slow", 200, 5200)); // Slow request
        events.add(BrowserEvent.javascriptException(sessionId, "TypeError: Cannot read property 'value' of null", 
                   "at validateForm (form.js:42:15)"));
        
        // Add console activity that was triggered in the test
        events.add(BrowserEvent.consoleLog(sessionId, "INFO", "Test completed successfully", "test-script"));
        events.add(BrowserEvent.consoleLog(sessionId, "WARN", "This is a test warning for monitoring", "test-script"));
        
        totalEventsCount.addAndGet(events.size());
        
        logger.debug("Simulated {} events for session: {}", events.size(), sessionId);
    }
} 