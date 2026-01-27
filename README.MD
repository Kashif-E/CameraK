# CameraK

<p align="center">
  <a href="https://github.com/Kashif-E/CameraK/releases/tag/0.0.12">
    <img src="https://img.shields.io/github/v/release/kashif-e/camerak" alt="Version">
  </a>
  <a href="https://mailchi.mp/kotlinweekly/kotlin-weekly-425">
    <img src="https://img.shields.io/badge/Kotlin_Weekly-425-blue" alt="Kotlin Weekly #425">
  </a>
</p>

## A Modern Camera Solution for Compose Multiplatform

CameraK is a comprehensive camera library designed specifically for Compose Multiplatform applications. It currently supports Android, iOS, and JVM platforms, with plans for expansion.

### Key Features

- 📱 **Cross-Platform Support**: Works on Android, iOS, and JVM
- 📸 **Camera Preview & Image Capture**: High-quality preview and capture capabilities
- 🎯 **Aspect Ratio Control**: Configure preview and capture aspect ratios (4:3, 16:9, 9:16, 1:1)
- 🔍 **Zoom Support**: Pinch-to-zoom with programmatic zoom control on Android & iOS
- 📷 **Advanced Camera Selection**: Choose specific camera types (ultra-wide, telephoto) on iOS
- ⚡ **Fast File Capture**: Direct-to-file capture with `takePictureToFile()` for better performance
- 💾 **Image Processing**: Save images locally or process them as ByteArrays
- 🧩 **Plugin Architecture**: Extend functionality through modular plugins
- 🔍 **QR/Barcode Scanning**: Multi-format barcode detection with dedicated plugin
- 📝 **OCR Support**: Text recognition capabilities with OCR plugin

## Installation

Add the core library to your project:

```kotlin
implementation("io.github.kashif-mehmood-km:camerak:+")
```

### Optional Plugins

Image saving plugin:
```kotlin
implementation("io.github.kashif-mehmood-km:image_saver_plugin:0.0.7")
```

QR scanning plugin:
```kotlin
implementation("io.github.kashif-mehmood-km:qr_scanner_plugin:0.0.8")
```

OCR plugin:
```kotlin
implementation("io.github.kashif-mehmood-km:ocr_plugin:0.0.3")
```

## Platform Setup

### Android

Add these permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### iOS

Add these keys to your `Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>Camera permission is required for the app to work.</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>Photo Library permission is required for the app to work.</string>
```

## Quick Start

### 1. Handle Permissions

```kotlin
// Obtain platform permissions helper
val permissions = providePermissions()

// Initialize and check permissions
val cameraPermissionState = remember { mutableStateOf(permissions.hasCameraPermission()) }
val storagePermissionState = remember { mutableStateOf(permissions.hasStoragePermission()) }

// Request permissions if needed
if (!cameraPermissionState.value) {
    permissions.RequestCameraPermission(
        onGranted = { cameraPermissionState.value = true },
        onDenied = { println("Camera Permission Denied") }
    )
}

if (!storagePermissionState.value) {
    permissions.RequestStoragePermission(
        onGranted = { storagePermissionState.value = true },
        onDenied = { println("Storage Permission Denied") }
    )
}
```

### 2. Set Up the Camera Controller

```kotlin
val cameraController = remember { mutableStateOf<CameraController?>(null) }

// Configure plugins if needed
val imageSaverPlugin = rememberImageSaverPlugin(
    config = ImageSaverConfig(
        isAutoSave = false,
        prefix = "MyApp",
        directory = Directory.PICTURES,
        customFolderName = "MyAppPhotos"  // Android only
    )
)

val qrScannerPlugin = rememberQRScannerPlugin(coroutineScope = coroutineScope)

// Set up QR code detection
LaunchedEffect(Unit) {
    qrScannerPlugin.getQrCodeFlow().distinctUntilChanged()
        .collectLatest { qrCode ->
            println("QR Code Detected: $qrCode")
            qrScannerPlugin.pauseScanning()
        }
}
```

### 3. Implement the Camera Preview

```kotlin
CameraPreview(
    modifier = Modifier.fillMaxSize(),
    cameraConfiguration = {
        setCameraLens(CameraLens.BACK)
        setFlashMode(FlashMode.OFF)
        setImageFormat(ImageFormat.JPEG)
        setDirectory(Directory.PICTURES)
        setAspectRatio(AspectRatio.RATIO_16_9)  // Configure preview/capture aspect ratio
        setResolution(1920 to 1080)  // Optional: set specific resolution
        setCameraDeviceType(CameraDeviceType.ULTRA_WIDE)  // iOS: select camera type
        addPlugin(imageSaverPlugin)
        addPlugin(qrScannerPlugin)
    },
    onCameraControllerReady = {
        cameraController.value = it
        qrScannerPlugin.startScanning()
    }
)

// Display your custom camera UI once controller is ready
cameraController.value?.let { controller ->
    CameraScreen(cameraController = controller, imageSaverPlugin)
}
```

### 4. Capture and Save Images

