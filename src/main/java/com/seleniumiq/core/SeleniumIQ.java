package com.seleniumiq.core;

import com.seleniumiq.config.MonitorConfig;
import com.seleniumiq.events.EventCollector;
import com.seleniumiq.analysis.LLMAnalysisService;
import com.seleniumiq.reporting.ReportGenerator;
import com.seleniumiq.model.MonitoringSession;
import com.seleniumiq.model.BrowserEvent;
import com.seleniumiq.model.AnalysisResult;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

/**
 * Main SeleniumIQ monitoring class that orchestrates event collection, analysis, and reporting.
 * This class serves as the primary entry point for integrating intelligent monitoring into existing Selenium tests.
 */
public class SeleniumIQ {
    private static final Logger logger = LoggerFactory.getLogger(SeleniumIQ.class);
    
    private final MonitorConfig config;
    private final EventCollector eventCollector;
    private final LLMAnalysisService analysisService;
    private final ReportGenerator reportGenerator;
    private final ScheduledExecutorService analysisScheduler;
    
    // Active monitoring sessions
    private final Map<String, MonitoringSession> activeSessions = new ConcurrentHashMap<>();
    
    // Singleton instance for global access
    private static volatile SeleniumIQ instance;
    
    /**
     * Private constructor for singleton pattern
     */
    private SeleniumIQ() {
        this.config = MonitorConfig.load();
        this.eventCollector = new EventCollector(config);
        this.analysisService = new LLMAnalysisService(config);
        this.reportGenerator = new ReportGenerator(config);
        this.analysisScheduler = Executors.newScheduledThreadPool(2);
        
        // Schedule periodic analysis
        schedulePeriodicAnalysis();
        
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        
        logger.info("SeleniumIQ initialized with config: {}", config.getProvider());
    }
    
    /**
     * Get the singleton instance of SeleniumIQ
     */
    public static SeleniumIQ getInstance() {
        if (instance == null) {
            synchronized (SeleniumIQ.class) {
                if (instance == null) {
                    instance = new SeleniumIQ();
                }
            }
        }
        return instance;
    }
    
    /**
     * Start monitoring a WebDriver session
     * 
     * @param driver The WebDriver instance to monitor
     * @return Session ID for tracking this monitoring session
     */
    public String startMonitoring(WebDriver driver) {
        return startMonitoring(driver, null);
    }
    
    /**
     * Start monitoring a WebDriver session with custom session name
     * 
     * @param driver The WebDriver instance to monitor
     * @param sessionName Custom name for the session (optional)
     * @return Session ID for tracking this monitoring session
     */
    public String startMonitoring(WebDriver driver, String sessionName) {
        if (!config.isMonitoringEnabled()) {
            logger.warn("Monitoring is disabled in configuration");
            return null;
        }
        
        String sessionId = UUID.randomUUID().toString();
        String name = sessionName != null ? sessionName : "Session-" + sessionId.substring(0, 8);
        
        try {
            // Verify monitoring is enabled for this driver
            if (!isMonitoringEnabled(driver)) {
                throw new IllegalStateException("WebDriver monitoring is not enabled. Please configure your driver with monitoring support.");
            }
            
            MonitoringSession session = new MonitoringSession(sessionId, name, driver, Instant.now());
            activeSessions.put(sessionId, session);
            
            // Start event collection for this session
            eventCollector.startCollecting(session);
            
            logger.info("Started monitoring session: {} ({})", name, sessionId);
            
            return sessionId;
            
        } catch (Exception e) {
            logger.error("Failed to start monitoring for session: {}", name, e);
            throw new RuntimeException("Failed to start SeleniumIQ monitoring", e);
        }
    }
    
    /**
     * Stop monitoring a specific session
     * 
     * @param sessionId The session ID to stop monitoring
     */
    public void stopMonitoring(String sessionId) {
        MonitoringSession session = activeSessions.remove(sessionId);
        if (session != null) {
            try {
                eventCollector.stopCollecting(session);
                
                // Generate final report for this session
                generateSessionReport(session);
                
                logger.info("Stopped monitoring session: {} ({})", session.getName(), sessionId);
                
            } catch (Exception e) {
                logger.error("Error stopping monitoring for session: {}", sessionId, e);
            }
        }
    }
    
    /**
     * Stop monitoring all active sessions
     */
    public void stopAllMonitoring() {
        logger.info("Stopping all active monitoring sessions: {}", activeSessions.size());
        
        activeSessions.keySet().forEach(this::stopMonitoring);
    }
    
    /**
     * Get real-time suggestions for a specific session
     * 
     * @param sessionId The session ID to analyze
     * @return CompletableFuture containing analysis results
     */
    public CompletableFuture<AnalysisResult> getRealtimeSuggestions(String sessionId) {
        MonitoringSession session = activeSessions.get(sessionId);
        if (session == null) {
            return CompletableFuture.completedFuture(
                AnalysisResult.error("Session not found: " + sessionId)
            );
        }
        
        List<BrowserEvent> recentEvents = eventCollector.getRecentEvents(sessionId, 20);
        return analysisService.analyzeEvents(recentEvents, session);
    }
    
