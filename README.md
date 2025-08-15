# SeleniumIQ

A comprehensive monitoring plugin for Selenium WebDriver tests that leverages **WebDriver BiDi protocol** and **Chrome DevTools** to capture real browser events in real-time and uses Large Language Models (LLM) to generate intelligent suggestions for test optimization, error resolution, and performance improvements.

> **🔗 What is BiDi?** WebDriver BiDi enables **bidirectional communication** with browsers, allowing SeleniumIQ to listen to real console logs, network requests, JavaScript exceptions, and performance metrics as they happen. [Learn more about BiDi](BIDI_EXPLAINED.md)

## Features

### 🚀 **Real-time Browser Event Monitoring via BiDi**
- **Console Logs**: Capture actual JavaScript `console.log()`, `console.error()`, and warnings as they happen
- **Network Requests**: Monitor real HTTP requests, responses, failures, and timing data  
- **Performance Metrics**: Track actual page load times, resource usage, and performance bottlenecks
- **JavaScript Exceptions**: Detect real unhandled errors and promise rejections with stack traces
- **Security Violations**: Monitor actual CSP violations and mixed content warnings from the browser

### 🤖 **AI-Powered Analysis**
- **LLM Integration**: Support for OpenAI GPT-4, Anthropic Claude, Azure OpenAI, and Ollama
- **Intelligent Suggestions**: Get actionable recommendations for test improvements
- **Error Resolution**: Receive specific guidance for fixing detected issues
- **Performance Optimization**: Identify opportunities to improve test execution speed
- **Best Practices**: Learn about Selenium automation best practices

### 🔧 **Easy Integration**
- **Zero Code Changes**: Add monitoring with a simple annotation
- **JUnit 5 Integration**: Automatic test lifecycle management
- **Flexible Configuration**: Extensive configuration options via HOCON
- **Multiple LLM Providers**: Choose your preferred AI service

### 📊 **Comprehensive Reporting**
- **Real-time Analysis**: Get suggestions during test execution
- **Detailed Reports**: Generate comprehensive analysis reports
- **Issue Categorization**: Organize findings by type and priority
- **Test Correlation**: Link browser events to specific test methods

## Quick Start

### 1. Installation

**Option A: Download Pre-built JAR (Recommended)**

