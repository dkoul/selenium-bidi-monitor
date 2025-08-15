package com.seleniumiq.integration;

import com.seleniumiq.core.SeleniumIQ;
import com.seleniumiq.model.AnalysisResult;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * JUnit 5 Test Watcher that automatically integrates SeleniumIQ monitoring with Selenium tests.
 *
 * Usage:
 * @ExtendWith(SeleniumIQWatcher.class)
 * public class MySeleniumTest {
 *     private WebDriver driver;
 *
 *     @Test
 *     void testSomething() {
 *         // Your test code here
 *         // Monitoring happens automatically
 *     }
 * }
 */
public class SeleniumIQWatcher implements TestWatcher {
    private static final Logger logger = LoggerFactory.getLogger(SeleniumIQWatcher.class);

    private final SeleniumIQ monitor = SeleniumIQ.getInstance();
    private final Map<String, String> activeMonitoringSessions = new ConcurrentHashMap<>();

    public void testStarted(ExtensionContext context) {
        String testId = getTestId(context);
        String testName = getTestName(context);

        logger.info("Starting SeleniumIQ monitoring for test: {}", testName);

        try {
            // Find WebDriver instance in the test class
            WebDriver driver = findWebDriverInstance(context);

            if (driver != null) {
                // Enable monitoring if not already enabled
                ensureMonitoringEnabled(driver);

                // Start monitoring
                String sessionId = monitor.startMonitoring(driver, testName);
                if (sessionId != null) {
                    activeMonitoringSessions.put(testId, sessionId);
                    logger.info("SeleniumIQ monitoring started for test: {} (session: {})", testName, sessionId);
                } else {
                    logger.warn("Failed to start SeleniumIQ monitoring for test: {}", testName);
                }
            } else {
                logger.debug("No WebDriver instance found for test: {}", testName);
            }

        } catch (Exception e) {
            logger.error("Error starting SeleniumIQ monitoring for test: {}", testName, e);
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        String testId = getTestId(context);
        String testName = getTestName(context);

        logger.info("Test successful: {}", testName);

        stopMonitoringAndAnalyze(testId, testName, "PASSED");
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testId = getTestId(context);
        String testName = getTestName(context);

        logger.warn("Test failed: {} - {}", testName, cause.getMessage());

        stopMonitoringAndAnalyze(testId, testName, "FAILED");
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        String testId = getTestId(context);
        String testName = getTestName(context);

        logger.warn("Test aborted: {} - {}", testName, cause.getMessage());

        stopMonitoringAndAnalyze(testId, testName, "ABORTED");
    }

    @Override
    public void testDisabled(ExtensionContext context, java.util.Optional<String> reason) {
        String testName = getTestName(context);
        logger.info("Test disabled: {} - {}", testName, reason.orElse("No reason provided"));
    }

    /**
     * Stop monitoring and perform analysis for the completed test
     */
    private void stopMonitoringAndAnalyze(String testId, String testName, String testResult) {
        String sessionId = activeMonitoringSessions.remove(testId);

        if (sessionId != null) {
            try {
                // Get real-time analysis before stopping
                CompletableFuture<AnalysisResult> analysisFuture = monitor.getRealtimeSuggestions(sessionId);

                // Stop monitoring
                monitor.stopMonitoring(sessionId);

                // Process analysis results
                analysisFuture.thenAccept(result -> {
                    if (result.hasIssues()) {
                        logger.warn("SeleniumIQ analysis found {} issues in test: {}",
                                  result.getIssues().size(), testName);

                        // Log critical issues
                        result.getIssues().stream()
                            .filter(issue -> issue.getPriority() == com.seleniumiq.model.Suggestion.Priority.CRITICAL
                                          || issue.getPriority() == com.seleniumiq.model.Suggestion.Priority.HIGH)
                            .forEach(issue -> {
                                logger.error("Critical issue in test {}: {} - {}",
                                             testName, issue.getTitle(), issue.getSuggestion());
                            });
                    }

                    if (result.hasRecommendations()) {
                        logger.info("SeleniumIQ analysis generated {} recommendations for test: {}",
                                  result.getRecommendations().size(), testName);
                    }

                    // Store results for reporting
                    storeAnalysisResults(testName, testResult, result);

                }).exceptionally(throwable -> {
                    logger.error("Failed to analyze test results for: {}", testName, throwable);
                    return null;
                });

                logger.info("SeleniumIQ monitoring stopped for test: {} (session: {})", testName, sessionId);

            } catch (Exception e) {
                logger.error("Error stopping SeleniumIQ monitoring for test: {}", testName, e);
            }
        }
    }

    /**
     * Find WebDriver instance in the test class
     */
    private WebDriver findWebDriverInstance(ExtensionContext context) {
        Object testInstance = context.getRequiredTestInstance();
        Class<?> testClass = testInstance.getClass();

        // Look for WebDriver fields
        for (Field field : testClass.getDeclaredFields()) {
            if (WebDriver.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    WebDriver driver = (WebDriver) field.get(testInstance);
                    if (driver != null) {
                        return driver;
                    }
                } catch (IllegalAccessException e) {
                    logger.debug("Could not access WebDriver field: {}", field.getName(), e);
                }
            }
        }

        // Look for WebDriver getter methods
        for (Method method : testClass.getDeclaredMethods()) {
            if (WebDriver.class.isAssignableFrom(method.getReturnType()) &&
                method.getParameterCount() == 0 &&
                (method.getName().startsWith("get") || method.getName().startsWith("driver"))) {
                try {
                    method.setAccessible(true);
                    WebDriver driver = (WebDriver) method.invoke(testInstance);
                    if (driver != null) {
                        return driver;
                    }
                } catch (Exception e) {
                    logger.debug("Could not invoke WebDriver method: {}", method.getName(), e);
                }
            }
        }

        return null;
    }

