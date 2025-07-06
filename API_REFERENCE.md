# CameraK API Reference

## 📖 Overview

This document provides comprehensive API documentation for CameraK, including all public classes, interfaces, and functions available to developers.

## 🎯 Core APIs

### CameraController

The main interface for camera operations across all platforms.

```kotlin
expect class CameraController {
    /**
     * Captures an image asynchronously.
     * 
     * @return ImageCaptureResult containing either success with image data or error
     */
    suspend fun takePicture(): ImageCaptureResult
    
    /**
     * Toggles flash mode through ON -> OFF -> AUTO -> ON cycle.
     */
    fun toggleFlashMode()
    
    /**
     * Sets specific flash mode.
     * 
     * @param mode The desired FlashMode (ON, OFF, AUTO)
     */
    fun setFlashMode(mode: FlashMode)
    
    /**
     * Gets current flash mode.
     * 
     * @return Current FlashMode or null if not available
     */
    fun getFlashMode(): FlashMode?
    
    /**
     * Toggles torch mode between ON and OFF.
     * Note: iOS also supports AUTO mode.
     */
    fun toggleTorchMode()
    
    /**
     * Sets specific torch mode.
     * 
     * @param mode The desired TorchMode (ON, OFF, AUTO)
     */
    fun setTorchMode(mode: TorchMode)
    
    /**
     * Toggles camera lens between FRONT and BACK.
     */
    fun toggleCameraLens()
    
    /**
     * Starts the camera session.
     */
    fun startSession()
    
    /**
     * Stops the camera session.
     */
    fun stopSession()
    
    /**
     * Adds a listener for image capture events.
     * 
     * @param listener Callback function receiving captured image as ByteArray
     */
    fun addImageCaptureListener(listener: (ByteArray) -> Unit)
    
    /**
     * Initializes all registered plugins.
     */
    fun initializeControllerPlugins()
}
```

### CameraPreview

Cross-platform Composable for camera preview display.

```kotlin
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraConfiguration: CameraControllerBuilder.() -> Unit,
    onCameraControllerReady: (CameraController) -> Unit
)
```

**Parameters:**
- `modifier`: Modifier for styling the preview component
- `cameraConfiguration`: Lambda to configure camera settings using builder pattern
- `onCameraControllerReady`: Callback invoked when camera controller is initialized

**Usage Example:**
```kotlin
CameraPreview(
    modifier = Modifier.fillMaxSize(),
    cameraConfiguration = {
        setCameraLens(CameraLens.BACK)
        setFlashMode(FlashMode.OFF)
        addPlugin(imageSaverPlugin)
    },
    onCameraControllerReady = { controller ->
        // Camera is ready for use
    }
)
```

## 🏗️ Builder Pattern APIs

### CameraControllerBuilder

Interface for configuring camera settings using builder pattern.

```kotlin
interface CameraControllerBuilder {
    /**
     * Sets flash mode for camera.
     * 
     * @param flashMode Desired flash mode (ON, OFF, AUTO)
     * @return Builder instance for chaining
     */
    fun setFlashMode(flashMode: FlashMode): CameraControllerBuilder
    
    /**
     * Sets camera lens to use.
     * 
     * @param cameraLens Desired camera lens (FRONT, BACK)
     * @return Builder instance for chaining
     */
    fun setCameraLens(cameraLens: CameraLens): CameraControllerBuilder
    
    /**
     * Sets image format for captured photos.
     * 
     * @param imageFormat Desired format (JPEG, PNG)
     * @return Builder instance for chaining
     */
    fun setImageFormat(imageFormat: ImageFormat): CameraControllerBuilder
    
    /**
     * Sets directory for saving images.
     * 
     * @param directory Target directory (PICTURES, DCIM, DOWNLOADS)
     * @return Builder instance for chaining
     */
    fun setDirectory(directory: Directory): CameraControllerBuilder
    
    /**
     * Adds a plugin to the camera controller.
     * 
     * @param plugin Plugin implementing CameraPlugin interface
     * @return Builder instance for chaining
     */
    fun addPlugin(plugin: CameraPlugin): CameraControllerBuilder
    
    /**
     * Sets torch mode for camera.
     * 
     * @param torchMode Desired torch mode (ON, OFF, AUTO)
     * @return Builder instance for chaining
     */
    fun setTorchMode(torchMode: TorchMode): CameraControllerBuilder
    
    /**
     * Sets quality prioritization for image capture.
     * 
     * @param prioritization Quality vs speed preference (QUALITY, SPEED)
     * @return Builder instance for chaining
     */
    fun setQualityPrioritization(prioritization: QualityPrioritization): CameraControllerBuilder
    
    /**
     * Builds configured camera controller.
     * 
     * @return Configured CameraController instance
     * @throws InvalidConfigurationException for invalid configurations
     */
    fun build(): CameraController
}
```

