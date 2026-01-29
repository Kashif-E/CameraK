# CameraK

<p align="center">
  <img src="docs/assets/cameraklogo.png" alt="CameraK Logo" width="200" style="border-radius: 50%;"/>
</p>

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
    implementation("io.github.kashif-mehmood-km:camerak:0.2.0")
    
    // Optional plugins
    implementation("io.github.kashif-mehmood-km:image_saver_plugin:0.2.0")
    implementation("io.github.kashif-mehmood-km:qr_scanner_plugin:0.2.0")
    implementation("io.github.kashif-mehmood-km:ocr_plugin:0.2.0")
}
```

### Using Version Catalog

Add to your `libs.versions.toml`:

```toml
[versions]
camerak = "0.2.0"

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

### New Compose-First API (v0.2.0+)

The library now uses reactive state management with StateFlow for a seamless Compose experience:

```kotlin
@Composable
fun CameraScreen() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    
    // Create camera state holder - all configuration and plugins handled here
    val stateHolder = rememberCameraKState(
        permissions = permissions,
        cameraConfiguration = {
            setCameraLens(CameraLens.BACK)
            setFlashMode(FlashMode.OFF)
            setAspectRatio(AspectRatio.RATIO_16_9)
        },
        plugins = listOf(
            rememberImageSaverPlugin(config = ImageSaverConfig(isAutoSave = true)),
            rememberQRScannerPlugin(),
            rememberOcrPlugin()
        )
    )
    
    // Observe camera state reactively
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    val qrCodes by stateHolder.qrCodeFlow.collectAsStateWithLifecycle(initial = emptyList())
    val recognizedText by stateHolder.recognizedTextFlow.collectAsStateWithLifecycle(initial = "")
    
    // Render based on state
    when (cameraState) {
        is CameraKState.Initializing -> CircularProgressIndicator()
        is CameraKState.Ready -> {
            val controller = (cameraState as CameraKState.Ready).controller
            
            CameraPreviewComposable(
                modifier = Modifier.fillMaxSize(),
                controller = controller
            )
            
            // UI elements overlay the camera
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
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text("Capture Photo")
            }
            
            // Display QR codes
            if (qrCodes.isNotEmpty()) {
                Text("QR: ${qrCodes.last()}", modifier = Modifier.align(Alignment.TopStart))
            }
            
            // Display OCR results
            if (recognizedText.isNotEmpty()) {
                Text("Text: $recognizedText", modifier = Modifier.align(Alignment.TopEnd))
            }
        }
        is CameraKState.Error -> {
            Text("Camera Error: ${(cameraState as CameraKState.Error).exception.message}")
        }
    }
}
```

### Core State Management API

#### `rememberCameraKState()`

Creates a reactive camera state holder that manages all camera operations, plugin lifecycle, and state:

```kotlin
@Composable
fun rememberCameraKState(
    permissions: PermissionController,
    cameraConfiguration: CameraConfiguration.() -> Unit = {},
    plugins: List<CameraKPlugin> = emptyList(),
    onStateChange: (CameraKState) -> Unit = {}
): CameraKStateHolder
```

**Returns**: `CameraKStateHolder` with:
- `cameraState: StateFlow<CameraKState>` - Observable camera lifecycle state
- `qrCodeFlow: StateFlow<List<String>>` - Scanned QR codes (auto-populated by plugin)
- `recognizedTextFlow: StateFlow<String>` - OCR results (auto-populated by plugin)
- `eventsFlow: StateFlow<CameraEvent>` - Camera events (focus, zoom, etc.)
- `pluginScope: CoroutineScope` - For plugin lifecycle management

#### Camera State Variants

```kotlin
sealed class CameraKState {
    data object Initializing : CameraKState()
    data class Ready(val controller: CameraController) : CameraKState()
    data class Error(val exception: Exception) : CameraKState()
}
```

**State Lifecycle:**
1. `Initializing` → Camera starting, permissions requested, hardware initializing
2. `Ready` → Camera operational, all plugins auto-activated, ready for capture
3. `Error` → Initialization failed, camera unavailable, permissions denied

## Platform Support

| Platform | Min Version | Backend |
|----------|-------------|---------|
| Android | API 21+ | CameraX |
| iOS | iOS 13.0+ | AVFoundation |
| Desktop | JDK 11+ | JavaCV |