1. **Download the latest JAR** from [GitHub Releases](https://github.com/yourusername/seleniumiq/releases)
   - `seleniumiq-core-1.0.0.jar` (requires dependencies in your project)
   - `seleniumiq-core-1.0.0-standalone.jar` (includes all dependencies)

2. **Add to your Maven project:**

```xml
<!-- For regular JAR -->
<dependency>
    <groupId>com.seleniumiq</groupId>
    <artifactId>seleniumiq-core</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/seleniumiq-core-1.0.0.jar</systemPath>
</dependency>

<!-- OR for standalone JAR (no other dependencies needed) -->
<dependency>
    <groupId>com.seleniumiq</groupId>
    <artifactId>seleniumiq-core</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/seleniumiq-core-1.0.0-standalone.jar</systemPath>
</dependency>
```

**Option B: Build from Source**

```bash
git clone https://github.com/yourusername/seleniumiq.git
cd seleniumiq
./build.sh
# Copy target/seleniumiq-core-1.0.0.jar to your project
```

### 2. Project Structure

Create a `lib/` directory in your project root and place the downloaded JAR:

```
your-selenium-project/
├── src/
├── lib/
│   └── seleniumiq-core-1.0.0-standalone.jar  ← Place JAR here
├── pom.xml
└── ...
```

### 3. Configure LLM Provider

Set your API key as an environment variable:

```bash
export OPENAI_API_KEY="your-api-key-here"
```

### 4. Enable Monitoring in Your Tests

```java
@ExtendWith(SeleniumIQWatcher.class)
public class MySeleniumTest {
    private WebDriver driver;
    
    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        SeleniumIQ.enableMonitoring(options); // Enable monitoring support
        driver = new ChromeDriver(options);
    }
    
    @Test
    void testSomething() {
        driver.get("https://example.com");
        // Your test code here - monitoring happens automatically!
    }
    
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
```

That's it! The plugin will automatically:
- Start monitoring when your test begins
- Capture browser events during test execution
- Analyze events using AI when the test completes
- Generate suggestions and reports

## Configuration

### Basic Configuration

Create `src/main/resources/application.conf`:

```hocon
seleniumiq {
  llm {
    provider = "openai"  # openai, anthropic, ollama, azure-openai
    api {
      key = ${?OPENAI_API_KEY}
      model = "gpt-4"
      timeout = 30s
    }
  }
  
  monitoring {
    enabled = true
    events {
      console-logs = true
      network-requests = true
      performance-metrics = true
      javascript-exceptions = true
    }
  }
}
```

### Advanced Configuration

```hocon
seleniumiq {
  llm {
    provider = "openai"
    api {
      base-url = "https://api.openai.com/v1"
      key = ${?OPENAI_API_KEY}
      model = "gpt-4"
      timeout = 30s
      max-retries = 3
    }
  }
  
  monitoring {
    enabled = true
    
    events {
      console-logs = true
      network-requests = true
      performance-metrics = true
      dom-mutations = false
      javascript-exceptions = true
      security-violations = true
    }
    
    filters {
      min-log-level = "WARN"
      network {
        failed-requests-only = false
        excluded-domains = ["analytics.google.com"]
        included-content-types = ["application/json", "text/html"]
      }
      performance {
        slow-request-threshold = 5000  # milliseconds
        memory-usage-threshold = 100   # MB
        cpu-usage-threshold = 80       # percentage
      }
    }
    
    analysis {
      batch-size = 10
      analysis-interval = 30s
      real-time-suggestions = true
      categories = [
        "performance-optimization",
        "error-resolution", 
        "test-stability",
        "security-improvements",
        "best-practices"
      ]
    }
  }
}
```

## LLM Provider Configuration

### OpenAI
```hocon
llm {
  provider = "openai"
  api {
    key = ${?OPENAI_API_KEY}
    model = "gpt-4"
    base-url = "https://api.openai.com/v1"
  }
}
```

### Anthropic Claude
```hocon
llm {
  provider = "anthropic"
  api {
    key = ${?ANTHROPIC_API_KEY}
    model = "claude-3-sonnet-20240229"
    base-url = "https://api.anthropic.com"
  }
}
```

### Ollama (Local)
```hocon
llm {
  provider = "ollama"
  ollama {
    base-url = "http://localhost:11434"
    model = "llama2"
  }
}
```

### Azure OpenAI
```hocon
llm {
  provider = "azure-openai"
  azure {
    endpoint = ${?AZURE_OPENAI_ENDPOINT}
    key = ${?AZURE_OPENAI_KEY}
    deployment = "gpt-4"
    api-version = "2023-12-01-preview"
  }
}
```

## Usage Examples

### Manual Monitoring Control

```java
public class ManualMonitoringTest {
    private WebDriver driver;
    private SeleniumIQ monitor;
    
    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        SeleniumIQ.enableMonitoring(options);
        driver = new ChromeDriver(options);
        monitor = SeleniumIQ.getInstance();
    }
    
    @Test
    void testWithManualMonitoring() {
        // Start monitoring manually
        String sessionId = monitor.startMonitoring(driver, "Manual Test");
        
        try {
            driver.get("https://example.com");
            // Your test logic here
            
            // Get real-time suggestions
            CompletableFuture<AnalysisResult> analysis = monitor.getRealtimeSuggestions(sessionId);
            analysis.thenAccept(result -> {
                if (result.hasIssues()) {
                    System.out.println("Found " + result.getIssues().size() + " issues");
                    result.getIssues().forEach(issue -> 
                        System.out.println("- " + issue.getTitle() + ": " + issue.getSuggestion())
                    );
                }
            });
            
        } finally {
            // Stop monitoring
            monitor.stopMonitoring(sessionId);
        }
    }
}
```

### Performance Testing with Analysis

```java
@ExtendWith(SeleniumIQWatcher.class)
public class PerformanceTest {
    private WebDriver driver;
    
    @Test
    void testPageLoadPerformance() {
        driver.get("https://example.com");
        
        // Trigger performance measurement
        driver.executeScript("""
            const start = performance.now();
            // Simulate heavy operation
            for (let i = 0; i < 1000000; i++) {
                Math.random();
            }
            const duration = performance.now() - start;
            console.log('Operation took:', duration, 'ms');
        """);
        
        // SeleniumIQ will capture this and provide performance suggestions
    }
}
```

### Error Detection and Analysis

```java
@ExtendWith(SeleniumIQWatcher.class)
public class ErrorHandlingTest {
    private WebDriver driver;
    
    @Test
    void testErrorDetection() {
        driver.get("https://example.com");
        
        // Intentionally trigger JavaScript errors for analysis
        driver.executeScript("""
            try {
                nonExistentFunction();
            } catch (error) {
                console.error('JavaScript error detected:', error);
            }
        """);
        
        // SeleniumIQ will detect this error and suggest fixes
    }
}
```

## Analysis Results

The plugin generates detailed analysis results including:

### Issue Detection
```json
{
  "summary": "Found 3 performance issues and 1 JavaScript error",
  "severity": "MEDIUM",
  "issues": [
    {
      "type": "performance",
      "title": "Slow Network Request",
      "description": "Request to /api/data took 5.2 seconds",
      "suggestion": "Consider implementing request caching or optimizing the API endpoint",
      "priority": "HIGH",
      "impact": "May cause test timeouts and flaky behavior"
    }
  ]
}
```

### Recommendations
```json
{
  "recommendations": [
    {
      "category": "test-stability",
      "recommendation": "Add explicit waits instead of Thread.sleep()",
      "reasoning": "Explicit waits are more reliable and faster than fixed delays"
    }
  ]
}
```

## Best Practices

### 1. Configure Appropriate Filters
```hocon
filters {
  min-log-level = "WARN"  # Only capture warnings and errors
  network {
    excluded-domains = ["analytics.google.com", "doubleclick.net"]
  }
}
```

### 2. Use Batch Analysis for Performance
```hocon
analysis {
  batch-size = 20
  analysis-interval = 60s
  real-time-suggestions = false  # For high-volume test suites
}
```

### 3. Enable BiDi for All Browser Types
```java
// Chrome
ChromeOptions chromeOptions = new ChromeOptions();
SeleniumIQ.enableMonitoring(chromeOptions);

// Firefox  
FirefoxOptions firefoxOptions = new FirefoxOptions();
SeleniumIQ.enableMonitoring(firefoxOptions);

// Edge
EdgeOptions edgeOptions = new EdgeOptions();
SeleniumIQ.enableMonitoring(edgeOptions);
```

### 4. Handle Analysis Results
```java
monitor.getRealtimeSuggestions(sessionId)
    .thenAccept(result -> {
        // Log critical issues
        result.getIssues().stream()
            .filter(issue -> issue.getPriority() == Priority.CRITICAL)
            .forEach(issue -> logger.error("Critical: {}", issue.getSuggestion()));
            
        // Store recommendations for review
        result.getRecommendations().forEach(rec -> 
            reportingService.addRecommendation(rec)
        );
    });
```

## Troubleshooting

### BiDi Not Enabled
```
Error: WebDriver BiDi is not enabled
```
**Solution**: Ensure you call `SeleniumIQ.enableMonitoring(options)` before creating your WebDriver instance.

### LLM API Errors
```
Error: OpenAI API error: Invalid API key
```
**Solution**: Verify your API key is set correctly in environment variables.

### High Memory Usage
**Solution**: Reduce batch size and enable filtering:
```hocon
analysis {
  batch-size = 5
}
filters {
  min-log-level = "ERROR"
}
```

### Slow Analysis
**Solution**: Use a faster model or disable real-time analysis:
```hocon
llm.api.model = "gpt-3.5-turbo"  # Faster than gpt-4
analysis.real-time-suggestions = false
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Supported Browsers

- ✅ Chrome/Chromium (Full BiDi support)
- ✅ Firefox (Full BiDi support) 
- ✅ Edge (Full BiDi support)
- ⚠️ Safari (Limited BiDi support)

## Requirements

- Java 11 or higher
- Selenium WebDriver 4.19.1 or higher
- JUnit 5 (for automatic integration)
- Valid LLM API key (OpenAI, Anthropic, etc.)

## Support

For questions and support:
- 📧 Email: support@seleniumiq.com
- 💬 GitHub Issues: [Create an issue](https://github.com/seleniumiq/issues)
- 📖 Documentation: [Full documentation](https://docs.seleniumiq.com)

---

## 📦 Distribution & Releases

SeleniumIQ is distributed through **GitHub Releases** with pre-built JARs for easy integration.

### Release Process

1. **Automated Builds**: Every release is automatically built using GitHub Actions
2. **Two JAR Options**: 
   - Standard JAR (requires Selenium, Jackson, etc. in your project)
   - Standalone JAR (includes all dependencies - just drop in and use)
3. **Quality Assurance**: All JARs are tested before release

### Version Compatibility

| SeleniumIQ Version | Java Version | Selenium Version |
|-------------------|--------------|------------------|
| 1.0.0+            | 11+          | 4.19.1+          |

### Future Plans

- **Maven Central**: We plan to publish to Maven Central for easier dependency management
- **Gradle Plugin**: Native Gradle integration
- **Additional LLM Providers**: Anthropic Claude, Azure OpenAI support

## 🔄 Building from Source

For contributors and advanced users:

```bash
# Clone the repository
git clone https://github.com/yourusername/seleniumiq.git
cd seleniumiq

# Build with the provided script
./build.sh

# Or manually with Maven
mvn clean package assembly:single
```

**Generated artifacts:**
- `target/seleniumiq-core-1.0.0.jar` - Regular JAR
- `target/seleniumiq-core-1.0.0-jar-with-dependencies.jar` - Standalone JAR

---

**Happy Testing with AI-Powered Insights!** 🚀 