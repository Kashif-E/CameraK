# CameraK Development Setup Guide

## 🚀 Getting Started

This guide will help you set up a development environment for CameraK, understand the build process, and start contributing to the project.

## 📋 Prerequisites

### Required Software

1. **JDK 17 or higher**
   ```bash
   # Check Java version
   java -version
   javac -version
   ```

2. **Android Studio (for Android development)**
   - Latest stable version recommended
   - Android SDK with API level 21+
   - Android build tools 34.0.0+

3. **Xcode (for iOS development - macOS only)**
   - Xcode 14.0+
   - iOS SDK 13.0+
   - Command Line Tools

4. **Git**
   ```bash
   git --version
   ```

### Platform-Specific Requirements

#### Android Development
- Android SDK Platform 21+ (Android 5.0)
- Android Build Tools 34.0.0+
- Google Play services (for ML Kit)

#### iOS Development (macOS only)
- macOS 12.0+
- Xcode 14.0+
- iOS Simulator or physical device
- Apple Developer account (for device testing)

#### Desktop Development
- JDK 17+ (same as above)
- Native libraries for camera access

## 🔧 Environment Setup

### 1. Clone the Repository

```bash
git clone https://github.com/Kashif-E/CameraK.git
cd CameraK
```

### 2. Gradle Configuration

The project uses Gradle with Kotlin DSL. Ensure you have the wrapper:

```bash
# Make gradlew executable (Unix/macOS)
chmod +x gradlew

# Check Gradle version
./gradlew --version
```

### 3. IDE Setup

#### IntelliJ IDEA / Android Studio
1. Open the project in your IDE
2. Wait for Gradle sync to complete
3. Install Kotlin Multiplatform Mobile plugin
4. Install Compose Multiplatform plugin

#### VS Code (Alternative)
1. Install Kotlin extension
2. Install Gradle extension
3. Open project folder

## 🏗️ Build Instructions

### Understanding the Build System

The project uses Kotlin Multiplatform with these main configurations:

```kotlin
kotlin {
    androidTarget()           // Android builds
    jvm("desktop")           // Desktop/JVM builds
    iosX64()                // iOS Simulator (Intel)
    iosArm64()              // iOS Device (ARM64)
    iosSimulatorArm64()     // iOS Simulator (Apple Silicon)
}
```

### Common Build Commands

#### Clean Build
```bash
./gradlew clean
```

#### Build All Targets
```bash
./gradlew build
```

#### Build Specific Targets
```bash
# Android only
./gradlew :cameraK:assembleDebug

# Desktop only
./gradlew :cameraK:jvmJar

# iOS framework
./gradlew :cameraK:linkDebugFrameworkIosArm64
```

### Running Tests

#### All Tests
```bash
./gradlew test
```

#### Platform-specific Tests
```bash
# Android tests
./gradlew :cameraK:testDebugUnitTest

# Desktop tests
./gradlew :cameraK:desktopTest

# Common tests
./gradlew :cameraK:commonTest
```

## 🎯 Running Sample Applications

### Android Sample
```bash
# Build and install on connected device/emulator
./gradlew :Sample:installDebug

# Or run directly
./gradlew :Sample:run
```

### Desktop Sample
```bash
./gradlew :Sample:run
```

### iOS Sample
1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select target device/simulator
3. Click Run button or press Cmd+R

## 🔧 Development Workflow

### 1. Project Structure Understanding

```
CameraK/
├── build.gradle.kts          # Root build configuration
├── settings.gradle.kts       # Project structure definition
├── gradle/
│   └── libs.versions.toml    # Dependency version catalog
├── cameraK/                  # Core library
│   ├── src/
│   │   ├── commonMain/       # Shared code
│   │   ├── androidMain/      # Android-specific
│   │   ├── appleMain/        # iOS-specific
│   │   └── desktopMain/      # Desktop-specific
│   └── build.gradle.kts      # Module build config
├── Sample/                   # Demo application
├── *Plugin/                  # Plugin modules
└── iosApp/                   # iOS sample wrapper
```

### 2. Making Changes