## Configuration

### Camera Configuration DSL

Configure camera behavior in the `rememberCameraKState()` call:

```kotlin
val stateHolder = rememberCameraKState(
    permissions = permissions,
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
        
        // iOS only: Advanced camera device types
        setCameraDeviceType(CameraDeviceType.ULTRA_WIDE)
        // Options: DEFAULT, ULTRA_WIDE, TELEPHOTO, DUAL_CAMERA, TRIPLE_CAMERA
    },
    plugins = listOf(
        rememberImageSaverPlugin(),
        rememberQRScannerPlugin(),
        rememberOcrPlugin()
    )
)
```

### Complete Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `setCameraLens()` | `CameraLens` | `BACK` | Front or back camera |
| `setAspectRatio()` | `AspectRatio` | `RATIO_16_9` | 4:3, 16:9, 9:16, or 1:1 |
| `setResolution()` | `Pair<Int, Int>` | Device default | Specific width × height |
| `setFlashMode()` | `FlashMode` | `AUTO` | ON, OFF, or AUTO |
| `setImageFormat()` | `ImageFormat` | `JPEG` | JPEG or PNG |
| `setDirectory()` | `Directory` | `PICTURES` | DCIM, DOCUMENTS, DOWNLOADS, CACHE |
| `setCameraDeviceType()` | `CameraDeviceType` | `DEFAULT` | iOS: DEFAULT, ULTRA_WIDE, TELEPHOTO, etc. |

### Camera Runtime Control

Access runtime camera control via the `CameraController`:

```kotlin
// Get controller from Ready state
val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
when (cameraState) {
    is CameraKState.Ready -> {
        val controller = (cameraState as CameraKState.Ready).controller
        
        // Zoom
        controller.setZoom(2.5f)
        val maxZoom = controller.getMaxZoom()
        val currentZoom = controller.getZoom()
        
        // Flash
        controller.setFlashMode(FlashMode.ON)
        controller.toggleFlashMode()  // Cycles: OFF → ON → AUTO
        val mode = controller.getFlashMode()
        
        // Torch (continuous light)
        controller.toggleTorchMode()
        
        // Camera lens
        controller.setCameraLens(CameraLens.FRONT)
        controller.toggleCameraLens()  // Switches between FRONT/BACK
    }
}
```

### Aspect Ratios

Standard video aspect ratios for different use cases:

```kotlin
AspectRatio.RATIO_4_3   // Standard (old phones, broadcasts)
AspectRatio.RATIO_16_9  // Widescreen (most common)
AspectRatio.RATIO_9_16  // Vertical stories (Instagram, TikTok)
AspectRatio.RATIO_1_1   // Square (Instagram feed)
```

### Image Formats

```kotlin
ImageFormat.JPEG  // Lossy compression, smaller files, web-ready
ImageFormat.PNG   // Lossless compression, larger files, transparency support
```

## Plugins

### Plugin System Overview

CameraK uses an auto-activating plugin system. Plugins are attached via `rememberCameraKState()` and automatically activate when the camera reaches the `Ready` state. This eliminates manual lifecycle management and provides clean reactive patterns.

**How Plugins Work:**
1. Plugin created and attached via `plugins = listOf(...)` in `rememberCameraKState()`
2. State holder calls `plugin.onAttach(stateHolder)` when mounting
3. Plugin observes `stateHolder.cameraState` and auto-activates when `Ready`
4. Plugin processes camera frames or handles capture events
5. Plugin cancels all operations on `onDetach()` (automatic cleanup)

### Image Saver Plugin

Automatically saves captured images with customizable naming and storage location.

#### Setup

```kotlin
val imageSaverPlugin = rememberImageSaverPlugin(
    config = ImageSaverConfig(
        isAutoSave = false,           // Manual save vs. auto-save on capture
        prefix = "MyApp",              // Filename prefix
        directory = Directory.PICTURES, // Storage directory
        customFolderName = "MyAppPhotos" // Android: custom folder in app directory
    )
)
```

#### Auto-Save Mode

Images automatically saved when camera captures:

```kotlin
val imageSaverPlugin = rememberImageSaverPlugin(
    config = ImageSaverConfig(isAutoSave = true)
)

// Add to camera state
val stateHolder = rememberCameraKState(
    permissions = permissions,
    plugins = listOf(imageSaverPlugin)
)

// Capture automatically saves
scope.launch {
    when (val result = stateHolder.controller?.takePictureToFile()) {
        is ImageCaptureResult.SuccessWithFile -> {
            // Already auto-saved by plugin
            println("File saved: ${result.filePath}")
        }
    }
}
```

#### Manual Save Mode

```kotlin
val imageSaverPlugin = rememberImageSaverPlugin(
    config = ImageSaverConfig(isAutoSave = false)
)

// Manual save
scope.launch {
    when (val result = stateHolder.controller?.takePicture()) {
        is ImageCaptureResult.Success -> {
            imageSaverPlugin.saveImage(
                byteArray = result.byteArray,
                imageName = "Photo_${System.currentTimeMillis()}"
            )
        }
    }
}
```

#### Storage Locations

| Directory | Android Path | iOS Path |
|-----------|--------------|----------|
| `PICTURES` | `/DCIM/` or `Pictures/` | `Photos` app |
| `DOCUMENTS` | `Documents/` | `Files > Documents` |
| `DOWNLOADS` | `Downloads/` | `Files > Downloads` |

### QR Scanner Plugin

Real-time QR code detection from camera frames. Results automatically streamed via `qrCodeFlow`.

#### Setup

```kotlin
val qrScannerPlugin = rememberQRScannerPlugin()

val stateHolder = rememberCameraKState(
    permissions = permissions,
    plugins = listOf(qrScannerPlugin)
)

// Observe QR codes
val qrCodes by stateHolder.qrCodeFlow.collectAsStateWithLifecycle(initial = emptyList())

Text("Last QR: ${qrCodes.lastOrNull() ?: "Scanning..."}")
```

#### Control Scanning

```kotlin
// Start scanning (called automatically on camera ready)
qrScannerPlugin.startScanning()

// Pause scanning (useful after detecting a code)
qrScannerPlugin.pauseScanning()

// Resume scanning
qrScannerPlugin.resumeScanning()

// Get scan results
LaunchedEffect(Unit) {
    stateHolder.qrCodeFlow
        .distinctUntilChanged()
        .collectLatest { qrCode ->
            println("QR Code: $qrCode")
            qrScannerPlugin.pauseScanning()
            
            // Process result...
            delay(2000)
            
            qrScannerPlugin.resumeScanning()
        }
}
```


### OCR Plugin

Optical Character Recognition - detects and extracts text from camera frames. Results automatically streamed via `recognizedTextFlow`.

#### Setup

```kotlin
val ocrPlugin = rememberOcrPlugin()

val stateHolder = rememberCameraKState(
    permissions = permissions,
    plugins = listOf(ocrPlugin)
)

// Observe recognized text
val recognizedText by stateHolder.recognizedTextFlow.collectAsStateWithLifecycle(initial = "")

Text("Detected text: $recognizedText", modifier = Modifier.align(Alignment.TopStart))
```

#### Manual Recognition

```kotlin
scope.launch {
    when (val result = stateHolder.controller?.takePicture()) {
        is ImageCaptureResult.Success -> {
            val text = ocrPlugin.recognizeText(result.byteArray)
            println("Text: $text")
        }
    }
}
```

#### Supported Languages

- English, Spanish, French, German, Italian, Portuguese, Chinese, Japanese, Korean
- Multi-language detection automatic

### Creating Custom Plugins

Plugins implement simple lifecycle interface:

```kotlin
interface CameraKPlugin {
    fun onAttach(stateHolder: CameraKStateHolder)
    fun onDetach()
}
```

#### Example: Custom Text Detection Plugin

```kotlin
@Stable
class CustomTextPlugin : CameraKPlugin {
    override fun onAttach(stateHolder: CameraKStateHolder) {
        // Option 1: Observe state and auto-activate
        stateHolder.pluginScope.launch {
            stateHolder.cameraState
                .filterIsInstance<CameraKState.Ready>()
                .collect { ready ->
                    startTextDetection(ready.controller)
                }
        }
    }
    
    override fun onDetach() {
        // Cleanup: cancel jobs, close resources
    }
    
    private suspend fun startTextDetection(controller: CameraController) {
        // Your detection logic here
    }
}
```

#### Migration from Old Callback API

If you have custom plugins using the deprecated `getController()` approach:

```kotlin
// ❌ OLD (v0.2.0) - Callback based
override fun onAttach(stateHolder: CameraKStateHolder) {
    val controller = stateHolder.getController() // Deprecated
    startDetection(controller)
}

// ✅ NEW (v0.2.0+) - Reactive state based
override fun onAttach(stateHolder: CameraKStateHolder) {
    stateHolder.pluginScope.launch {
        stateHolder.cameraState
            .filterIsInstance<CameraKState.Ready>()
            .collect { ready ->
                startDetection(ready.controller)
            }
    }
}
```



## API Reference

### CameraKStateHolder

Main entry point for all camera operations. Created via `rememberCameraKState()`:

```kotlin
interface CameraKStateHolder {
    // State flows - observe for reactivity
    val cameraState: StateFlow<CameraKState>      // Initializing/Ready/Error
    val qrCodeFlow: StateFlow<List<String>>       // Auto-populated by QRScannerPlugin
    val recognizedTextFlow: StateFlow<String>     // Auto-populated by OcrPlugin
    val eventsFlow: StateFlow<CameraEvent>        // Camera events (focus, zoom, etc.)
    
    // Plugin management
    fun attachPlugin(plugin: CameraKPlugin)
    fun detachPlugin(plugin: CameraKPlugin)
    val pluginScope: CoroutineScope               // For plugin lifecycle operations
    
    // Utilities
    suspend fun getReadyCameraController(): CameraController?  // Wait until camera ready
}
```

### CameraController

Low-level camera operations returned in `CameraKState.Ready`:

```kotlin
interface CameraController {
    // Capture operations
    suspend fun takePictureToFile(): ImageCaptureResult        // Recommended - direct file save
    @Deprecated("Use takePictureToFile()")
    suspend fun takePicture(): ImageCaptureResult              // Legacy - returns ByteArray
    
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
    
    // Event listeners
    fun addImageCaptureListener(listener: (ByteArray) -> Unit)
    fun removeImageCaptureListener(listener: (ByteArray) -> Unit)
}
```

### CameraKPlugin

Base interface for all plugins:

```kotlin
interface CameraKPlugin {
    /**
     * Called when plugin attached to camera state holder.
     * Use this to observe [CameraKStateHolder.cameraState] and auto-activate.
     */
    fun onAttach(stateHolder: CameraKStateHolder)
    
    /**
     * Called when plugin detached or component destroyed.
     * Cancel all jobs and cleanup resources here.
     */
    fun onDetach()
}
```

### ImageCaptureResult

Result of image capture operations:

```kotlin
sealed class ImageCaptureResult {
    data class SuccessWithFile(val filePath: String) : ImageCaptureResult()
    data class Success(val byteArray: ByteArray) : ImageCaptureResult()   // Deprecated
    data class Error(val exception: Exception) : ImageCaptureResult()
}
```

### CameraEvent

Events emitted during camera operation:

```kotlin
sealed class CameraEvent {
    data class FocusChanged(val x: Float, val y: Float) : CameraEvent()
    data class ZoomChanged(val level: Float) : CameraEvent()
    data class FlashModeChanged(val mode: FlashMode) : CameraEvent()
    data class LensChanged(val lens: CameraLens) : CameraEvent()
}

## Migration Guide: v0.2.0 → v0.2.0

### API Migration

The v0.2.0 release introduces a new Compose-first reactive API. The old callback-based API is deprecated but still supported with a one-year deprecation timeline:

- **v0.2.0**: New reactive API available, old API functional with warnings
- **v1.0.0**: Old API marked as error-level deprecation (IDE warnings, future removal)
- **v2.0.0**: Old API removed completely

#### Option 1: Simple Migration (Recommended)

Old callback-based approach:

```kotlin
// ❌ v0.2.0 - Callback based
CameraPreview(
    cameraConfiguration = { /* ... */ },
    onCameraControllerReady = { controller ->
        // Manual controller management
    }
)
```

New reactive approach:

```kotlin
// ✅ v0.2.0+ - Reactive StateFlow based
@Composable
fun CameraScreen() {
    val stateHolder = rememberCameraKState(
        permissions = providePermissions(),
        cameraConfiguration = { /* ... */ }
    )
    
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    
    when (cameraState) {
        is CameraKState.Ready -> {
            val controller = (cameraState as CameraKState.Ready).controller
            // Use controller here
        }
    }
}
```

#### Option 2: Using Helper Method

If you need to wait for camera readiness:

```kotlin
scope.launch {
    val controller = stateHolder.getReadyCameraController()
    if (controller != null) {
        // Camera is ready - safe to use
        when (val result = controller.takePictureToFile()) {
            is ImageCaptureResult.SuccessWithFile -> { /* ... */ }
            is ImageCaptureResult.Error -> { /* ... */ }
        }
    }
}
```

### Plugin Migration

If you've created custom plugins using the old API:

#### ❌ Old Pattern (v0.2.0)

```kotlin
class MyPlugin : CameraKPlugin {
    override fun onAttach(stateHolder: CameraKStateHolder) {
        val controller = stateHolder.getController()  // Deprecated
        // Directly use controller - race condition risk!
        startDetection(controller)
    }
}
```

#### ✅ New Pattern (v0.2.0+)

Option A - Stream observation (reactive):

```kotlin
class MyPlugin : CameraKPlugin {
    private var job: Job? = null
    
