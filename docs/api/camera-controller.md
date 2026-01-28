# CameraController

Core API for controlling camera operations at runtime.

## Interface Definition

```kotlin
expect class CameraController {
    // Capture
    @Deprecated("Use takePictureToFile() instead")
    suspend fun takePicture(): ImageCaptureResult
    suspend fun takePictureToFile(): ImageCaptureResult
    
    // Flash control
    fun toggleFlashMode()
    fun setFlashMode(mode: FlashMode)
    fun getFlashMode(): FlashMode?
    
    // Torch control
    fun toggleTorchMode()
    fun setTorchMode(mode: TorchMode)
    fun getTorchMode(): TorchMode?
    
    // Camera lens
    fun toggleCameraLens()
    fun getCameraLens(): CameraLens?
    
    // Zoom control
    fun setZoom(zoomRatio: Float)
    fun getZoom(): Float
    fun getMaxZoom(): Float
    
    // Getters
    fun getImageFormat(): ImageFormat
    fun getQualityPrioritization(): QualityPrioritization
    fun getPreferredCameraDeviceType(): CameraDeviceType
    
    // Session management
    fun startSession()
    fun stopSession()
    
    // Listeners & cleanup
    fun addImageCaptureListener(listener: (ByteArray) -> Unit)
    fun initializeControllerPlugins()
    fun cleanup()
}
```

## Image Capture

### takePictureToFile()

**Recommended method** - Captures an image and saves it directly to a file for optimal performance.

```kotlin
suspend fun takePictureToFile(): ImageCaptureResult
```

**Returns:**
- `ImageCaptureResult.SuccessWithFile` - Contains the file path
- `ImageCaptureResult.Error` - On capture failure

**Benefits:**
- 2-3 seconds faster than `takePicture()`
- No ByteArray conversion overhead
- Direct disk write without decode/encode cycles

**Example:**
```kotlin
when (val result = controller.takePictureToFile()) {
    is ImageCaptureResult.SuccessWithFile -> {
        println("Saved to: ${result.filePath}")
    }
    is ImageCaptureResult.Error -> {
        println("Error: ${result.exception.message}")
    }
}
```

### takePicture()

**Deprecated** - Captures an image and returns it as ByteArray. Use `takePictureToFile()` instead for better performance.

```kotlin
@Deprecated("Use takePictureToFile() instead")
suspend fun takePicture(): ImageCaptureResult
```

**Returns:**
- `ImageCaptureResult.Success` - Contains ByteArray
- `ImageCaptureResult.Error` - On capture failure

**Note:** This method will be removed in v2.0.0.

## Flash Control

### toggleFlashMode()

Cycles through flash modes: OFF → ON → AUTO → OFF

```kotlin
fun toggleFlashMode()
```

**Example:**
```kotlin
controller.toggleFlashMode()
val currentMode = controller.getFlashMode()
```

### setFlashMode()

Sets a specific flash mode.

```kotlin
fun setFlashMode(mode: FlashMode)
```

**Parameters:**
- `mode` - `FlashMode.ON`, `FlashMode.OFF`, or `FlashMode.AUTO`

**Example:**
```kotlin
controller.setFlashMode(FlashMode.AUTO)
```

### getFlashMode()

Gets the current flash mode.

```kotlin
fun getFlashMode(): FlashMode?
```

**Returns:** Current `FlashMode` or null if not available

## Torch Control

### toggleTorchMode()

Cycles through torch modes: OFF → ON → AUTO → OFF

```kotlin
fun toggleTorchMode()
```

**Platform notes:**
- **Android:** AUTO mode is not supported by CameraX and will be treated as ON
- **iOS:** AUTO mode is fully supported through AVFoundation
- **Desktop:** Torch is not available

### setTorchMode()

Sets a specific torch mode.

```kotlin
fun setTorchMode(mode: TorchMode)
```

**Parameters:**
- `mode` - `TorchMode.ON`, `TorchMode.OFF`, or `TorchMode.AUTO`

**Platform notes:**
- **Android:** AUTO will be treated as ON
- **iOS:** AUTO is supported
- **Desktop:** No-op

**Example:**
```kotlin
controller.setTorchMode(TorchMode.ON)
```

### getTorchMode()

Gets the current torch mode.

```kotlin
fun getTorchMode(): TorchMode?
```

**Returns:** Current `TorchMode` or null if not available (Desktop)

## Camera Lens

### toggleCameraLens()

Switches between front and back cameras.

```kotlin
fun toggleCameraLens()
```

**Platform notes:**
- **Desktop:** No-op (single camera)

**Example:**
```kotlin
controller.toggleCameraLens()
val currentLens = controller.getCameraLens()
```