### Factory Function

```kotlin
/**
 * Creates a platform-specific camera controller builder.
 * 
 * @return CameraControllerBuilder instance for current platform
 */
fun createCameraControllerBuilder(): CameraControllerBuilder
```

## 📊 Data Types and Enums

### ImageCaptureResult

Sealed class representing image capture operation results.

```kotlin
sealed class ImageCaptureResult {
    /**
     * Successful image capture.
     * 
     * @param byteArray Captured image data
     */
    data class Success(val byteArray: ByteArray) : ImageCaptureResult()
    
    /**
     * Failed image capture.
     * 
     * @param exception Error that occurred during capture
     */
    data class Error(val exception: Exception) : ImageCaptureResult()
}
```

### Enums

#### CameraLens
```kotlin
enum class CameraLens {
    FRONT,  // Front-facing camera
    BACK    // Back-facing camera
}
```

#### FlashMode
```kotlin
enum class FlashMode {
    ON,     // Flash always on
    OFF,    // Flash always off
    AUTO    // Flash automatic based on lighting
}
```

#### TorchMode
```kotlin
enum class TorchMode {
    ON,     // Torch always on
    OFF,    // Torch always off
    AUTO    // Torch automatic (iOS only)
}
```

#### ImageFormat
```kotlin
enum class ImageFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png")
}
```

#### Directory
```kotlin
enum class Directory {
    PICTURES,   // Pictures directory
    DCIM,       // DCIM directory (Android)
    DOWNLOADS   // Downloads directory
}
```

#### QualityPrioritization
```kotlin
enum class QualityPrioritization {
    QUALITY,    // Prioritize image quality
    SPEED       // Prioritize capture speed
}
```

## 🔌 Plugin System APIs

### CameraPlugin Interface

Base interface for all camera plugins.

```kotlin
interface CameraPlugin {
    /**
     * Initializes the plugin with camera controller.
     * Called when camera controller is ready.
     * 
     * @param cameraController The initialized camera controller
     */
    fun initialize(cameraController: CameraController)
}
```

### Plugin Development Template

```kotlin
class CustomPlugin : CameraPlugin {
    override fun initialize(cameraController: CameraController) {
        // Register for camera events
        cameraController.addImageCaptureListener { imageData ->
            // Process captured image
            processImage(imageData)
        }
    }
    
    private fun processImage(imageData: ByteArray) {
        // Custom image processing logic
    }
}
```

## 📦 Built-in Plugins

### ImageSaverPlugin

Plugin for saving captured images to device storage.

```kotlin
class ImageSaverPlugin(val config: ImageSaverConfig) : CameraPlugin {
    /**
     * Saves image with custom name.
     * 
     * @param byteArray Image data to save
     * @param imageName Custom name for the image
     * @return Path where image was saved, or null if failed
     */
    suspend fun saveImage(byteArray: ByteArray, imageName: String): String?
}

/**
 * Creates and remembers ImageSaverPlugin instance.
 * 
 * @param config Configuration for image saving behavior
 * @return Remembered ImageSaverPlugin instance
 */
@Composable
fun rememberImageSaverPlugin(
    config: ImageSaverConfig = ImageSaverConfig()
): ImageSaverPlugin
```