    override fun onAttach(stateHolder: CameraKStateHolder) {
        job = stateHolder.pluginScope.launch {
            stateHolder.cameraState
                .filterIsInstance<CameraKState.Ready>()
                .collect { ready ->
                    startDetection(ready.controller)
                }
        }
    }
    
    override fun onDetach() {
        job?.cancel()
    }
}
```

Option B - Suspend until ready:

```kotlin
class MyPlugin : CameraKPlugin {
    override fun onAttach(stateHolder: CameraKStateHolder) {
        stateHolder.pluginScope.launch {
            val controller = stateHolder.getReadyCameraController()
            if (controller != null) {
                startDetection(controller)
            }
        }
    }
    
    override fun onDetach() {
        // Cleanup if needed
    }
}
```

#### Deprecation Timeline for Custom Plugins

The old `getController()` method is deprecated:

```kotlin
// ⚠️ Warning level (v0.2.0)
@Deprecated(
    "Use cameraState.filterIsInstance<Ready>() instead. " +
    "Will be removed in v2.0.0",
    replaceWith = ReplaceWith(
        "pluginScope.launch { cameraState.filterIsInstance<CameraKState.Ready>().collect { ... } }"
    ),
    level = DeprecationLevel.WARNING
)
fun getController(): CameraController?

// ⛔ Error level (v1.0.0)
// Same method but with DeprecationLevel.ERROR

// ❌ Removed (v2.0.0)
// Method no longer exists
```

### Image Capture API

The `takePicture()` method is deprecated in favor of `takePictureToFile()`:

```kotlin
// ❌ Deprecated - Manual file handling required
@Deprecated("Use takePictureToFile() instead")
suspend fun takePicture(): ImageCaptureResult

// ✅ Recommended - Direct file save, 2-3x faster
suspend fun takePictureToFile(): ImageCaptureResult
```

**Performance Benefit:**
- `takePicture()`: ByteArray in memory → manual file write (slower, ~2-3 seconds)
- `takePictureToFile()`: Direct file save (faster, ~0.5-1 second)

### Breaking Changes Summary

| Feature | v0.2.0 | v0.2.0+ | Timeline |
|---------|--------|---------|----------|
| Callback-based API | ✅ | ⚠️ Deprecated | v2.0.0 removal |
| Reactive StateFlow API | ❌ | ✅ Recommended | Current |
| `getController()` | ✅ | ⚠️ Deprecated | v2.0.0 removal |
| `takePicture()` | ✅ | ⚠️ Deprecated | v2.0.0 removal |
| `takePictureToFile()` | ❌ | ✅ Recommended | Current |
| Plugin auto-activation | ❌ | ✅ Built-in | Current |
| `rememberCameraKState()` | ❌ | ✅ New | Current |

### Need More Help?

See [PLUGIN_MIGRATION_GUIDE.md](PLUGIN_MIGRATION_GUIDE.md) for comprehensive custom plugin migration examples.

## Deprecation Notices (Legacy)

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


## Troubleshooting

### Common Issues

#### "Camera not initialized" error

**Problem**: Plugins try to access camera before it's ready.

**Solution**: Always observe `cameraState` and wait for `Ready`:

```kotlin
// ❌ Wrong - immediate access
override fun onAttach(stateHolder: CameraKStateHolder) {
    val controller = stateHolder.getController()  // Might be null!
    startDetection(controller)
}

