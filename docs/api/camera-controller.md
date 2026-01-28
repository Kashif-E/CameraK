# CameraController

Core API for controlling camera operations.

## Interface Definition

```kotlin
interface CameraController {
    // Lifecycle
    fun startPreview()
    fun stopPreview()
    fun release()
    
    // Capture
    suspend fun capturePhoto(): Photo
    suspend fun capturePhotoWithEffect(effect: PhotoEffect): Photo
    suspend fun captureBurst(count: Int, interval: Long): List<Photo>
    
    // Video
    fun startVideoRecording()
    suspend fun stopVideoRecording(): Video
    
    // Configuration
    fun setCameraSelector(selector: CameraSelector)
    fun setFlash(flash: CameraFlash)
    fun setZoom(zoomRatio: Float)
    fun setExposure(compensationValue: Float)
    fun setWhiteBalance(whiteBalance: WhiteBalance)
    fun setFocusMode(focusMode: FocusMode)
    
    // Getters
    fun getCurrentCameraSelector(): CameraSelector
    fun getMinZoomRatio(): Float
    fun getMaxZoomRatio(): Float
    fun isTorchAvailable(): Boolean
    fun isAutoFocusAvailable(): Boolean
}
```

## Methods

### startPreview()

Starts the camera preview stream.

```kotlin
fun startPreview()
```

**Throws:**
- `PermissionDeniedException` – Camera permission not granted
- `CameraNotAvailableException` – No camera device available
- `CameraException` – General camera error

**Example:**
```kotlin
try {
    controller.startPreview()
} catch (e: PermissionDeniedException) {
    println("Request camera permission")
}
```

### stopPreview()

Stops the camera preview stream.

```kotlin
fun stopPreview()
```

**Note:** Call this when leaving the camera screen to free resources.

### capturePhoto()

Captures a single photo.

```kotlin
suspend fun capturePhoto(): Photo
```

**Returns:**
- `Photo` object with uri, dimensions, metadata

**Throws:**
- `CameraTimeoutException` – Capture timed out
- `StorageException` – No storage space available
- `CameraException` – General error

**Example:**
```kotlin
viewModelScope.launch {
    try {
        val photo = controller.capturePhoto()
        println("Photo saved: ${photo.uri}")
    } catch (e: StorageException) {
        println("No storage space available")
    }
}
```

### capturePhotoWithEffect()

Captures a photo with an effect applied.

```kotlin
suspend fun capturePhotoWithEffect(effect: PhotoEffect): Photo
```

**Parameters:**
- `effect` – `PhotoEffect.BEAUTY_FILTER`, `PhotoEffect.BLACK_AND_WHITE`, `PhotoEffect.HDR`, etc.

**Example:**
```kotlin
val beautifulPhoto = controller.capturePhotoWithEffect(
    effect = PhotoEffect.BEAUTY_FILTER
)
```

### captureBurst()

Captures multiple photos in rapid succession.

```kotlin
suspend fun captureBurst(count: Int, interval: Long): List<Photo>
```

**Parameters:**
- `count` – Number of photos to capture (1-20)
- `interval` – Time between captures in milliseconds

**Example:**
```kotlin
val photos = controller.captureBurst(
    count = 10,
    interval = 100
)
```

### startVideoRecording()

Starts recording video.

```kotlin
fun startVideoRecording()
```

**Throws:**
- `AudioPermissionException` – Microphone permission not granted (when audio enabled)
- `StorageException` – Insufficient storage

**Example:**
```kotlin
try {
    controller.startVideoRecording()
} catch (e: StorageException) {
    println("Insufficient storage for video")
}
```

### stopVideoRecording()

Stops video recording and returns the file.

```kotlin
suspend fun stopVideoRecording(): Video
```

**Returns:**
- `Video` object with uri, duration, codec, etc.

**Example:**
```kotlin
val video = controller.stopVideoRecording()
println("Video duration: ${video.duration}ms")
```

### setCameraSelector()

Switches between front and back cameras.

```kotlin
fun setCameraSelector(selector: CameraSelector)
```

**Parameters:**
- `selector` – `CameraSelector.BACK` or `CameraSelector.FRONT`

**Throws:**
- `CameraNotAvailableException` – Selected camera not available

**Example:**
```kotlin
// Switch to selfie camera
controller.setCameraSelector(CameraSelector.FRONT)

// Switch back to main camera
controller.setCameraSelector(CameraSelector.BACK)
```

### setFlash()

Controls flash mode.

```kotlin
fun setFlash(flash: CameraFlash)
```

**Parameters:**
- `flash` – `CameraFlash.ON`, `CameraFlash.OFF`, or `CameraFlash.AUTO`

**Example:**
```kotlin
controller.setFlash(CameraFlash.AUTO)
```

### setZoom()

Sets zoom level.

