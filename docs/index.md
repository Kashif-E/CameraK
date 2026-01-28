# CameraK

**Kotlin Multiplatform Camera SDK** – Capture stunning photos and videos across all platforms.

## Features

- 📱 **Multiplatform Support** – Android, iOS, Desktop, and JavaScript
- 🎥 **Flexible Camera Control** – Full control over camera settings and modes
- 📸 **High-Quality Capture** – Support for various image formats and quality levels
- 🔐 **Permission Handling** – Built-in permission management for all platforms
- 🎨 **Jetpack Compose Integration** – Seamless UI integration with Compose Multiplatform
- 🚀 **Performance Optimized** – Efficient resource usage across platforms
- 📚 **Well Documented** – Comprehensive documentation and examples

## Quick Start

```kotlin
// Initialize camera controller
val controller = CameraController.Builder()
    .setVideoResolution(1920, 1080)
    .build()

// Start camera preview
controller.startPreview()

// Capture photo
val photo = controller.capturePhoto()
```

## Supported Platforms

| Platform | Status | Features |
|----------|--------|----------|
| 🔶 Android | ✅ Full Support | CameraX API |
| 🍎 iOS | ✅ Full Support | AVFoundation |
| 🖥️ Desktop | ✅ Full Support | JavaCV |
| 🌐 JavaScript | ✅ Full Support | Web Camera API |

## Installation

Install CameraK using Gradle:

```gradle
dependencies {
    implementation("dev.kashif:cameraK:0.2.0")
}
```

## Platform-Specific Setup

### Android
```kotlin
// Add to AndroidManifest.xml
<uses-permission android:name="android.permission.CAMERA" />
```

### iOS
```swift
// Add to Info.plist
<key>NSCameraUsageDescription</key>
<string>We need camera access to capture photos</string>
```

## Documentation

- [Getting Started Guide](getting-started/installation.md)
- [API Reference](api/camera-controller.md)
- [Examples](examples/android.md)

## Contributing

We welcome contributions! See [CONTRIBUTING.md](contributing.md) for guidelines.

## License

CameraK is licensed under the MIT License. See [LICENSE](license.md) for details.

## Support

- 📖 [Documentation](https://github.com/kashif-e/CameraK/tree/main/docs)
- 🐛 [Report Issues](https://github.com/kashif-e/CameraK/issues)
- 💬 [Discussions](https://github.com/kashif-e/CameraK/discussions)

---

**Built with ❤️ for mobile and desktop developers**
