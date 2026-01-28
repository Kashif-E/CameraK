# Core Concepts

Understand the fundamental concepts behind CameraK.

## Architecture Overview

CameraK is built on a layered architecture:

```
┌─────────────────────────────────┐
│   Compose UI Layer              │
│   (Camera Preview, Controls)    │
├─────────────────────────────────┤
│   CameraController (API)        │
│   High-level abstraction        │
├─────────────────────────────────┤
│   Platform Implementation       │
│   (Android/iOS/Desktop)         │
├─────────────────────────────────┤
│   Native Camera Framework       │
│   (CameraX/AVFoundation/JavaCV) │
└─────────────────────────────────┘
```

## Key Components

### CameraController

The main API for controlling camera operations:

```kotlin
interface CameraController {
    // Preview
    fun startPreview()
    fun stopPreview()
    
    // Capture
    suspend fun capturePhoto(): Photo
    suspend fun capturePhotoWithEffect(effect: PhotoEffect): Photo
    
    // Recording
    fun startVideoRecording()
    fun stopVideoRecording(): Video
    
    // Configuration
    fun setCameraSelector(selector: CameraSelector)
    fun setFlash(flash: CameraFlash)
    fun setZoom(zoomRatio: Float)
    fun setExposure(compensationValue: Float)
}
```

### Camera State

Manages camera lifecycle and state:

```kotlin
@Composable
fun rememberCameraKState(): CameraKState {
    return remember {
        CameraKState()
    }
}
```

### Camera Preview

Displays camera feed in Compose:

```kotlin
@Composable
fun CameraPreviewComposable(
    controller: CameraController,
    modifier: Modifier = Modifier
)
```

## Camera Lifecycle

```
┌─────────────────┐
│   Not Started   │
└────────┬────────┘
         │
         │ startPreview()
         ▼
┌─────────────────┐
│   Previewing    │◄────┐
└────────┬────────┘     │
         │              │
    ┌────┴────┬─────────┘
    │         │
    ▼         │ stopPreview()
┌─────────┐  │
│Capturing│──┘
└────┬────┘
     │
     ▼
┌─────────────────┐
│   Recording     │
└────────┬────────┘
         │
         │ stopVideoRecording()
         ▼
┌──────────────┐
│   Stopped    │
└──────────────┘
```

## Permission Model

CameraK automatically handles permissions:

```kotlin
@Composable
fun CameraScreen() {
    // Permissions are requested automatically
    val cameraState = rememberCameraKState()
    
    // Check permission status
    when (cameraState.permissionState) {
        PermissionState.GRANTED -> {
            // Camera ready
            CameraPreviewComposable(cameraState.controller)
        }
        PermissionState.DENIED -> {
            Text("Camera permission required")
        }
        PermissionState.PENDING -> {
            CircularProgressIndicator()
        }
    }
}
```

## Error Handling

CameraK uses sealed result types for errors:

```kotlin
sealed class CameraResult<out T> {
    data class Success<T>(val data: T) : CameraResult<T>()
    data class Error<T>(
        val exception: CameraException,
        val isRetryable: Boolean
    ) : CameraResult<T>()
    class Loading<T> : CameraResult<T>()
}
```

Usage:

```kotlin
viewModelScope.launch {
    when (val result = controller.capturePhoto()) {
        is CameraResult.Success -> {
            // Handle photo
        }
        is CameraResult.Error -> {
            if (result.isRetryable) {
                // Retry logic
            }
        }
        is CameraResult.Loading -> {
            // Show loading
        }
    }
}
```

## Threading Model

CameraK uses Kotlin coroutines:

```kotlin
// All suspension functions are main-safe
viewModelScope.launch { // Main thread
    val photo = controller.capturePhoto() // Suspends, runs on proper thread
    updateUI(photo) // Back on main thread
}
```

## Resource Management

Proper cleanup is important:

```kotlin
@Composable
fun CameraScreen() {
    val cameraState = rememberCameraKState()
    
    // Automatically cleans up on disposal
    DisposableEffect(Unit) {
        onDispose {
            cameraState.controller.stopPreview()
            cameraState.controller.release()
        }
    }
}
```

## Platform Differences

| Feature | Android | iOS | Desktop |
|---------|---------|-----|---------|
| Camera API | CameraX | AVFoundation | OpenCV/JavaCV |
| Threading | Camera Executor | Dispatch Queue | Thread Pool |
| Permissions | Runtime Manifest | Info.plist | System Dialog |
| Preview | TextureView | CALayer | Canvas |

## Key Enums

### CameraSelector
```kotlin
enum class CameraSelector {
    BACK,   // Main/rear camera
    FRONT   // Front/selfie camera
}
```

### CameraFlash
```kotlin
enum class CameraFlash {
    ON,     // Always on
    OFF,    // Always off
    AUTO    // Automatic based on lighting
}
```

### ImageFormat
```kotlin
enum class ImageFormat {
    JPEG,   // JPEG format (compressed)
    PNG,    // PNG format (lossless)
    WEBP    // WebP format (efficient)
}
```

## Best Practices

1. **Initialize Early** – Set up CameraController before showing preview
2. **Use State Management** – Leverage Compose state for reactive updates
3. **Handle Permissions** – Always check permission status
4. **Manage Resources** – Properly release camera in `DisposableEffect`
5. **Error Handling** – Always handle `CameraResult.Error` cases
6. **Performance** – Use appropriate resolutions for target device

## Next Steps

- [Camera Capture Guide](camera-capture.md)
- [Permission Handling](permissions.md)
- [API Reference](../api/camera-controller.md)