```kotlin
fun setZoom(zoomRatio: Float)
```

**Parameters:**
- `zoomRatio` – Zoom multiplier (1.0 = no zoom, max = `getMaxZoomRatio()`)

**Throws:**
- `IllegalArgumentException` – Zoom ratio out of range

**Example:**
```kotlin
// 2x zoom
controller.setZoom(2.0f)

// Get supported range
val minZoom = controller.getMinZoomRatio()
val maxZoom = controller.getMaxZoomRatio()
controller.setZoom(maxZoom / 2)  // Half max zoom
```

### setExposure()

Adjusts exposure compensation.

```kotlin
fun setExposure(compensationValue: Float)
```

**Parameters:**
- `compensationValue` – Exposure compensation (-2.0 to +2.0)

**Example:**
```kotlin
controller.setExposure(-0.5f)  // Darker
controller.setExposure(0.5f)   // Brighter
controller.setExposure(0.0f)   // Normal
```

### setWhiteBalance()

Sets white balance mode.

```kotlin
fun setWhiteBalance(whiteBalance: WhiteBalance)
```

**Parameters:**
- `whiteBalance` – `WhiteBalance.AUTO`, `DAYLIGHT`, `CLOUDY`, `TUNGSTEN`, `FLUORESCENT`

**Example:**
```kotlin
controller.setWhiteBalance(WhiteBalance.DAYLIGHT)
```

### setFocusMode()

Configures focus behavior.

```kotlin
fun setFocusMode(focusMode: FocusMode)
```

**Parameters:**
- `focusMode` – `FocusMode.AUTO`, `MANUAL`, or `CONTINUOUS`

**Example:**
```kotlin
// Continuous autofocus (video mode)
controller.setFocusMode(FocusMode.CONTINUOUS)

// Manual focus control
controller.setFocusMode(FocusMode.MANUAL)
controller.setFocusDistance(0.5f)
```

## Getters

### getMinZoomRatio()

```kotlin
fun getMinZoomRatio(): Float
```

Returns minimum supported zoom (typically 1.0).

### getMaxZoomRatio()

```kotlin
fun getMaxZoomRatio(): Float
```

Returns maximum supported zoom (typically 2.0-10.0 depending on device).

### isTorchAvailable()

```kotlin
fun isTorchAvailable(): Boolean
```

Checks if flashlight/torch is available.

### isAutoFocusAvailable()

```kotlin
fun isAutoFocusAvailable(): Boolean
```

Checks if autofocus is supported.

## Builder Pattern

### CameraController.Builder

```kotlin
val controller = CameraController.Builder()
    .setCameraSelector(CameraSelector.BACK)
    .setPhotoResolution(3840, 2160)
    .setVideoResolution(1920, 1080)
    .setFlash(CameraFlash.AUTO)
    .setFocusMode(FocusMode.CONTINUOUS)
    .build()
```

See [Configuration Guide](../getting-started/configuration.md) for all builder options.

## Exception Hierarchy

```
CameraException
├── PermissionDeniedException
├── CameraNotAvailableException
├── CameraTimeoutException
├── StorageException
├── AudioPermissionException
└── GeneralCameraException
```

## Usage Examples

### Complete Camera App

```kotlin
@Composable
fun SimpleCameraApp(viewModel: CameraViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraState = rememberCameraKState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        CameraPreviewComposable(
            controller = cameraState.controller,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        
        // Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Switch Camera
            IconButton(onClick = {
                val selector = if (state.isFrontCamera) {
                    CameraSelector.BACK
                } else {
                    CameraSelector.FRONT
                }
                cameraState.controller.setCameraSelector(selector)
                viewModel.toggleCamera()
            }) {
                Icon(Icons.Default.Flip, contentDescription = "Switch Camera")
            }
            
            // Capture Button
            IconButton(onClick = {
                viewModel.capturePhoto(cameraState.controller)
            }) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Capture")
            }
            
            // Flash Control
            IconButton(onClick = {
                viewModel.toggleFlash(cameraState.controller)
            }) {
                Icon(Icons.Default.Flashlight, contentDescription = "Flash")
            }
        }
    }
}
```

## Best Practices

1. ✅ Check permissions before starting preview
2. ✅ Call `stopPreview()` when leaving camera screen
3. ✅ Wrap suspend functions in try-catch
4. ✅ Test zoom levels before using `setZoom()`
5. ✅ Use `isTorchAvailable()` before enabling flash

## Platform-Specific Notes

- **Android:** Uses CameraX 1.5+
- **iOS:** Uses AVFoundation
- **Desktop:** Uses OpenCV/JavaCV

## See Also

- [Getting Started](../getting-started/quick-start.md)
- [Configuration](../getting-started/configuration.md)
- [Camera Capture Guide](../guide/camera-capture.md)
