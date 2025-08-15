package com.seleniumiq.model;

import org.openqa.selenium.WebDriver;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a monitoring session for a WebDriver instance
 */
public class MonitoringSession {
    private final String id;
    private final String name;
    private final WebDriver driver;
    private final Instant startTime;
    private Instant endTime;
    private String status;
    
    public MonitoringSession(String id, String name, WebDriver driver, Instant startTime) {
        this.id = id;
        this.name = name;
        this.driver = driver;
        this.startTime = startTime;
        this.status = "ACTIVE";
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public WebDriver getDriver() {
        return driver;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
    
    public void stop() {
        this.status = "STOPPED";
        this.endTime = Instant.now();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonitoringSession that = (MonitoringSession) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("MonitoringSession{id='%s', name='%s', status='%s', startTime=%s}",
                id, name, status, startTime);
    }
} 