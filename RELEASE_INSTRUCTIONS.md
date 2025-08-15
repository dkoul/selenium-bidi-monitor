# 🚀 SeleniumIQ Release Instructions

This document explains how to release SeleniumIQ using GitHub Releases with automated JAR builds.

## Prerequisites

1. **GitHub Repository**: Push your SeleniumIQ code to a GitHub repository
2. **GitHub Actions**: Ensure Actions are enabled for your repository
3. **Version Tags**: Use semantic versioning (e.g., v1.0.0, v1.1.0)

## Release Process

### Step 1: Prepare for Release

1. **Update Version in pom.xml**:
   ```xml
   <version>1.0.0</version>
   ```

2. **Update README if needed**:
   - Update version numbers in examples
   - Add any new features to documentation

3. **Test Build Locally**:
   ```bash
   ./build.sh
   ```

4. **Commit Changes**:
   ```bash
   git add .
   git commit -m "Prepare for release v1.0.0"
   git push origin main
   ```

### Step 2: Create Release Tag

1. **Create and Push Tag**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **Alternative - Create via GitHub**:
   - Go to your repository on GitHub
   - Click "Releases" → "Create a new release"
   - Enter tag: `v1.0.0`
   - Target: `main` branch

### Step 3: Automated Build & Release

Once you push the tag, GitHub Actions will automatically:

1. ✅ **Build the project** with Maven
2. ✅ **Run tests** to ensure quality
3. ✅ **Create two JARs**:
   - `seleniumiq-core-1.0.0.jar` (regular)
   - `seleniumiq-core-1.0.0-jar-with-dependencies.jar` (standalone)
4. ✅ **Create GitHub Release** with release notes
5. ✅ **Upload JARs** as release assets

### Step 4: Verify Release

1. **Check GitHub Release Page**:
   - Visit: `https://github.com/yourusername/seleniumiq/releases`
   - Verify release was created
   - Verify both JAR files are attached

2. **Test Download**:
   - Download the standalone JAR
   - Test in a sample project

## Release Checklist

Before releasing, ensure:

- [ ] Version updated in `pom.xml`
- [ ] All tests pass locally (`./build.sh`)
- [ ] README updated with new version
- [ ] CHANGELOG updated (if you maintain one)
- [ ] No uncommitted changes
- [ ] Main branch is stable

## Version Strategy

We use [Semantic Versioning](https://semver.org/):

- **MAJOR** (v2.0.0): Breaking changes
- **MINOR** (v1.1.0): New features, backwards compatible
- **PATCH** (v1.0.1): Bug fixes, backwards compatible

## Distribution Files

Each release generates:

1. **`seleniumiq-core-X.Y.Z.jar`**
   - Regular JAR file
   - Requires users to have Selenium, Jackson, etc. dependencies
   - Smaller file size

2. **`seleniumiq-core-X.Y.Z-jar-with-dependencies.jar`** (Recommended)
   - Standalone JAR with all dependencies included
   - Users only need this one file
   - Larger file size but easier for users

## User Instructions

Include these instructions for your users:

### For Users - How to Use Released JARs

1. **Download** from GitHub Releases
2. **Place in `lib/` directory** of their project
3. **Add to `pom.xml`**:
   ```xml
   <dependency>
       <groupId>com.seleniumiq</groupId>
       <artifactId>seleniumiq-core</artifactId>
       <version>1.0.0</version>
       <scope>system</scope>
       <systemPath>${project.basedir}/lib/seleniumiq-core-1.0.0-jar-with-dependencies.jar</systemPath>
   </dependency>
   ```

## Troubleshooting

### Build Fails
- Check GitHub Actions logs
- Ensure Java 21 compatibility
- Verify all dependencies are available

### JAR Not Generated
- Check Maven assembly plugin configuration
- Ensure `mvn assembly:single` works locally

### Release Not Created
- Verify GitHub token permissions
- Check if tag matches the pattern `v*`
- Ensure Actions are enabled

## Future: Maven Central

Eventually, we plan to publish to Maven Central for easier dependency management. This will require:

1. Sonatype OSSRH account
2. GPG signing setup
3. Domain verification
4. Maven Central sync process

Until then, GitHub Releases provide an excellent distribution mechanism!