    /**
     * Ensure monitoring is enabled for the WebDriver
     */
    private void ensureMonitoringEnabled(WebDriver driver) {
        // This is a simplified check - in a real implementation, you'd need to
        // check if monitoring is already enabled and possibly restart the driver if needed
        try {
            if (driver instanceof org.openqa.selenium.remote.RemoteWebDriver) {
                org.openqa.selenium.remote.RemoteWebDriver remoteDriver = 
                    (org.openqa.selenium.remote.RemoteWebDriver) driver;
                Object capabilities = remoteDriver.getCapabilities();
                if (capabilities != null) {
                    // Check if webSocketUrl capability is present
                    Object webSocketUrl = remoteDriver.getCapabilities().getCapability("webSocketUrl");
                    if (webSocketUrl == null) {
                        logger.warn("WebDriver monitoring may not be enabled. Consider using SeleniumIQ.enableMonitoring() when creating your driver options.");
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not check monitoring capabilities", e);
        }
    }

    /**
     * Store analysis results for later reporting
     */
    private void storeAnalysisResults(String testName, String testResult, AnalysisResult analysisResult) {
        // Store results in a way that can be accessed by reporting systems
        // This could be extended to integrate with test reporting frameworks

        String resultKey = String.format("test.%s.analysis", testName.replaceAll("[^a-zA-Z0-9]", "_"));

        // Store in system properties for access by reporting tools
        System.setProperty(resultKey + ".summary", analysisResult.getSummary());
        System.setProperty(resultKey + ".severity", analysisResult.getSeverity().toString());
        System.setProperty(resultKey + ".issues.count", String.valueOf(analysisResult.getIssues().size()));
        System.setProperty(resultKey + ".recommendations.count", String.valueOf(analysisResult.getRecommendations().size()));
        System.setProperty(resultKey + ".test.result", testResult);

        logger.debug("Stored analysis results for test: {} with key: {}", testName, resultKey);
    }

    /**
     * Get unique test identifier
     */
    private String getTestId(ExtensionContext context) {
        return context.getUniqueId();
    }

    /**
     * Get human-readable test name
     */
    private String getTestName(ExtensionContext context) {
        return context.getDisplayName();
    }
}