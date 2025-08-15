#!/bin/bash

# SeleniumIQ Build Script
# This script builds SeleniumIQ locally for development and testing

set -e  # Exit on any error

echo "🧠 Building SeleniumIQ..."
echo "=========================="

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven first."
    echo "   Visit: https://maven.apache.org/install.html"
    exit 1
fi

# Check Java version
echo "☕ Checking Java version..."
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$java_version" -lt 11 ]; then
    echo "❌ Java 11 or higher is required. Current version: $java_version"
    exit 1
fi
echo "✅ Java version: $(java -version 2>&1 | head -n1)"

# Clean previous builds
echo ""
echo "🧹 Cleaning previous builds..."
mvn clean

# Compile and package
echo ""
echo "🔨 Compiling and packaging..."
mvn compile package -DskipTests

# Run tests
echo ""
echo "🧪 Running tests..."
if mvn test; then
    echo "✅ All tests passed!"
else
    echo "⚠️  Some tests failed, but continuing with build..."
fi

# Create JAR with dependencies (standalone)
echo ""
echo "📦 Creating standalone JAR..."
mvn assembly:single

# Check if JARs were created
if [ -f "target/seleniumiq-core-1.0.0.jar" ]; then
    echo ""
    echo "🎉 Build completed successfully!"
    echo "=========================="
    echo "📁 Generated files:"
    echo "   • target/seleniumiq-core-1.0.0.jar (main JAR)"
    if [ -f "target/seleniumiq-core-1.0.0-jar-with-dependencies.jar" ]; then
        echo "   • target/seleniumiq-core-1.0.0-jar-with-dependencies.jar (standalone JAR)"
    fi
    echo ""
    echo "🚀 Usage:"
    echo "   1. Copy the JAR to your project's lib/ directory"
    echo "   2. Add system dependency to your pom.xml:"
    echo "      <dependency>"
    echo "          <groupId>com.seleniumiq</groupId>"
    echo "          <artifactId>seleniumiq-core</artifactId>"
    echo "          <version>1.0.0</version>"
    echo "          <scope>system</scope>"
    echo "          <systemPath>\${project.basedir}/lib/seleniumiq-core-1.0.0.jar</systemPath>"
    echo "      </dependency>"
    echo ""
    echo "   3. Use in your tests:"
    echo "      @ExtendWith(SeleniumIQWatcher.class)"
    echo ""
else
    echo "❌ Build failed - JAR not found!"
    exit 1
fi