// ✅ Correct - wait for Ready state
override fun onAttach(stateHolder: CameraKStateHolder) {
    stateHolder.pluginScope.launch {
        stateHolder.cameraState
            .filterIsInstance<CameraKState.Ready>()
            .collect { ready ->
                startDetection(ready.controller)
            }
    }
}
```

#### Plugins not auto-activating

**Problem**: Plugins attached but not starting operations.

**Solution**: Ensure you're using `rememberCameraKState()`:

```kotlin
// ❌ Wrong - old callback API
CameraPreview(
    onCameraControllerReady = { controller ->
        // Auto-activation not supported
    }
)

// ✅ Correct - new reactive API
val stateHolder = rememberCameraKState(
    permissions = permissions,
    plugins = listOf(myPlugin)  // Auto-activates when Ready
)
```

#### Memory leaks from plugins

**Problem**: Plugins continue running after unmount.

**Solution**: Cancel jobs in `onDetach()`:

```kotlin
class MyPlugin : CameraKPlugin {
    private var job: Job? = null
    
    override fun onAttach(stateHolder: CameraKStateHolder) {
        job = stateHolder.pluginScope.launch {
            // Collector will auto-cancel on DisposableEffect cleanup
            stateHolder.cameraState
                .filterIsInstance<CameraKState.Ready>()
                .collect { ready -> startDetection(ready.controller) }
        }
    }
    
    override fun onDetach() {
        job?.cancel()  // Explicit cleanup
    }
}
```

#### QR Scanner not detecting codes

**Problem**: Scanning active but no codes detected.

**Solution**: Verify plugin is attached and check permissions:

```kotlin
val qrPlugin = rememberQRScannerPlugin()

LaunchedEffect(Unit) {
    if (!permissions.hasCameraPermission()) {
        permissions.RequestCameraPermission(
            onGranted = { /* Try again */ },
            onDenied = { /* Handle */ }
        )
    }
}

val stateHolder = rememberCameraKState(
    permissions = permissions,
    plugins = listOf(qrPlugin)
)

// Verify codes coming through
val qrCodes by stateHolder.qrCodeFlow.collectAsStateWithLifecycle(initial = emptyList())
Text("Codes detected: ${qrCodes.size}")
```

#### OCR producing poor results

**Problem**: Text recognition accuracy low on camera stream.

**Solution**: Use high-resolution images for recognition:

```kotlin
// ✅ Better accuracy - from captured image
scope.launch {
    when (val result = controller.takePictureToFile()) {
        is ImageCaptureResult.SuccessWithFile -> {
            val file = File(result.filePath)
            val byteArray = file.readBytes()
            val text = ocrPlugin.recognizeText(byteArray)
        }
    }
}

// ⚠️ Lower accuracy - real-time stream
// ocrPlugin.recognizedTextFlow auto-updated from stream
```

#### Compilation errors with plugins

**Problem**: "Unresolved reference" for plugin classes.

**Solution**: Ensure plugins are added to dependencies:

```gradle
dependencies {
    implementation("io.github.kashif-mehmood-km:camerak:0.2.0")
    implementation("io.github.kashif-mehmood-km:qr_scanner_plugin:0.2.0")
    implementation("io.github.kashif-mehmood-km:ocr_plugin:0.2.0")
    implementation("io.github.kashif-mehmood-km:image_saver_plugin:0.2.0")
}
```

#### Performance: Camera preview stutters

**Problem**: UI frame rate drops when camera active.

**Solution**: Use `collectAsStateWithLifecycle()` instead of `collectAsState()`:

```kotlin
// ❌ Causes recomposition on every frame
val qrCodes by stateHolder.qrCodeFlow.collectAsState()  // Dangerous!

// ✅ Lifecycle-aware, fewer recompositions
val qrCodes by stateHolder.qrCodeFlow.collectAsStateWithLifecycle()
```

### Debug Logging

Enable debug logging to diagnose issues:

```kotlin
// In your initialization code
if (BuildConfig.DEBUG) {
    stateHolder.eventsFlow.collectLatest { event ->
        Log.d("CameraK", "Event: $event")
    }
}
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
