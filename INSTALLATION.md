# 📦 SeleniumIQ Installation Guide

Quick guide to get SeleniumIQ running in your Selenium test suite.

## 🚀 Quick Install (5 minutes)

### Step 1: Download JAR

1. Go to [SeleniumIQ Releases](https://github.com/yourusername/seleniumiq/releases)
2. Download the latest `seleniumiq-core-X.X.X-jar-with-dependencies.jar` (**standalone version recommended**)

### Step 2: Add to Your Project

1. **Create `lib/` directory** in your project root:
   ```
   your-project/
   ├── src/
   ├── lib/              ← Create this directory
   ├── pom.xml
   └── ...
   ```

2. **Copy the JAR** to `lib/` directory:
   ```
   lib/
   └── seleniumiq-core-1.0.0-jar-with-dependencies.jar
   ```

### Step 3: Update Maven Configuration

Add this to your `pom.xml`:

```xml
<dependencies>
    <!-- Your existing dependencies -->
    
    <!-- SeleniumIQ -->
    <dependency>
        <groupId>com.seleniumiq</groupId>
        <artifactId>seleniumiq-core</artifactId>
        <version>1.0.0</version>
        <scope>system</scope>
        <systemPath>${project.basedir}/lib/seleniumiq-core-1.0.0-jar-with-dependencies.jar</systemPath>
    </dependency>
</dependencies>
```

### Step 4: Configure AI Provider

**Option A: OpenAI (Cloud)**
```bash
export OPENAI_API_KEY="your-openai-api-key"
```

**Option B: Ollama (Local - Free)**
```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Download a model
ollama pull mistral:latest
```

### Step 5: Update Your Tests

Add one annotation to your test class:

```java
@ExtendWith(SeleniumIQWatcher.class)  // ← Add this line
public class MySeleniumTest {
    private WebDriver driver;
    
    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        SeleniumIQ.enableMonitoring(options);  // ← Add this line
        driver = new ChromeDriver(options);
    }
    
    @Test
    void myTest() {
        driver.get("https://example.com");
        // Your existing test code - no changes needed!
    }
}
```

### Step 6: Run Your Tests

```bash
mvn test
```

**That's it!** 🎉 SeleniumIQ will now:
- Monitor your browser events in real-time
- Analyze them with AI
- Generate beautiful HTML and JSON reports in `./monitoring-reports/`

## 📋 Verification

After running tests, you should see:

1. **Console output** with AI analysis
2. **Report directory** created: `./monitoring-reports/`
3. **HTML reports** you can open in any browser
4. **JSON reports** for programmatic analysis

Example console output:
```
[INFO] SeleniumIQ initialized with config: ollama
[INFO] Started monitoring session: MyTest (abc12345)
[INFO] Analysis found 2 issues in session: MyTest
[INFO] Generated report with AI analysis for session: MyTest
[INFO] Session reports generated: ./monitoring-reports/seleniumiq-session-abc12345-2024-01-15_10-30-45.html (JSON & HTML)
```

## 🔧 Configuration (Optional)

Create `src/main/resources/application.conf` for custom settings:

```hocon
seleniumiq {
  llm {
    provider = "ollama"  # or "openai"
    ollama {
      base-url = "http://localhost:11434"
      model = "mistral:latest"
    }
  }
  
  monitoring {
    analysis {
      real-time-suggestions = true
      analysis-interval = 30s
    }
  }
}
```

## 🛠️ Gradle Users

Add to your `build.gradle`:

```gradle
dependencies {
    implementation files('lib/seleniumiq-core-1.0.0-jar-with-dependencies.jar')
}
```

## ❓ Troubleshooting

### "Class not found" errors
- Ensure JAR is in `lib/` directory
- Check `systemPath` in `pom.xml` matches actual file location

### "BiDi not enabled" warnings
- Ensure you call `SeleniumIQ.enableMonitoring(options)` before creating WebDriver
- Update to Selenium 4.19.1+ for best BiDi support

### No reports generated
- Check if `monitoring-reports/` directory was created
- Look for errors in console output
- Verify AI provider is configured correctly

### AI analysis not working
- **OpenAI**: Verify `OPENAI_API_KEY` environment variable is set
- **Ollama**: Ensure Ollama is running (`ollama serve`) and model is downloaded

## 🆘 Need Help?

- 📖 [Full Documentation](README.md)
- 🐛 [Report Issues](https://github.com/yourusername/seleniumiq/issues)
- 💬 [Discussions](https://github.com/yourusername/seleniumiq/discussions)

---

**Get AI-powered insights into your Selenium tests in under 5 minutes!** 🧠✨