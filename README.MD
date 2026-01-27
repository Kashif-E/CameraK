# CameraK

A modern camera library for Compose Multiplatform supporting Android, iOS, and Desktop with a unified API.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kashif-mehmood-km/camerak?label=Maven%20Central&color=blue)](https://search.maven.org/search?q=g:io.github.kashif-mehmood-km)
[![GitHub Release](https://img.shields.io/github/v/release/kashif-e/camerak)](https://github.com/Kashif-E/CameraK/releases)
[![Kotlin Weekly](https://img.shields.io/badge/Kotlin_Weekly-425-7F52FF)](https://mailchi.mp/kotlinweekly/kotlin-weekly-425)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

## Features

- 📱 **Cross-Platform**: Android, iOS, and JVM Desktop
- 📸 **Compose-First**: Native Compose Multiplatform API  
- 🎯 **Flexible Configuration**: Aspect ratios, zoom, flash control
- 🔌 **Plugin System**: Modular QR/barcode scanning, OCR, image saving
- ⚡ **Optimized Capture**: Direct file saving with `takePictureToFile()`
- 📷 **Advanced Control**: Camera selection (ultra-wide, telephoto on iOS)

## Installation

Add dependencies to your `build.gradle.kts`:

```kotlin
dependencies {
    // Core library
    implementation("io.github.kashif-mehmood-km:camerak:0.1.0")
    
    // Optional plugins
    implementation("io.github.kashif-mehmood-km:image_saver_plugin:0.1.0")
    implementation("io.github.kashif-mehmood-km:qr_scanner_plugin:0.1.0")
    implementation("io.github.kashif-mehmood-km:ocr_plugin:0.1.0")
}
```

### Using Version Catalog

Add to your `libs.versions.toml`:

```toml
[versions]
camerak = "0.1.0"

[libraries]
camerak = { module = "io.github.kashif-mehmood-km:camerak", version.ref = "camerak" }
camerak-image-saver = { module = "io.github.kashif-mehmood-km:image_saver_plugin", version.ref = "camerak" }
camerak-qr-scanner = { module = "io.github.kashif-mehmood-km:qr_scanner_plugin", version.ref = "camerak" }
camerak-ocr = { module = "io.github.kashif-mehmood-km:ocr_plugin", version.ref = "camerak" }
```

Then in your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.camerak)
    implementation(libs.camerak.image.saver)
    implementation(libs.camerak.qr.scanner)
    implementation(libs.camerak.ocr)
}
```

### Platform Setup

**Android** - Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

**iOS** - Add to `Info.plist`:
```xml
<key>NSCameraUsageDescription</key>
<string>Camera access required for taking photos</string>
<key>NSPhotoLibraryAddUsageDescription</key>
<string>Photo library access required for saving images</string>
```

## Quick Start

```kotlin
@Composable
fun CameraScreen() {
    val permissions = providePermissions()
    val cameraController = remember { mutableStateOf<CameraController?>(null) }
    val scope = rememberCoroutineScope()
    
    // Request permissions
    LaunchedEffect(Unit) {
        if (!permissions.hasCameraPermission()) {
            permissions.RequestCameraPermission(
                onGranted = { /* Granted */ },
                onDenied = { /* Denied */ }
            )
        }
    }
    
    // Camera preview
    CameraPreview(
        modifier = Modifier.fillMaxSize(),
        cameraConfiguration = {
            setCameraLens(CameraLens.BACK)
            setFlashMode(FlashMode.OFF)
            setAspectRatio(AspectRatio.RATIO_16_9)
        },
        onCameraControllerReady = { controller ->
            cameraController.value = controller
        }
    )
    
    // Capture button
    cameraController.value?.let { controller ->
        Button(
            onClick = {
                scope.launch {
                    when (val result = controller.takePictureToFile()) {
                        is ImageCaptureResult.SuccessWithFile -> {
                            println("Saved: ${result.filePath}")
                        }
                        is ImageCaptureResult.Error -> {
                            println("Error: ${result.exception.message}")
                        }
                    }
                }
            }
        ) {
            Text("Capture Photo")
        }
    }
}
```

## Platform Support

| Platform | Min Version | Backend |
|----------|-------------|---------|
| Android | API 21+ | CameraX |
| iOS | iOS 13.0+ | AVFoundation |
| Desktop | JDK 11+ | JavaCV |

## Configuration

### Camera Configuration DSL

```kotlin
CameraPreview(
    cameraConfiguration = {
        // Camera selection
        setCameraLens(CameraLens.BACK)  // FRONT or BACK
        
        // Visual settings
        setAspectRatio(AspectRatio.RATIO_16_9)  // 4:3, 16:9, 9:16, 1:1
        setResolution(1920 to 1080)  // Optional specific resolution
        
        // Flash control
        setFlashMode(FlashMode.AUTO)  // ON, OFF, AUTO
        
        // Image output
        setImageFormat(ImageFormat.JPEG)  // JPEG or PNG
        setDirectory(Directory.PICTURES)  // DCIM, DOCUMENTS, etc.
        
        // iOS only: Camera device type
        setCameraDeviceType(CameraDeviceType.ULTRA_WIDE)
        // Options: DEFAULT, ULTRA_WIDE, TELEPHOTO, DUAL_CAMERA, TRIPLE_CAMERA
        
        // Add plugins
        addPlugin(imageSaverPlugin)
        addPlugin(qrScannerPlugin)
    }
)
```

### Zoom Control

```kotlin
// Get maximum zoom capability
val maxZoom = controller.getMaxZoom()

// Set zoom level (1.0 = no zoom)
controller.setZoom(2.5f)

// Get current zoom
val currentZoom = controller.getZoom()
```

### Flash and Torch

```kotlin
// Toggle flash modes (cycles: OFF → ON → AUTO)
controller.toggleFlashMode()

// Set specific flash mode
controller.setFlashMode(FlashMode.ON)

// Get current flash mode
val mode = controller.getFlashMode()

// Torch mode (continuous light)
controller.toggleTorchMode()
```

### Camera Lens

```kotlin
// Switch between front/back cameras
controller.toggleCameraLens()

// Set specific lens
controller.setCameraLens(CameraLens.FRONT)
```

### Aspect Ratios

```kotlin
AspectRatio.RATIO_4_3   // Standard 4:3
AspectRatio.RATIO_16_9  // Widescreen 16:9
AspectRatio.RATIO_9_16  // Vertical 9:16 (stories)
AspectRatio.RATIO_1_1   // Square 1:1
```

## Plugins

### Image Saver Plugin

```kotlin
val imageSaverPlugin = rememberImageSaverPlugin(
    config = ImageSaverConfig(
        isAutoSave = false,
        prefix = "MyApp",
        directory = Directory.PICTURES,
        customFolderName = "MyAppPhotos"  // Android only
    )
)

// Add to camera configuration
addPlugin(imageSaverPlugin)

// Manual save
scope.launch {
    when (val result = controller.takePicture()) {
        is ImageCaptureResult.Success -> {
            imageSaverPlugin.saveImage(
                byteArray = result.byteArray,
                imageName = "Photo_${System.currentTimeMillis()}"
            )
        }
        is ImageCaptureResult.Error -> {
            // Handle error
        }
    }
}
```

### QR Scanner Plugin

```kotlin
val qrScannerPlugin = rememberQRScannerPlugin(
    coroutineScope = rememberCoroutineScope()
)

// Start scanning
LaunchedEffect(Unit) {
    qrScannerPlugin.startScanning()
    
    qrScannerPlugin.getQrCodeFlow()
        .distinctUntilChanged()
        .collectLatest { qrCode ->
            println("QR Code: $qrCode")
            qrScannerPlugin.pauseScanning()
            
            // Show result, then resume
            delay(2000)
            qrScannerPlugin.resumeScanning()
        }
}

// Add to camera configuration
addPlugin(qrScannerPlugin)
```

### OCR Plugin

```kotlin
val ocrPlugin = rememberOcrPlugin()

// Add to camera configuration
addPlugin(ocrPlugin)

// Use OCR
scope.launch {
    when (val result = controller.takePicture()) {
        is ImageCaptureResult.Success -> {
            val text = ocrPlugin.recognizeText(result.byteArray)
            println("Detected text: $text")
        }
        is ImageCaptureResult.Error -> {
            // Handle error
        }
    }
}
```


### Debug Tips

```kotlin
@Composable
fun CameraScreen() {
    val controller = remember { mutableStateOf<CameraController?>(null) }
    
    DisposableEffect(Unit) {
        onDispose {
            // Always release camera resources
            controller.value?.stopSession()
            controller.value = null
        }
    }
}
```

## API Reference

### CameraController

```kotlin
// Capture to file (recommended - faster)
suspend fun takePictureToFile(): ImageCaptureResult

// Capture to ByteArray (slower - deprecated)
@Deprecated("Use takePictureToFile() instead")
suspend fun takePicture(): ImageCaptureResult

// Zoom control
fun setZoom(zoom: Float)
fun getZoom(): Float
fun getMaxZoom(): Float

// Flash control
fun setFlashMode(mode: FlashMode)
fun getFlashMode(): FlashMode?
fun toggleFlashMode()

// Torch control
fun toggleTorchMode()

// Camera selection
fun setCameraLens(lens: CameraLens)
fun toggleCameraLens()

// Session management
fun startSession()
fun stopSession()
```

### CameraPreview

```kotlin
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraConfiguration: CameraConfiguration.() -> Unit,
    onCameraControllerReady: (CameraController) -> Unit
)
```

## Deprecation Notices

### v1.0 Migration

#### `takePicture()` → `takePictureToFile()`

```kotlin
// ❌ Deprecated (slower, will be removed in v2.0)
when (val result = controller.takePicture()) {
    is ImageCaptureResult.Success -> {
        val byteArray = result.byteArray
        // Manual file save required
    }
}

// ✅ Recommended (2-3x faster)
when (val result = controller.takePictureToFile()) {
    is ImageCaptureResult.SuccessWithFile -> {
        val filePath = result.filePath
        // File already saved
    }
}
```

#### iOS: `rememberIOSPermissions()` → `providePermissions()`

```kotlin
// ❌ Deprecated (iOS-only)
val permissions = rememberIOSPermissions()

// ✅ Recommended (cross-platform)
val permissions = providePermissions()
```


## Contributing

Contributions welcome! Please:

1. Open an issue to discuss changes
2. Fork the repository
3. Create a feature branch
4. Submit a pull request

## Support

If you find this library useful:

<a href="https://www.buymeacoffee.com/kashifmehmood"><img src="https://img.buymeacoffee.com/button-api/?text=Buy me a coffee&emoji=&slug=kashifmehmood&button_colour=FFDD00&font_colour=000000&font_family=Cookie&outline_colour=000000&coffee_colour=ffffff" /></a>

## License

```
Apache License 2.0

Copyright 2025 Kashif Mehmood

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