```kotlin
Button(
    onClick = {
        scope.launch {
            // Fast file capture (recommended for better performance)
            when (val result = cameraController.takePictureToFile()) {
                is ImageCaptureResult.SuccessWithFile -> {
                    println("Image saved at: ${result.filePath}")
                    // File is already saved, use filePath for further processing
                }
                is ImageCaptureResult.Success -> {
                    // Fallback for platforms that don't support direct file capture
                    val bitmap = result.byteArray.decodeToImageBitmap()
                }
                is ImageCaptureResult.Error -> {
                    println("Image Capture Error: ${result.exception.message}")
                }
            }
            
            // OR: Traditional ByteArray capture
            when (val result = cameraController.takePicture()) {
                is ImageCaptureResult.Success -> {
                    val bitmap = result.byteArray.decodeToImageBitmap()
                    
                    // Manually save if auto-save is disabled
                    if (!imageSaverPlugin.config.isAutoSave) {
                        imageSaverPlugin.saveImage(
                            byteArray = result.byteArray,
                            imageName = "Photo_${System.currentTimeMillis()}"
                        )
                    }
                }
                is ImageCaptureResult.Error -> {
                    println("Image Capture Error: ${result.exception.message}")
                }
            }
        }
    }
) {
    Text("Capture")
}
```

### 5. Zoom Control (Android & iOS)

```kotlin
// Get max zoom level
val maxZoom = cameraController.getMaxZoom()

// Set zoom level (1.0 = no zoom, maxZoom = maximum zoom)
cameraController.setZoom(2.5f)

// Get current zoom level
val currentZoom = cameraController.getZoom()
```

## Advanced Usage

### Available Aspect Ratios

- `AspectRatio.RATIO_4_3` - Standard 4:3 (default on most devices)
- `AspectRatio.RATIO_16_9` - Widescreen 16:9
- `AspectRatio.RATIO_9_16` - Portrait 9:16 (ideal for stories/vertical content)
- `AspectRatio.RATIO_1_1` - Square 1:1

### Camera Device Types (iOS)

- `CameraDeviceType.DEFAULT` - Default wide camera
- `CameraDeviceType.ULTRA_WIDE` - Ultra-wide angle camera
- `CameraDeviceType.TELEPHOTO` - Telephoto camera (if available)
- `CameraDeviceType.DUAL_CAMERA` - Dual camera system
- `CameraDeviceType.TRIPLE_CAMERA` - Triple camera system

### Complete Sample

Check the sample app in the repository for a complete implementation showcasing all features, including:

- Toggling between front and back cameras
- Managing flash and torch modes
- Zoom controls with slider
- Aspect ratio selection
- Camera device type selection (iOS)
- Processing captured images
- Handling QR code scanning results
- OCR implementation

## Creating Custom Plugins

CameraK features a powerful plugin API that allows developers to extend its functionality. Explore the `qrScannerPlugin` or `imageSaverPlugin` source code for examples of how to build your own plugins.

## Status

This library is currently in experimental stage. APIs may change in future releases.

## ⚠️ Deprecation Notices

### Deprecated APIs (→ v1.0)

The following APIs are deprecated and will be removed in v1.0:

#### `takePicture()` → `takePictureToFile()`
**Reason:** Performance - `takePicture()` has 2-3 second overhead due to decode/encode cycles.

```kotlin
// ❌ Deprecated (slower)
val result = cameraController.takePicture()

// ✅ Recommended (2-3x faster)
val result = cameraController.takePictureToFile()
```

#### iOS: `rememberIOSPermissions()` → `providePermissions()`
**Reason:** Consistency - Use the cross-platform API for all platforms.

```kotlin
// ❌ Deprecated (iOS-specific)
val permissions = rememberIOSPermissions()

// ✅ Recommended (cross-platform)
val permissions = providePermissions()
```

### Removed Components

- **`BurstCaptureManager` (Android)**: Moved to iOS-only implementation. Android now uses CameraX's built-in burst capture optimization.

## 📚 Documentation

For comprehensive information about CameraK:

- **[Codebase Overview](CODEBASE_OVERVIEW.md)** - Understand the project structure and architecture
- **[Architecture Guide](ARCHITECTURE.md)** - Detailed architectural documentation with diagrams
- **[API Reference](API_REFERENCE.md)** - Complete API documentation with examples
- **[Development Setup](DEVELOPMENT_SETUP.md)** - Setup guide for contributors and developers
- **[Troubleshooting](TROUBLESHOOTING.md)** - Common issues and solutions

## Contributing

Contributions are welcome! Before submitting a pull request:

1. Read the [Development Setup Guide](DEVELOPMENT_SETUP.md)
2. Open an issue to discuss your proposed changes
3. Fork the repository and create your branch
4. Implement your feature or fix following the [Architecture Guide](ARCHITECTURE.md)
5. Ensure tests pass and check the [Troubleshooting Guide](TROUBLESHOOTING.md) if needed
6. Submit your pull request

## Support

If you find this library useful, consider supporting the developer:

<a href="https://www.buymeacoffee.com/kashifmehmood"><img src="https://img.buymeacoffee.com/button-api/?text=Buy me a coffee&emoji=&slug=kashifmehmood&button_colour=FFDD00&font_colour=000000&font_family=Cookie&outline_colour=000000&coffee_colour=ffffff" /></a>

## License

```
MIT License
```