### getCameraLens()

Gets the current camera lens.

```kotlin
fun getCameraLens(): CameraLens?
```

**Returns:** `CameraLens.FRONT` or `CameraLens.BACK`, or null if not available

## Zoom Control

### setZoom()

Sets the zoom level.

```kotlin
fun setZoom(zoomRatio: Float)
```

**Parameters:**
- `zoomRatio` - Zoom ratio where 1.0 is no zoom, values > 1.0 zoom in
  - **Android:** Typically 1.0 to 2.0-10.0 depending on hardware
  - **iOS:** Typically 1.0 to device.maxAvailableVideoZoomFactor
  - **Desktop:** Not supported (no-op)

**Example:**
```kotlin
controller.setZoom(2.5f)
```

### getZoom()

Gets the current zoom ratio.

```kotlin
fun getZoom(): Float
```

**Returns:** Current zoom ratio, or 1.0 if zoom is not supported

### getMaxZoom()

Gets the maximum supported zoom ratio.

```kotlin
fun getMaxZoom(): Float
```

**Returns:** Maximum zoom ratio, or 1.0 if zoom is not supported

**Example:**
```kotlin
val maxZoom = controller.getMaxZoom()
println("Max zoom: ${maxZoom}x")

// Set to half of max zoom
controller.setZoom(maxZoom / 2)
```

## Configuration Getters

### getImageFormat()

Gets the configured image format.

```kotlin
fun getImageFormat(): ImageFormat
```

**Returns:** `ImageFormat.JPEG` or `ImageFormat.PNG`

### getQualityPrioritization()

Gets the quality prioritization setting.

```kotlin
fun getQualityPrioritization(): QualityPrioritization
```

**Returns:** 
- `QualityPrioritization.QUALITY` - Best quality, slower
- `QualityPrioritization.SPEED` - Faster capture, lower quality
- `QualityPrioritization.BALANCED` - Balanced approach
- `QualityPrioritization.NONE` - Platform default

### getPreferredCameraDeviceType()

Gets the preferred camera device type.

```kotlin
fun getPreferredCameraDeviceType(): CameraDeviceType
```

**Returns:**
- `CameraDeviceType.DEFAULT` - Default camera
- `CameraDeviceType.WIDE_ANGLE` - Wide angle camera
- `CameraDeviceType.TELEPHOTO` - Telephoto camera
- `CameraDeviceType.ULTRA_WIDE` - Ultra-wide camera
- Others based on platform support

## Session Management

### startSession()

Starts the camera session. Called automatically when `CameraKState` transitions to `Ready`.

```kotlin
fun startSession()
```

### stopSession()

Stops the camera session. Called automatically when disposing.

```kotlin
fun stopSession()
```

## Listeners & Cleanup

### addImageCaptureListener()

Adds a listener for image capture events.

```kotlin
fun addImageCaptureListener(listener: (ByteArray) -> Unit)
```

**Parameters:**
- `listener` - Lambda receiving captured image as ByteArray

**Example:**
```kotlin
controller.addImageCaptureListener { imageData ->
    println("Captured ${imageData.size} bytes")
    processImage(imageData)
}
```

### initializeControllerPlugins()

Initializes all registered plugins. Called automatically during controller setup.

```kotlin
fun initializeControllerPlugins()
```

### cleanup()

Cleans up resources when the controller is no longer needed.

```kotlin
fun cleanup()
```

**Important:** After calling `cleanup()`, the controller should not be used again.

**Note:** This is typically called automatically when the `CameraKStateHolder` is disposed.

## Complete Example

```kotlin
// Get controller from Ready state
val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()

when (cameraState) {
    is CameraKState.Ready -> {
        val controller = (cameraState as CameraKState.Ready).controller
        
        // Configure zoom
        val maxZoom = controller.getMaxZoom()
        controller.setZoom(maxZoom / 2)
        
        // Configure flash
        controller.setFlashMode(FlashMode.AUTO)
        
        // Switch to front camera
        controller.toggleCameraLens()
        
        // Capture image
        launch {
            when (val result = controller.takePictureToFile()) {
                is ImageCaptureResult.SuccessWithFile -> {
                    println("Saved to: ${result.filePath}")
                }
                is ImageCaptureResult.Error -> {
                    println("Error: ${result.exception.message}")
                }
            }
        }
    }
    is CameraKState.Error -> {
        println("Camera error: ${(cameraState as CameraKState.Error).exception}")
    }
    else -> {
        // Handle other states
    }
}
```

## See Also

- [Configuration Guide](../getting-started/configuration.md)
- [Data Models](models.md)
- [Examples](../examples/android.md)