    /**
     * Configure WebDriver options to enable monitoring support
     * 
     * @param options The browser options to configure
     * @return The configured options with monitoring enabled
     */
    public static <T> T enableMonitoring(T options) {
        if (options instanceof ChromeOptions) {
            ((ChromeOptions) options).setCapability("webSocketUrl", true);
            logger.debug("Enabled monitoring for Chrome");
        } else if (options instanceof FirefoxOptions) {
            ((FirefoxOptions) options).setCapability("webSocketUrl", true);
            logger.debug("Enabled monitoring for Firefox");
        } else if (options instanceof EdgeOptions) {
            ((EdgeOptions) options).setCapability("webSocketUrl", true);
            logger.debug("Enabled monitoring for Edge");
        } else {
            logger.warn("Unknown browser options type: {}. Monitoring may not be enabled.", options.getClass().getSimpleName());
        }
        
        return options;
    }
    
    /**
     * Generate a comprehensive monitoring report for all sessions
     * 
     * @return CompletableFuture containing the generated report path
     */
    public CompletableFuture<String> generateReport() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return reportGenerator.generateComprehensiveReport(activeSessions.values());
            } catch (Exception e) {
                logger.error("Failed to generate comprehensive report", e);
                throw new RuntimeException("Report generation failed", e);
            }
        });
    }
    
    /**
     * Get current monitoring statistics
     * 
     * @return Map containing monitoring statistics
     */
    public Map<String, Object> getMonitoringStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("activeSessions", activeSessions.size());
        stats.put("totalEventsCollected", eventCollector.getTotalEventsCount());
        stats.put("analysisQueueSize", analysisService.getQueueSize());
        stats.put("configProvider", config.getProvider());
        stats.put("monitoringEnabled", config.isMonitoringEnabled());
        
        return stats;
    }
    
    /**
     * Check if monitoring is enabled for the given WebDriver
     */
    private boolean isMonitoringEnabled(WebDriver driver) {
        try {
            // Try to access BiDi capabilities - this is a simplified check
            // In reality, you'd check the actual BiDi connection
            if (driver instanceof org.openqa.selenium.remote.RemoteWebDriver) {
                org.openqa.selenium.remote.RemoteWebDriver remoteDriver = 
                    (org.openqa.selenium.remote.RemoteWebDriver) driver;
                return driver.toString().contains("BiDi") ||
                       remoteDriver.getCapabilities().getCapability("webSocketUrl") != null;
            }
            return driver.toString().contains("BiDi");
        } catch (Exception e) {
            logger.debug("Monitoring check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Schedule periodic analysis of collected events
     */
    private void schedulePeriodicAnalysis() {
        if (!config.isRealtimeSuggestionsEnabled()) {
            return;
        }
        
        long intervalSeconds = config.getAnalysisInterval().getSeconds();
        
        analysisScheduler.scheduleAtFixedRate(() -> {
            try {
                for (MonitoringSession session : activeSessions.values()) {
                    List<BrowserEvent> events = eventCollector.getRecentEvents(session.getId(), config.getBatchSize());
                    if (!events.isEmpty()) {
                        analysisService.analyzeEvents(events, session)
                            .thenAccept(result -> {
                                if (result.hasIssues()) {
                                    logger.info("Analysis found {} issues in session: {}", 
                                              result.getIssues().size(), session.getName());
                                }
                            })
                            .exceptionally(throwable -> {
                                logger.error("Periodic analysis failed for session: {}", session.getName(), throwable);
                                return null;
                            });
                    }
                }
            } catch (Exception e) {
                logger.error("Error in periodic analysis", e);
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        
        logger.info("Scheduled periodic analysis every {} seconds", intervalSeconds);
    }
    
    /**
     * Generate a final report for a completed session
     */
    private void generateSessionReport(MonitoringSession session) {
        try {
            List<BrowserEvent> allEvents = eventCollector.getAllEvents(session.getId());
            if (!allEvents.isEmpty()) {
                // Get final analysis for the session
                analysisService.analyzeEvents(allEvents, session)
                    .thenAccept(analysisResult -> {
                        // Generate report with analysis results
                        reportGenerator.generateSessionReport(session, allEvents, analysisResult);
                        logger.info("Generated report with AI analysis for session: {}", session.getName());
                    })
                    .exceptionally(throwable -> {
                        // Generate report without analysis if analysis fails
                        reportGenerator.generateSessionReport(session, allEvents);
                        logger.warn("Generated report without AI analysis for session: {} - analysis failed: {}", 
                                  session.getName(), throwable.getMessage());
                        return null;
                    });
            }
        } catch (Exception e) {
            logger.error("Failed to generate report for session: {}", session.getName(), e);
        }
    }
    
    /**
     * Shutdown the monitoring system gracefully
     */
    private void shutdown() {
        logger.info("Shutting down SeleniumIQ...");
        
        stopAllMonitoring();
        
        analysisScheduler.shutdown();
        try {
            if (!analysisScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                analysisScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            analysisScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        analysisService.shutdown();
        reportGenerator.shutdown();
        
        logger.info("SeleniumIQ shutdown complete");
    }
}