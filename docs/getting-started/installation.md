# Installation

## Prerequisites

- Kotlin 1.9+
- Gradle 8.0+
- Android 8.0+ (for Android platform)
- iOS 14+ (for iOS platform)
- Java 11+ (for Desktop platform)

## Adding CameraK to Your Project

### Step 1: Add Repository

Add the CameraK repository to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}
```

### Step 2: Add Dependency

Add CameraK to your module's `build.gradle.kts`:

=== "Multiplatform"
    ```kotlin
    kotlin {
        sourceSets {
            commonMain.dependencies {
                implementation("dev.kashif:cameraK:0.2.0")
            }
        }
    }
    ```

=== "Android Only"
    ```kotlin
    dependencies {
        implementation("dev.kashif:cameraK:0.2.0")
    }
    ```

=== "iOS Only"
    ```kotlin
    kotlin {
        iosMain.dependencies {
            implementation("dev.kashif:cameraK:0.2.0")
        }
    }
    ```

## Platform Configuration

### Android

Add the following to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### iOS

Add to your `Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>We need camera access to capture photos and videos</string>
<key>NSMicrophoneUsageDescription</key>
<string>We need microphone access for video recording</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>We need access to your photo library</string>
```

### Desktop

No additional configuration required. CameraK uses JavaCV for desktop support.

## Verification

To verify the installation, create a simple test:

```kotlin
fun main() {
    try {
        val controller = CameraController.Builder()
            .build()
        println("✅ CameraK initialized successfully!")
    } catch (e: Exception) {
        println("❌ Failed to initialize: ${e.message}")
    }
}
```

## Troubleshooting

### Dependencies Not Found
- Clear Gradle cache: `rm -rf ~/.gradle/caches`
- Run: `./gradlew clean build`

### Build Fails on iOS
- Ensure CocoaPods is updated: `pod repo update`
- Clean Xcode build: `xcodebuild clean -scheme CameraK`

### Runtime Issues
- Check [Troubleshooting Guide](../troubleshooting.md)
- Review platform-specific setup
- Enable debug logging in configuration

## Next Steps

- [Quick Start Guide](quick-start.md)
- [Configuration](configuration.md)
- [Examples](../examples/android.md)
