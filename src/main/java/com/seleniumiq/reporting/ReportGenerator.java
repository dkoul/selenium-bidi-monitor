package com.seleniumiq.reporting;

import com.seleniumiq.config.MonitorConfig;
import com.seleniumiq.model.BrowserEvent;
import com.seleniumiq.model.MonitoringSession;
import com.seleniumiq.model.AnalysisResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Generates monitoring reports in JSON and HTML formats
 */
public class ReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);
    
    private final MonitorConfig config;
    private final ObjectMapper objectMapper;
    private final Path reportsDirectory;
    
    public ReportGenerator(MonitorConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Create reports directory
        this.reportsDirectory = Paths.get("./monitoring-reports");
        createReportsDirectory();
        
        logger.info("Report Generator initialized - reports directory: {}", reportsDirectory.toAbsolutePath());
    }
    
    /**
     * Generate comprehensive report for all sessions
     */
    public String generateComprehensiveReport(Collection<MonitoringSession> sessions) {
        logger.info("Generating comprehensive report for {} sessions", sessions.size());
        
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("reportType", "comprehensive");
            report.put("timestamp", Instant.now());
            report.put("generatedBy", "SeleniumIQ v1.0.0");
            report.put("totalSessions", sessions.size());
            
            List<Map<String, Object>> sessionSummaries = new ArrayList<>();
            for (MonitoringSession session : sessions) {
                Map<String, Object> sessionSummary = new HashMap<>();
                sessionSummary.put("sessionId", session.getId());
                sessionSummary.put("sessionName", session.getName());
                sessionSummary.put("startTime", session.getStartTime());
                sessionSummary.put("status", "completed");
                sessionSummaries.add(sessionSummary);
            }
            report.put("sessions", sessionSummaries);
            
            // Generate filename with timestamp
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                .format(Instant.now().atZone(java.time.ZoneId.systemDefault()));
            String fileName = String.format("seleniumiq-comprehensive-%s.json", timestamp);
            Path reportPath = reportsDirectory.resolve(fileName);
            
            // Write report to file
            objectMapper.writeValue(reportPath.toFile(), report);
            
            logger.info("Comprehensive report generated: {}", reportPath.toAbsolutePath());
            return reportPath.toAbsolutePath().toString();
            
        } catch (IOException e) {
            logger.error("Failed to generate comprehensive report", e);
            throw new RuntimeException("Report generation failed", e);
        }
    }
    
    /**
     * Generate report for a specific session
     */
    public void generateSessionReport(MonitoringSession session, List<BrowserEvent> events) {
        generateSessionReport(session, events, null);
    }
    
    /**
     * Generate report for a specific session with analysis results
     */
    public void generateSessionReport(MonitoringSession session, List<BrowserEvent> events, AnalysisResult analysisResult) {
        logger.info("Generating session report for: {} with {} events", 
                   session.getName(), events.size());
        
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("reportType", "session");
            report.put("timestamp", Instant.now());
            report.put("generatedBy", "SeleniumIQ v1.0.0");
            
            // Session information
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionId", session.getId());
            sessionInfo.put("sessionName", session.getName());
            sessionInfo.put("startTime", session.getStartTime());
            sessionInfo.put("duration", calculateDuration(session.getStartTime()));
            report.put("session", sessionInfo);
            
            // Events summary
            Map<String, Object> eventsSummary = new HashMap<>();
            eventsSummary.put("totalEvents", events.size());
            eventsSummary.put("eventTypes", summarizeEventTypes(events));
            report.put("eventsSummary", eventsSummary);
            
            // Detailed events (limit to avoid huge files)
            List<Map<String, Object>> eventDetails = new ArrayList<>();
            int maxEvents = Math.min(events.size(), 50); // Limit to 50 events
            for (int i = 0; i < maxEvents; i++) {
                BrowserEvent event = events.get(i);
                Map<String, Object> eventDetail = new HashMap<>();
                eventDetail.put("timestamp", event.getTimestamp());
                eventDetail.put("type", event.getType());
                eventDetail.put("level", event.getLevel());
                eventDetail.put("message", event.getMessage());
                eventDetail.put("source", event.getSource());
                if (event.getDetails() != null && !event.getDetails().isEmpty()) {
                    eventDetail.put("details", event.getDetails());
                }
                eventDetails.add(eventDetail);
            }
            report.put("events", eventDetails);
            
            if (events.size() > maxEvents) {
                report.put("eventsNote", String.format("Showing first %d of %d events", maxEvents, events.size()));
            }
            
            // Analysis results if available
            if (analysisResult != null) {
                Map<String, Object> analysis = new HashMap<>();
                analysis.put("summary", analysisResult.getSummary());
                analysis.put("severity", analysisResult.getSeverity().toString());
                analysis.put("issuesCount", analysisResult.getIssues().size());
                analysis.put("recommendationsCount", analysisResult.getRecommendations().size());
                
                if (!analysisResult.getIssues().isEmpty()) {
                    List<Map<String, Object>> issues = new ArrayList<>();
                    analysisResult.getIssues().forEach(issue -> {
                        Map<String, Object> issueMap = new HashMap<>();
                        issueMap.put("type", issue.getType());
                        issueMap.put("title", issue.getTitle());
                        issueMap.put("description", issue.getDescription());
                        issueMap.put("suggestion", issue.getSuggestion());
                        issueMap.put("priority", issue.getPriority().toString());
                        issueMap.put("impact", issue.getImpact());
                        issues.add(issueMap);
                    });
                    analysis.put("issues", issues);
                }
                
                if (!analysisResult.getRecommendations().isEmpty()) {
                    List<Map<String, Object>> recommendations = new ArrayList<>();
                    analysisResult.getRecommendations().forEach(rec -> {
                        Map<String, Object> recMap = new HashMap<>();
                        recMap.put("category", rec.getCategory());
                        recMap.put("recommendation", rec.getRecommendation());
                        recMap.put("reasoning", rec.getReasoning());
                        recommendations.add(recMap);
                    });
                    analysis.put("recommendations", recommendations);
                }
                
                report.put("aiAnalysis", analysis);
            }
            
            // Generate filename
            String sessionPrefix = session.getId().substring(0, 8);
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                .format(Instant.now().atZone(java.time.ZoneId.systemDefault()));
            String fileName = String.format("seleniumiq-session-%s-%s.json", sessionPrefix, timestamp);
            Path reportPath = reportsDirectory.resolve(fileName);
            
            // Write JSON report to file
            objectMapper.writeValue(reportPath.toFile(), report);
            
            // Generate HTML report
            generateHtmlSessionReport(session, events, analysisResult, sessionPrefix, timestamp);
            
            logger.info("Session reports generated: {} (JSON & HTML)", reportPath.toAbsolutePath());
            
        } catch (IOException e) {
            logger.error("Failed to generate session report for: {}", session.getName(), e);
        }
    }
    
    /**
     * Create reports directory if it doesn't exist
     */
    private void createReportsDirectory() {
        try {
            if (!Files.exists(reportsDirectory)) {
                Files.createDirectories(reportsDirectory);
                logger.info("Created monitoring reports directory: {}", reportsDirectory.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to create reports directory: {}", reportsDirectory, e);
            throw new RuntimeException("Cannot create reports directory", e);
        }
    }
    
    /**
     * Summarize event types and their counts
     */
    private Map<String, Integer> summarizeEventTypes(List<BrowserEvent> events) {
        Map<String, Integer> typeCounts = new HashMap<>();
        for (BrowserEvent event : events) {
            typeCounts.merge(event.getType(), 1, Integer::sum);
        }
        return typeCounts;
    }
    
    /**
     * Calculate duration from start time to now
     */
    private String calculateDuration(Instant startTime) {
        long durationMillis = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        long seconds = durationMillis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Generate HTML report for a session
     */
    private void generateHtmlSessionReport(MonitoringSession session, List<BrowserEvent> events, 
                                          AnalysisResult analysisResult, String sessionPrefix, String timestamp) {
        try {
            String htmlFileName = String.format("seleniumiq-session-%s-%s.html", sessionPrefix, timestamp);
            Path htmlPath = reportsDirectory.resolve(htmlFileName);
            
            String htmlContent = buildHtmlReport(session, events, analysisResult);
            Files.write(htmlPath, htmlContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            logger.info("HTML report generated: {}", htmlPath.toAbsolutePath());
            
        } catch (IOException e) {
            logger.error("Failed to generate HTML report for session: {}", session.getName(), e);
        }
    }
    
    /**
     * Build HTML report content
     */
    private String buildHtmlReport(MonitoringSession session, List<BrowserEvent> events, AnalysisResult analysisResult) {
        StringBuilder html = new StringBuilder();
        
        // HTML Header with CSS
        html.append("""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SeleniumIQ Test Report - %s</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            line-height: 1.6;
            color: #333;
            background: #f8f9fa;
            padding: 20px;
        }
        
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        
        .header {
            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        
        .header h1 {
            font-size: 2.5em;
            margin-bottom: 10px;
            font-weight: 300;
        }
        
        .header .subtitle {
            font-size: 1.2em;
            opacity: 0.9;
        }
        
        .content {
            padding: 30px;
        }
        
        .section {
            margin-bottom: 40px;
        }
        
        .section h2 {
            color: #2c3e50;
            border-bottom: 3px solid #3498db;
            padding-bottom: 10px;
            margin-bottom: 20px;
            font-size: 1.8em;
        }
        
        .info-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        
        .info-card {
            background: #f8f9fa;
            border: 1px solid #e9ecef;
            border-radius: 8px;
            padding: 20px;
            text-align: center;
        }
        
        .info-card h3 {
            color: #495057;
            margin-bottom: 10px;
            font-size: 0.9em;
            text-transform: uppercase;
            letter-spacing: 1px;
        }
        
        .info-card .value {
            font-size: 1.8em;
            font-weight: bold;
            color: #2c3e50;
        }
        
        .severity-low { color: #28a745; }
        .severity-medium { color: #ffc107; }
        .severity-high { color: #fd7e14; }
        .severity-critical { color: #dc3545; }
        
        .issue-card, .recommendation-card {
            background: white;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 20px;
            margin-bottom: 15px;
            border-left: 4px solid #3498db;
        }
        
        .issue-card.priority-high { border-left-color: #fd7e14; }
        .issue-card.priority-critical { border-left-color: #dc3545; }
        .issue-card.priority-medium { border-left-color: #ffc107; }
        .issue-card.priority-low { border-left-color: #28a745; }
        
        .issue-title {
            font-size: 1.2em;
            font-weight: bold;
            margin-bottom: 10px;
            color: #2c3e50;
        }
        
        .issue-type {
            display: inline-block;
            background: #e9ecef;
            color: #495057;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 0.8em;
            margin-bottom: 10px;
            text-transform: uppercase;
            font-weight: 500;
        }
        
        .priority-badge {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 0.8em;
            font-weight: bold;
            text-transform: uppercase;
            margin-left: 10px;
        }
        
        .priority-high { background: #fd7e14; color: white; }
        .priority-critical { background: #dc3545; color: white; }
        .priority-medium { background: #ffc107; color: #212529; }
        .priority-low { background: #28a745; color: white; }
        
        .events-table {
            width: 100%%;
            border-collapse: collapse;
            margin-top: 20px;
            font-size: 0.9em;
        }
        
        .events-table th {
            background: #f8f9fa;
            padding: 12px;
            text-align: left;
            border-bottom: 2px solid #dee2e6;
            font-weight: 600;
            color: #495057;
        }
        
        .events-table td {
            padding: 12px;
            border-bottom: 1px solid #dee2e6;
            vertical-align: top;
        }
        
        .events-table tr:hover {
            background: #f8f9fa;
        }
        
        .event-type {
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 0.8em;
            font-weight: 500;
        }
        
        .event-console-log { background: #e3f2fd; color: #1565c0; }
        .event-network-request { background: #e8f5e8; color: #2e7d32; }
        .event-javascript-exception { background: #ffebee; color: #c62828; }
        .event-performance-metric { background: #fff3e0; color: #ef6c00; }
        
        .level-info { color: #17a2b8; }
        .level-warn { color: #ffc107; }
        .level-error { color: #dc3545; }
        
        .timestamp {
            font-family: 'Monaco', 'Menlo', monospace;
            font-size: 0.8em;
            color: #6c757d;
        }
        
        .no-issues {
            text-align: center;
            padding: 40px;
            color: #28a745;
            font-size: 1.2em;
        }
        
        .footer {
            background: #f8f9fa;
            padding: 20px;
            text-align: center;
            color: #6c757d;
            border-top: 1px solid #dee2e6;
        }
        
        .collapsible {
            cursor: pointer;
            user-select: none;
        }
        
        .collapsible:hover {
            background: #f8f9fa;
        }
        
        .collapsible-content {
            max-height: 300px;
            overflow-y: auto;
        }
    </style>
</head>
<body>
""".formatted(session.getName()));
        
        // Header
        html.append("""
    <div class="container">
        <div class="header">
            <h1>🧠 SeleniumIQ Test Report</h1>
            <div class="subtitle">AI-Powered Test Intelligence</div>
        </div>
        
        <div class="content">
""");
        
        // Session Information
        html.append(buildSessionInfoSection(session, events));
        
        // AI Analysis Section
        if (analysisResult != null) {
            html.append(buildAnalysisSection(analysisResult));
        }
        
        // Events Section
        html.append(buildEventsSection(events));
        
        // Footer
        html.append("""
        </div>
        
        <div class="footer">
            <p>Generated by <strong>SeleniumIQ v1.0.0</strong> at %s</p>
            <p>AI-Powered Test Intelligence for Selenium WebDriver</p>
        </div>
    </div>
    
    <script>
        // Add collapsible functionality
        document.querySelectorAll('.collapsible').forEach(function(element) {
            element.addEventListener('click', function() {
                const content = this.nextElementSibling;
                if (content.style.display === 'none') {
                    content.style.display = 'block';
                } else {
                    content.style.display = 'none';
                }
            });
        });
    </script>
</body>
</html>
""".formatted(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(Instant.now().atZone(java.time.ZoneId.systemDefault()))));
        
        return html.toString();
    }
    
    /**
     * Build session information section
     */
    private String buildSessionInfoSection(MonitoringSession session, List<BrowserEvent> events) {
        Map<String, Integer> eventTypes = summarizeEventTypes(events);
        
        return """
            <div class="section">
                <h2>📊 Session Overview</h2>
                <div class="info-grid">
                    <div class="info-card">
                        <h3>Session Name</h3>
                        <div class="value">%s</div>
                    </div>
                    <div class="info-card">
                        <h3>Duration</h3>
                        <div class="value">%s</div>
                    </div>
                    <div class="info-card">
                        <h3>Total Events</h3>
                        <div class="value">%d</div>
                    </div>
                    <div class="info-card">
                        <h3>Start Time</h3>
                        <div class="value timestamp">%s</div>
                    </div>
                </div>
                
                <div class="info-grid">
                    %s
                </div>
            </div>
        """.formatted(
            session.getName(),
            calculateDuration(session.getStartTime()),
            events.size(),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(session.getStartTime().atZone(java.time.ZoneId.systemDefault())),
            buildEventTypeCards(eventTypes)
        );
    }
    
    /**
     * Build event type cards
     */
    private String buildEventTypeCards(Map<String, Integer> eventTypes) {
        StringBuilder cards = new StringBuilder();
        
        eventTypes.forEach((type, count) -> {
            String displayName = type.replace("-", " ");
            displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
            
            cards.append("""
                <div class="info-card">
                    <h3>%s</h3>
                    <div class="value">%d</div>
                </div>
            """.formatted(displayName, count));
        });
        
        return cards.toString();
    }
    
    /**
     * Build AI analysis section
     */
    private String buildAnalysisSection(AnalysisResult analysisResult) {
        StringBuilder section = new StringBuilder();
        
        String severityClass = "severity-" + analysisResult.getSeverity().toString().toLowerCase();
        
        section.append("""
            <div class="section">
                <h2>🤖 AI Analysis Results</h2>
                <div class="info-grid">
                    <div class="info-card">
                        <h3>Severity</h3>
                        <div class="value %s">%s</div>
                    </div>
                    <div class="info-card">
                        <h3>Issues Found</h3>
                        <div class="value">%d</div>
                    </div>
                    <div class="info-card">
                        <h3>Recommendations</h3>
                        <div class="value">%d</div>
                    </div>
                    <div class="info-card">
                        <h3>Summary</h3>
                        <div class="value" style="font-size: 1em;">%s</div>
                    </div>
                </div>
        """.formatted(
            severityClass,
            analysisResult.getSeverity(),
            analysisResult.getIssues().size(),
            analysisResult.getRecommendations().size(),
            analysisResult.getSummary()
        ));
        
        // Issues
        if (!analysisResult.getIssues().isEmpty()) {
            section.append("<h3>🚨 Issues Detected</h3>");
            analysisResult.getIssues().forEach(issue -> {
                String priorityClass = "priority-" + issue.getPriority().toString().toLowerCase();
                section.append("""
                    <div class="issue-card %s">
                        <div class="issue-title">
                            %s
                            <span class="priority-badge %s">%s</span>
                        </div>
                        <div class="issue-type">%s</div>
                        <p><strong>Description:</strong> %s</p>
                        <p><strong>Suggestion:</strong> %s</p>
                        <p><strong>Impact:</strong> %s</p>
                    </div>
                """.formatted(
                    priorityClass,
                    issue.getTitle(),
                    priorityClass,
                    issue.getPriority(),
                    issue.getType(),
                    issue.getDescription(),
                    issue.getSuggestion(),
                    issue.getImpact()
                ));
            });
        } else {
            section.append("<div class=\"no-issues\">✅ No issues detected - Great job!</div>");
        }
        
        // Recommendations
        if (!analysisResult.getRecommendations().isEmpty()) {
            section.append("<h3>💡 Recommendations</h3>");
            analysisResult.getRecommendations().forEach(rec -> {
                section.append("""
                    <div class="recommendation-card">
                        <div class="issue-title">%s</div>
                        <div class="issue-type">%s</div>
                        <p><strong>Recommendation:</strong> %s</p>
                        <p><strong>Reasoning:</strong> %s</p>
                    </div>
                """.formatted(
                    rec.getCategory().replace("-", " ").toUpperCase(),
                    rec.getCategory(),
                    rec.getRecommendation(),
                    rec.getReasoning()
                ));
            });
        }
        
        section.append("</div>");
        return section.toString();
    }
    
    /**
     * Build events section
     */
    private String buildEventsSection(List<BrowserEvent> events) {
        StringBuilder section = new StringBuilder();
        
        section.append("""
            <div class="section">
                <h2 class="collapsible">📋 Browser Events (%d events) - Click to toggle</h2>
                <div class="collapsible-content">
                    <table class="events-table">
                        <thead>
                            <tr>
                                <th>Timestamp</th>
                                <th>Type</th>
                                <th>Level</th>
                                <th>Message</th>
                                <th>Source</th>
                            </tr>
                        </thead>
                        <tbody>
        """.formatted(events.size()));
        
        int maxEvents = Math.min(events.size(), 50);
        for (int i = 0; i < maxEvents; i++) {
            BrowserEvent event = events.get(i);
            String typeClass = "event-" + event.getType().replace("_", "-");
            String levelClass = event.getLevel() != null ? "level-" + event.getLevel().toLowerCase() : "";
            
            section.append("""
                            <tr>
                                <td class="timestamp">%s</td>
                                <td><span class="event-type %s">%s</span></td>
                                <td class="%s">%s</td>
                                <td>%s</td>
                                <td>%s</td>
                            </tr>
            """.formatted(
                DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(event.getTimestamp().atZone(java.time.ZoneId.systemDefault())),
                typeClass,
                event.getType().replace("-", " "),
                levelClass,
                event.getLevel() != null ? event.getLevel() : "",
                escapeHtml(event.getMessage()),
                escapeHtml(event.getSource() != null ? event.getSource() : "")
            ));
        }
        
        section.append("                        </tbody>\n                    </table>");
        
        if (events.size() > maxEvents) {
            section.append(String.format("\n                    <p style=\"text-align: center; margin-top: 20px; color: #6c757d;\">Showing first %d of %d events</p>", maxEvents, events.size()));
        }
        
        section.append("\n                </div>\n            </div>");
        
        return section.toString();
    }
    
    /**
     * Escape HTML characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    public void shutdown() {
        logger.info("Report Generator shutdown");
    }
}