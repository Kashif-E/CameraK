# Quick Start

Get up and running with CameraK in 5 minutes.

## Basic Setup

### 1. Initialize CameraController

```kotlin
import dev.kashif.camerak.CameraController
import dev.kashif.camerak.state.rememberCameraKState

@Composable
fun CameraScreen() {
    // Initialize camera state
    val cameraState = rememberCameraKState()
    
    // Create camera controller
    val controller = CameraController.Builder()
        .setCameraSelector(CameraSelector.BACK)
        .setVideoResolution(1920, 1080)
        .build()
    
    LaunchedEffect(Unit) {
        controller.startPreview()
    }
}
```

### 2. Display Camera Preview

```kotlin
@Composable
fun CameraPreview(controller: CameraController) {
    CameraPreviewComposable(
        controller = controller,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    )
}
```

### 3. Capture Photo

```kotlin
@Composable
fun CaptureButton(controller: CameraController) {
    Button(
        onClick = {
            viewModelScope.launch {
                try {
                    val photo = controller.capturePhoto()
                    // Handle captured photo
                    println("Photo saved: ${photo.uri}")
                } catch (e: Exception) {
                    println("Capture failed: ${e.message}")
                }
            }
        }
    ) {
        Text("Capture Photo")
    }
}
```

## Complete Example

```kotlin
@Composable
fun SimpleCameraApp() {
    val cameraState = rememberCameraKState()
    var capturedImage by remember { mutableStateOf<Uri?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        CameraPreviewComposable(
            controller = cameraState.controller,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        
        // Capture Button
        Button(
            onClick = {
                viewModelScope.launch {
                    capturedImage = cameraState.controller.capturePhoto().uri
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        ) {
            Text("📸 Capture")
        }
    }
}
```

## Platform-Specific Notes

### Android
- Requires `android.permission.CAMERA` permission
- Runtime permission check automatically handled
- Compatible with CameraX 1.3+

### iOS
- Requires `NSCameraUsageDescription` in Info.plist
- Uses AVFoundation framework
- Supports iOS 14+

### Desktop
- Works on Windows, macOS, Linux
- Requires webcam/USB camera
- Uses JavaCV backend

## Common Tasks

### Request Permissions

```kotlin
// Android (handled automatically with Compose permissions library)
LaunchedEffect(Unit) {
    requestPermission(android.Manifest.permission.CAMERA)
}
```

### Switch Between Cameras

```kotlin
// Switch to front camera
controller.setCameraSelector(CameraSelector.FRONT)

// Switch to back camera
controller.setCameraSelector(CameraSelector.BACK)
```

### Record Video

```kotlin
// Start recording
controller.startVideoRecording()

// Stop recording
val videoFile = controller.stopVideoRecording()
```

### Adjust Camera Settings

```kotlin
controller.apply {
    setExposure(compensationValue = 0.5f)
    setZoom(zoomRatio = 2.0f)
    setFlash(CameraFlash.AUTO)
}
```

## Next Steps

- [Configuration Guide](configuration.md)
- [Core Concepts](../guide/core-concepts.md)
- [API Reference](../api/camera-controller.md)
- [Examples](../examples/android.md)

## Troubleshooting

**Camera not initializing?**
- Check permission declarations
- Ensure camera device is available
- See [Troubleshooting Guide](../troubleshooting.md)

**Preview not showing?**
- Verify `CameraPreviewComposable` is properly sized
- Check camera permissions are granted
- Enable debug logging in configuration

**Capture not working?**
- Ensure storage permissions are granted
- Check disk space availability
- Verify camera controller is initialized