#### Core Library Changes
1. Navigate to `cameraK/src/`
2. Choose appropriate source set:
   - `commonMain` for shared functionality
   - `androidMain` for Android-specific code
   - `appleMain` for iOS-specific code
   - `desktopMain` for desktop-specific code

#### Plugin Development
1. Create new module or modify existing plugin
2. Implement `CameraPlugin` interface
3. Add platform-specific implementations

#### Sample App Updates
1. Modify `Sample/src/commonMain/kotlin/org/company/app/App.kt`
2. Test changes across platforms

### 3. Testing Changes

#### Unit Testing
```bash
# Test your changes
./gradlew test

# Test specific module
./gradlew :cameraK:test
./gradlew :qrScannerPlugin:test
```

#### Integration Testing
```bash
# Run sample app
./gradlew :Sample:run

# Install on Android device
./gradlew :Sample:installDebug
```

#### Manual Testing Checklist
- [ ] Camera preview loads correctly
- [ ] Image capture works
- [ ] Flash/torch controls function
- [ ] Camera lens toggle works
- [ ] Plugins initialize properly
- [ ] Permissions are requested correctly

## 🐛 Debugging

### Common Issues and Solutions

#### Build Failures

**Issue**: `Plugin [id: 'com.android.library'] was not found`
```bash
# Solution: Ensure Android SDK is properly installed
export ANDROID_HOME=/path/to/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

**Issue**: iOS build fails
```bash
# Solution: Clean and rebuild iOS framework
./gradlew :cameraK:cleanIosFramework
./gradlew :cameraK:linkDebugFrameworkIosArm64
```

#### Runtime Issues

**Issue**: Camera not working on Android
- Check permissions in AndroidManifest.xml
- Verify device has camera capability
- Check CameraX dependencies

**Issue**: Desktop camera access denied
- Check system permissions
- Verify JavaCV native libraries
- Test with different camera indices

### Debugging Tools

#### Logs
```kotlin
// Use Kermit for logging
import co.touchlab.kermit.Logger

Logger.d("Camera") { "Camera initialized successfully" }
Logger.e("Camera") { "Camera error: ${exception.message}" }
```

#### Platform-specific Debugging

**Android**:
- Use Android Studio debugger
- Check Logcat output
- Use `adb` commands

**iOS**:
- Use Xcode debugger
- Check device logs
- Use Instruments for performance

**Desktop**:
- Use IDE debugger
- Check console output
- Use JVM profiling tools

## 📦 Publishing (Maintainers Only)

### Local Publishing
```bash
# Publish to local Maven repository
./gradlew publishToMavenLocal
```

### Maven Central Publishing
```bash
# Build and publish all artifacts
./gradlew publishAllPublicationsToMavenCentralRepository
```

### Version Management
1. Update version in `build.gradle.kts` files
2. Update `libs.versions.toml`
3. Create git tag for release
4. Update README.md with new version

## 🔄 Continuous Integration

### GitHub Actions

The project uses GitHub Actions for CI/CD:

```yaml
# Example workflow structure
- Build on multiple platforms
- Run tests
- Check code style
- Generate documentation
- Publish artifacts (on release)
```

### Pre-commit Hooks

Set up pre-commit hooks for code quality:

```bash
# Install ktlint
brew install ktlint  # macOS
# or download from GitHub releases

# Check code style
ktlint --format
```

## 📚 Additional Resources

### Documentation
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform Documentation](https://github.com/JetBrains/compose-multiplatform)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [AVFoundation Documentation](https://developer.apple.com/documentation/avfoundation)

### Community
- GitHub Issues for bug reports
- GitHub Discussions for questions
- Stack Overflow for technical help

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Keep functions focused and small

## 🎯 Contributing Guidelines

### Pull Request Process
1. Fork the repository
2. Create feature branch
3. Make changes with tests
4. Ensure CI passes
5. Submit pull request

### Code Review Checklist
- [ ] Code follows project conventions
- [ ] Tests are included and passing
- [ ] Documentation is updated
- [ ] No breaking changes (unless major version)
- [ ] Platform compatibility maintained

### Issue Reporting
When reporting issues:
1. Use issue templates
2. Provide minimal reproduction case
3. Include platform/version information
4. Add relevant logs/screenshots

This development setup guide should help you get started with CameraK development and contribute effectively to the project.