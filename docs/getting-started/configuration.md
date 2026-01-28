# Configuration

Configure CameraK to suit your application's needs.

## Builder Pattern

CameraK uses the Builder pattern for flexible configuration:

```kotlin
// Platform-specific builder creation
val builder = when (platform) {
    Platform.Android -> createAndroidCameraControllerBuilder(context, lifecycleOwner)
    Platform.iOS -> createIOSCameraControllerBuilder()
    Platform.Desktop -> DesktopCameraControllerBuilder()
}

val controller = builder
    .setCameraLens(CameraLens.BACK)
    .setFlashMode(FlashMode.AUTO)
    .setImageFormat(ImageFormat.JPEG)
    .setDirectory(Directory.DCIM)
    .setAspectRatio(AspectRatio.RATIO_16_9)
    .setQualityPrioritization(QualityPrioritization.BALANCED)
    .build()
```

## Configuration Options

### Camera Selection

```kotlin
// Select camera lens
builder.setCameraLens(CameraLens.BACK)   // Back/main camera
builder.setCameraLens(CameraLens.FRONT)  // Front/selfie camera

// Preferred camera device type (hardware dependent)
builder.setPreferredCameraDeviceType(CameraDeviceType.WIDE_ANGLE)
builder.setPreferredCameraDeviceType(CameraDeviceType.TELEPHOTO)
builder.setPreferredCameraDeviceType(CameraDeviceType.ULTRA_WIDE)
```

### Resolution & Aspect Ratio

```kotlin
// Aspect ratio for preview and capture
builder.setAspectRatio(AspectRatio.RATIO_16_9)
builder.setAspectRatio(AspectRatio.RATIO_4_3)
builder.setAspectRatio(AspectRatio.RATIO_9_16)
builder.setAspectRatio(AspectRatio.RATIO_1_1)

// Target resolution (platform may use closest supported)
builder.setResolution(width = 3840, height = 2160)
```

### Flash Mode

```kotlin
builder.setFlashMode(FlashMode.ON)    // Always on
builder.setFlashMode(FlashMode.OFF)   // Always off
builder.setFlashMode(FlashMode.AUTO)  // Auto detect
```

### Torch Mode

```kotlin
builder.setTorchMode(TorchMode.ON)    // Continuous light on
builder.setTorchMode(TorchMode.OFF)   // Light off
builder.setTorchMode(TorchMode.AUTO)  // Auto (iOS only)
```

**Note:** On Android, `TorchMode.AUTO` is not natively supported by CameraX and will be treated as `ON`. iOS supports AUTO mode natively through AVFoundation.

### Image Format & Quality

```kotlin
// Output format
builder.setImageFormat(ImageFormat.JPEG)
builder.setImageFormat(ImageFormat.PNG)

// Quality prioritization
builder.setQualityPrioritization(QualityPrioritization.BALANCED)
builder.setQualityPrioritization(QualityPrioritization.SPEED)
builder.setQualityPrioritization(QualityPrioritization.QUALITY)
```

### Directory & File Output

```kotlin
// Save location
builder.setDirectory(Directory.DCIM)
builder.setDirectory(Directory.PICTURES)
builder.setDirectory(Directory.DOCUMENTS)

// Return type configuration
builder.setReturnFilePath(true)  // Return file path (faster)
builder.setReturnFilePath(false) // Return ByteArray (default)
```

### Runtime Control (Not Builder)

Zoom and other runtime controls are accessed through the `CameraController` instance after building:

```kotlin
// Get controller from Ready state
val controller = (cameraState as CameraKState.Ready).controller

// Zoom control (runtime only)
controller.setZoom(2.5f)
val currentZoom = controller.getZoom()
val maxZoom = controller.getMaxZoom()
```

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