#### ImageSaverConfig
```kotlin
data class ImageSaverConfig(
    val isAutoSave: Boolean = true,           // Automatically save captured images
    val prefix: String = "CameraK",           // Filename prefix
    val directory: Directory = Directory.PICTURES, // Target directory
    val customFolderName: String? = null      // Custom folder name (Android only)
)
```

### QRScannerPlugin

Plugin for QR code and barcode scanning.

```kotlin
class QRScannerPlugin : CameraPlugin {
    /**
     * Gets flow of detected QR codes.
     * 
     * @return SharedFlow emitting detected QR code strings
     */
    fun getQrCodeFlow(): SharedFlow<String>
    
    /**
     * Starts QR code scanning.
     */
    fun startScanning()
    
    /**
     * Pauses QR code scanning.
     */
    fun pauseScanning()
    
    /**
     * Resumes QR code scanning.
     */
    fun resumeScanning()
}

/**
 * Creates and remembers QRScannerPlugin instance.
 * 
 * @param coroutineScope Scope for coroutine operations
 * @return Remembered QRScannerPlugin instance
 */
@Composable
fun rememberQRScannerPlugin(
    coroutineScope: CoroutineScope
): QRScannerPlugin
```

### OcrPlugin

Plugin for optical character recognition (text detection).

```kotlin
class OcrPlugin : CameraPlugin {
    /**
     * Gets flow of detected text.
     * 
     * @return SharedFlow emitting detected text strings
     */
    fun getTextFlow(): SharedFlow<String>
    
    /**
     * Starts text recognition.
     */
    fun startTextRecognition()
    
    /**
     * Stops text recognition.
     */
    fun stopTextRecognition()
}

/**
 * Creates and remembers OcrPlugin instance.
 * 
 * @param coroutineScope Scope for coroutine operations
 * @return Remembered OcrPlugin instance
 */
@Composable
fun rememberOcrPlugin(
    coroutineScope: CoroutineScope
): OcrPlugin
```

## 🔐 Permissions APIs

### Permissions Interface

Cross-platform interface for camera and storage permissions.

```kotlin
interface Permissions {
    /**
     * Checks if camera permission is granted.
     * 
     * @return true if camera permission is granted
     */
    fun hasCameraPermission(): Boolean
    
    /**
     * Checks if storage permission is granted.
     * 
     * @return true if storage permission is granted
     */
    fun hasStoragePermission(): Boolean
    
    /**
     * Requests camera permission from user.
     * 
     * @param onGranted Callback when permission is granted
     * @param onDenied Callback when permission is denied
     */
    @Composable
    fun RequestCameraPermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit
    )
    
    /**
     * Requests storage permission from user.
     * 
     * @param onGranted Callback when permission is granted
     * @param onDenied Callback when permission is denied
     */
    @Composable
    fun RequestStoragePermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit
    )
}

/**
 * Provides platform-specific permissions implementation.
 * 
 * @return Permissions instance for current platform
 */
@Composable
fun providePermissions(): Permissions
```

## 🚨 Exception Handling

### InvalidConfigurationException

Thrown when camera configuration is invalid.

```kotlin
class InvalidConfigurationException(message: String) : Exception(message)
```

**Common scenarios:**
- Missing required permissions
- Incompatible camera settings
- Hardware limitations
- Platform-specific restrictions

## 💡 Usage Examples

### Basic Camera Setup

