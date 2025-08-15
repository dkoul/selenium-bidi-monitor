package com.seleniumiq.events;

import com.seleniumiq.config.MonitorConfig;
import com.seleniumiq.model.BrowserEvent;
import com.seleniumiq.model.MonitoringSession;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v85.log.Log;
import org.openqa.selenium.devtools.v85.network.Network;
import org.openqa.selenium.devtools.v85.runtime.Runtime;
import org.openqa.selenium.devtools.v85.performance.Performance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Optional;

/**
 * Real BiDi event collector that uses Chrome DevTools Protocol (CDP) and WebDriver BiDi
 * to capture actual browser events in real-time.
 */
public class BiDiEventCollector {
    private static final Logger logger = LoggerFactory.getLogger(BiDiEventCollector.class);
    
    private final MonitorConfig config;
    private final Map<String, List<BrowserEvent>> sessionEvents = new ConcurrentHashMap<>();
    private final Map<String, DevTools> activeDevTools = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsCount = new AtomicLong(0);
    
    public BiDiEventCollector(MonitorConfig config) {
        this.config = config;
        logger.info("Real BiDi Event Collector initialized");
    }
    
    /**
     * Start collecting real browser events using BiDi/DevTools
     */
    public void startCollecting(MonitoringSession session) {
        String sessionId = session.getId();
        WebDriver driver = session.getDriver();
        
        sessionEvents.put(sessionId, new ArrayList<>());
        
        try {
            // Check if driver supports DevTools (BiDi)
            if (!(driver instanceof HasDevTools)) {
                logger.warn("WebDriver does not support DevTools/BiDi. Falling back to simulated events for session: {}", sessionId);
                simulateEventCollection(session);
                return;
            }
            
            // Get DevTools instance
            DevTools devTools = ((HasDevTools) driver).getDevTools();
            devTools.createSession();
            activeDevTools.put(sessionId, devTools);
            
            // Enable domains we want to monitor
            enableMonitoringDomains(devTools, sessionId);
            
            // Set up event listeners
            setupEventListeners(devTools, sessionId);
            
            logger.info("Started real BiDi event collection for session: {}", sessionId);
            
        } catch (Exception e) {
            logger.error("Failed to start BiDi collection for session: {}. Falling back to simulation.", sessionId, e);
            simulateEventCollection(session);
        }
    }
    
    /**
     * Enable CDP/BiDi domains for monitoring
     */
    private void enableMonitoringDomains(DevTools devTools, String sessionId) {
        try {
            // Enable Console/Log domain
            devTools.send(Log.enable());
            logger.debug("Enabled Log domain for session: {}", sessionId);
            
            // Enable Runtime domain (for console events and exceptions)
            devTools.send(Runtime.enable());
            logger.debug("Enabled Runtime domain for session: {}", sessionId);
            
            // Enable Network domain
            devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
            logger.debug("Enabled Network domain for session: {}", sessionId);
            
            // Enable Performance domain
            devTools.send(Performance.enable(Optional.empty()));
            logger.debug("Enabled Performance domain for session: {}", sessionId);
            
        } catch (Exception e) {
            logger.error("Failed to enable monitoring domains for session: {}", sessionId, e);
        }
    }
    
