# Configuration

Configure CameraK to suit your application's needs.

## Builder Pattern

CameraK uses the Builder pattern for flexible configuration:

```kotlin
val controller = CameraController.Builder()
    .setCameraSelector(CameraSelector.BACK)
    .setVideoResolution(1920, 1080)
    .setPhotoResolution(4000, 3000)
    .setFlash(CameraFlash.AUTO)
    .setSceneMode(SceneMode.AUTO)
    .build()
```

## Configuration Options

### Camera Selection

```kotlin
// Select camera
builder.setCameraSelector(CameraSelector.BACK)  // Back/main camera
builder.setCameraSelector(CameraSelector.FRONT) // Front/selfie camera
```

### Resolution

```kotlin
// Photo resolution (default: device max)
builder.setPhotoResolution(width = 3840, height = 2160)

// Video resolution (default: 1920x1080)
builder.setVideoResolution(width = 1920, height = 1080)

// Aspect ratio (Android)
builder.setAspectRatio(AspectRatio.RATIO_16_9)
```

### Flash Control

```kotlin
builder.setFlash(CameraFlash.ON)      // Always on
builder.setFlash(CameraFlash.OFF)     // Always off
builder.setFlash(CameraFlash.AUTO)    // Auto detect
```

### Focus Mode

```kotlin
builder.setFocusMode(FocusMode.AUTO)     // Automatic focus
builder.setFocusMode(FocusMode.MANUAL)   // Manual focus control
builder.setFocusMode(FocusMode.CONTINUOUS) // Continuous autofocus
```

### Scene Modes

```kotlin
builder.setSceneMode(SceneMode.AUTO)       // Automatic
builder.setSceneMode(SceneMode.PORTRAIT)   // Portrait mode
builder.setSceneMode(SceneMode.LANDSCAPE)  // Landscape mode
builder.setSceneMode(SceneMode.NIGHT)      // Night mode
builder.setSceneMode(SceneMode.ACTION)     // Action/sports
builder.setSceneMode(SceneMode.DOCUMENT)   // Document scanning
```

### Image Format

```kotlin
// JPEG compression quality (0-100)
builder.setJpegQuality(quality = 95)

// Output format
builder.setImageFormat(ImageFormat.JPEG)
builder.setImageFormat(ImageFormat.PNG)
```

### Exposure & White Balance

```kotlin
// Exposure compensation (-2.0 to +2.0)
builder.setExposureCompensation(compensationValue = 0.5f)

// White balance mode
builder.setWhiteBalance(WhiteBalance.AUTO)
builder.setWhiteBalance(WhiteBalance.DAYLIGHT)
builder.setWhiteBalance(WhiteBalance.CLOUDY)
builder.setWhiteBalance(WhiteBalance.TUNGSTEN)
builder.setWhiteBalance(WhiteBalance.FLUORESCENT)
```

### Zoom

```kotlin
// Set zoom level (1.0 = no zoom)
builder.setZoom(zoomRatio = 2.0f)

// Get available zoom range
val minZoom = controller.getMinZoomRatio()
val maxZoom = controller.getMaxZoomRatio()
```

### Preview Settings

```kotlin
// Enable/disable preview scaling
builder.setScalePreview(scaleType = ScaleType.CENTER_CROP)
```

## Platform-Specific Configuration

### Android (CameraX)

```kotlin
// Additional Android-specific options
builder.setUseCasesCombination(useCases = listOf(UseCase.PREVIEW, UseCase.CAPTURE))
builder.setExtensionsMode(ExtensionsMode.AUTOMATIC)
```

### iOS (AVFoundation)

```kotlin
// iOS-specific session configuration
builder.setSessionPreset(SessionPreset.HIGH)
builder.setVideoStabilization(enabled = true)
```

### Desktop (JavaCV)

```kotlin
// Desktop-specific camera index
builder.setCameraIndex(cameraIndex = 0)
builder.setBackendType(BackendType.OPENCV)
```

## Runtime Configuration

Change settings at runtime:

```kotlin
// After initialization
controller.setFlash(CameraFlash.ON)
controller.setZoom(zoomRatio = 3.0f)
controller.setExposure(compensationValue = 1.0f)
controller.setWhiteBalance(WhiteBalance.DAYLIGHT)
```

## Configuration with Compose State

```kotlin
@Composable
fun ConfigurableCamera() {
    var flash by remember { mutableStateOf(CameraFlash.AUTO) }
    var zoom by remember { mutableStateOf(1.0f) }
    
    val controller = CameraController.Builder()
        .setFlash(flash)
        .setZoom(zoom)
        .build()
    
    Column {
        // Flash selector
        Dropdown(
            items = listOf(CameraFlash.ON, CameraFlash.OFF, CameraFlash.AUTO),
            onSelect = { flash = it }
        )
        
        // Zoom slider
        Slider(
            value = zoom,
            onValueChange = { zoom = it },
            valueRange = 1.0f..3.0f
        )
    }
}
```

## Performance Optimization

```kotlin
// For optimal performance:
builder.setVideoResolution(1920, 1080)     // Balanced quality
builder.setPhotoResolution(2048, 1536)     // Reasonable photo quality
builder.setJpegQuality(85)                 // Compress to reduce file size
```

## Best Practices

1. **Reuse CameraController** – Create once, use throughout app lifecycle
2. **Set Permissions Early** – Request permissions before initialization
3. **Handle Errors** – Wrap initialization in try-catch
4. **Test on Device** – Simulator may not support all features
5. **Monitor Performance** – Check memory usage with high resolutions

## Complete Configuration Example

```kotlin
val controller = CameraController.Builder()
    // Camera selection
    .setCameraSelector(CameraSelector.BACK)
    
    // Resolution
    .setPhotoResolution(3840, 2160)
    .setVideoResolution(1920, 1080)
    
    // Image quality
    .setJpegQuality(quality = 90)
    
    // Controls
    .setFlash(CameraFlash.AUTO)
    .setFocusMode(FocusMode.CONTINUOUS)
    .setSceneMode(SceneMode.AUTO)
    
    // Exposure
    .setExposureCompensation(compensationValue = 0.0f)
    .setWhiteBalance(WhiteBalance.AUTO)
    
    // Zoom
    .setZoom(zoomRatio = 1.0f)
    
    .build()
```

## Next Steps

- [Core Concepts](../guide/core-concepts.md)
- [API Reference](../api/camera-controller.md)
- [Advanced Usage](../guide/advanced.md)