```kotlin
@Composable
fun SimpleCameraScreen() {
    val permissions = providePermissions()
    var cameraController by remember { mutableStateOf<CameraController?>(null) }
    
    if (permissions.hasCameraPermission()) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            cameraConfiguration = {
                setCameraLens(CameraLens.BACK)
                setFlashMode(FlashMode.AUTO)
            },
            onCameraControllerReady = { controller ->
                cameraController = controller
            }
        )
        
        // Capture button
        Button(
            onClick = {
                cameraController?.let { controller ->
                    coroutineScope.launch {
                        when (val result = controller.takePicture()) {
                            is ImageCaptureResult.Success -> {
                                // Handle success
                            }
                            is ImageCaptureResult.Error -> {
                                // Handle error
                            }
                        }
                    }
                }
            }
        ) {
            Text("Capture")
        }
    } else {
        permissions.RequestCameraPermission(
            onGranted = { /* Permission granted */ },
            onDenied = { /* Permission denied */ }
        )
    }
}
```

### Advanced Configuration with Plugins

```kotlin
@Composable
fun AdvancedCameraScreen() {
    val coroutineScope = rememberCoroutineScope()
    val imageSaverPlugin = rememberImageSaverPlugin(
        config = ImageSaverConfig(
            isAutoSave = false,
            prefix = "MyApp",
            directory = Directory.PICTURES
        )
    )
    val qrScannerPlugin = rememberQRScannerPlugin(coroutineScope)
    val ocrPlugin = rememberOcrPlugin(coroutineScope)
    
    var cameraController by remember { mutableStateOf<CameraController?>(null) }
    var detectedQRCode by remember { mutableStateOf<String?>(null) }
    var detectedText by remember { mutableStateOf<String?>(null) }
    
    // Listen for QR codes
    LaunchedEffect(Unit) {
        qrScannerPlugin.getQrCodeFlow().collect { qrCode ->
            detectedQRCode = qrCode
        }
    }
    
    // Listen for text
    LaunchedEffect(Unit) {
        ocrPlugin.getTextFlow().collect { text ->
            detectedText = text
        }
    }
    
    CameraPreview(
        modifier = Modifier.fillMaxSize(),
        cameraConfiguration = {
            setCameraLens(CameraLens.BACK)
            setFlashMode(FlashMode.AUTO)
            setImageFormat(ImageFormat.JPEG)
            setQualityPrioritization(QualityPrioritization.QUALITY)
            addPlugin(imageSaverPlugin)
            addPlugin(qrScannerPlugin)
            addPlugin(ocrPlugin)
        },
        onCameraControllerReady = { controller ->
            cameraController = controller
            qrScannerPlugin.startScanning()
            ocrPlugin.startTextRecognition()
        }
    )
    
    // Display detected content
    detectedQRCode?.let { qr ->
        Text("QR: $qr", modifier = Modifier.padding(16.dp))
    }
    
    detectedText?.let { text ->
        Text("Text: $text", modifier = Modifier.padding(16.dp))
    }
}
```

## 🔧 Platform-Specific Extensions

### Android Extensions

```kotlin
// Android-specific CameraController methods
actual class CameraController {
    /**
     * Gets current camera info (Android only).
     */
    fun getCameraInfo(): CameraInfo
    
    /**
     * Sets camera exposure compensation (Android only).
     */
    fun setExposureCompensation(value: Int)
}
```

### iOS Extensions

```kotlin
// iOS-specific CameraController methods
actual class CameraController {
    /**
     * Sets focus point (iOS only).
     */
    fun setFocusPoint(x: Float, y: Float)
    
    /**
     * Sets zoom level (iOS only).
     */
    fun setZoomLevel(zoom: Float)
}
```

### Desktop Extensions

```kotlin
// Desktop-specific CameraController methods
actual class CameraController {
    /**
     * Lists available cameras (Desktop only).
     */
    fun getAvailableCameras(): List<String>
    
    /**
     * Selects camera by index (Desktop only).
     */
    fun selectCamera(index: Int)
}
```

This API reference provides comprehensive documentation for all public APIs in CameraK, enabling developers to effectively use and extend the library.