    /**
     * Set up real-time event listeners
     */
    private void setupEventListeners(DevTools devTools, String sessionId) {
        List<BrowserEvent> events = sessionEvents.get(sessionId);
        
        // Listen to console messages
        devTools.addListener(Log.entryAdded(), logEntry -> {
            try {
                BrowserEvent event = BrowserEvent.consoleLog(
                    sessionId,
                    logEntry.getLevel().toString(),
                    logEntry.getText(),
                    logEntry.getUrl().orElse("unknown")
                );
                events.add(event);
                totalEventsCount.incrementAndGet();
                
                logger.debug("Captured console log: {} - {}", logEntry.getLevel(), logEntry.getText());
            } catch (Exception e) {
                logger.debug("Error processing console log event", e);
            }
        });
        
        // Listen to JavaScript exceptions
        devTools.addListener(Runtime.exceptionThrown(), exceptionDetails -> {
            try {
                String exceptionText = exceptionDetails.getExceptionDetails().getText();
                String stackTrace = exceptionDetails.getExceptionDetails().getException()
                    .map(exception -> exception.getDescription().orElse(""))
                    .orElse("");
                
                BrowserEvent event = BrowserEvent.javascriptException(
                    sessionId,
                    exceptionText,
                    stackTrace
                );
                events.add(event);
                totalEventsCount.incrementAndGet();
                
                logger.debug("Captured JS exception: {}", exceptionText);
            } catch (Exception e) {
                logger.debug("Error processing exception event", e);
            }
        });
        
        // Listen to network requests
        devTools.addListener(Network.responseReceived(), responseReceived -> {
            try {
                var response = responseReceived.getResponse();
                
                BrowserEvent event = BrowserEvent.networkRequest(
                    sessionId,
                    response.getUrl(),
                    response.getStatus().intValue(),
                    System.currentTimeMillis() // This is a simplification - real timing would need requestWillBeSent
                );
                events.add(event);
                totalEventsCount.incrementAndGet();
                
                logger.debug("Captured network response: {} - {}", response.getStatus(), response.getUrl());
            } catch (Exception e) {
                logger.debug("Error processing network event", e);
            }
        });
        
        // Listen to network failures
        devTools.addListener(Network.loadingFailed(), loadingFailed -> {
            try {
                BrowserEvent event = BrowserEvent.networkRequest(
                    sessionId,
                    loadingFailed.getRequestId().toString(),
                    0, // Failed request
                    0
                );
                // Create network failure event using builder
                event = new BrowserEvent.Builder()
                    .sessionId(sessionId)
                    .type("network-failure")
                    .level("ERROR")
                    .message("Network request failed: " + loadingFailed.getErrorText())
                    .source(loadingFailed.getRequestId().toString())
                    .details("Error: " + loadingFailed.getErrorText())
                    .timestamp(Instant.now())
                    .build();
                events.add(event);
                totalEventsCount.incrementAndGet();
                
                logger.debug("Captured network failure: {}", loadingFailed.getErrorText());
            } catch (Exception e) {
                logger.debug("Error processing network failure event", e);
            }
        });
        
        logger.info("Set up BiDi event listeners for session: {}", sessionId);
    }
    
    /**
     * Stop collecting events and clean up DevTools session
     */
    public void stopCollecting(MonitoringSession session) {
        String sessionId = session.getId();
        
        try {
            DevTools devTools = activeDevTools.remove(sessionId);
            if (devTools != null) {
                devTools.close();
                logger.debug("Closed DevTools session for: {}", sessionId);
            }
            
            List<BrowserEvent> events = sessionEvents.get(sessionId);
            if (events != null) {
                logger.info("Stopped BiDi event collection for session: {} (collected {} real events)", 
                           sessionId, events.size());
            }
        } catch (Exception e) {
            logger.error("Error stopping BiDi collection for session: {}", sessionId, e);
        }
    }
    
    /**
     * Get recent events for a session
     */
    public List<BrowserEvent> getRecentEvents(String sessionId, int limit) {
        List<BrowserEvent> events = sessionEvents.get(sessionId);
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Return the most recent events
        int fromIndex = Math.max(0, events.size() - limit);
        return new ArrayList<>(events.subList(fromIndex, events.size()));
    }
    
    /**
     * Get all events for a session
     */
    public List<BrowserEvent> getAllEvents(String sessionId) {
        List<BrowserEvent> events = sessionEvents.get(sessionId);
        return events != null ? new ArrayList<>(events) : new ArrayList<>();
    }
    
    /**
     * Get total events count across all sessions
     */
    public long getTotalEventsCount() {
        return totalEventsCount.get();
    }
    
    /**
     * Fallback simulation when BiDi is not available
     */
    private void simulateEventCollection(MonitoringSession session) {
        String sessionId = session.getId();
        List<BrowserEvent> events = sessionEvents.get(sessionId);
        
        logger.warn("Using simulated events for session: {} (BiDi not available)", sessionId);
        
        // Add some realistic simulated events
        events.add(BrowserEvent.consoleLog(sessionId, "INFO", "Page navigation started", "about:blank"));
        events.add(BrowserEvent.performanceMetric(sessionId, "navigation-start", System.currentTimeMillis()));
        events.add(BrowserEvent.consoleLog(sessionId, "INFO", "DOM content loaded", "test-page"));
        events.add(BrowserEvent.performanceMetric(sessionId, "dom-content-loaded", System.currentTimeMillis()));
        
        // Add a warning to indicate simulation
        events.add(BrowserEvent.consoleLog(sessionId, "WARN", 
                   "SeleniumIQ: Using simulated events - enable BiDi for real browser monitoring", 
                   "seleniumiq"));
        
        totalEventsCount.addAndGet(events.size());
    }
}