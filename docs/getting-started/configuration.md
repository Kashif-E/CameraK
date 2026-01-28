# Configuration

Configure camera behavior, output format, and platform-specific features.

## Configuration DSL

Pass configuration during initialization:

```kotlin
val stateHolder = rememberCameraKState(
    permissions = permissions,
    cameraConfiguration = {
        // Your configuration here
    }
)
```

## Camera Selection

### Set Initial Camera Lens

```kotlin
cameraConfiguration = {
    setCameraLens(CameraLens.BACK)  // or CameraLens.FRONT
}
```

**Options:**
- `CameraLens.BACK` — Rear-facing camera (default)
- `CameraLens.FRONT` — Front-facing camera

**Switch at runtime:**

```kotlin
controller.toggleCameraLens()
// or
controller.setCameraLens(CameraLens.FRONT)
```

### iOS: Advanced Camera Types

On iOS devices with multiple cameras, select specific lenses:

```kotlin
cameraConfiguration = {
    setCameraDeviceType(CameraDeviceType.ULTRA_WIDE)
}
```

**Options:**
- `CameraDeviceType.DEFAULT` — Standard wide-angle (default)
- `CameraDeviceType.ULTRA_WIDE` — Ultra-wide lens (0.5x zoom)
- `CameraDeviceType.TELEPHOTO` — Telephoto lens (2x-3x zoom)
- `CameraDeviceType.WIDE_ANGLE` — Wide-angle lens

**Availability:** Depends on device hardware (iPhone 11 Pro+, iPhone 13+, etc.)

## Aspect Ratio

Set the capture aspect ratio:

```kotlin
cameraConfiguration = {
    setAspectRatio(AspectRatio.RATIO_16_9)
}
```

**Options:**

| Aspect Ratio | Use Case | Resolution Example |
|--------------|----------|-------------------|
| `RATIO_4_3` | Traditional photos | 2048×1536 |
| `RATIO_16_9` | Widescreen (default) | 1920×1080 |
| `RATIO_9_16` | Vertical stories | 1080×1920 |
| `RATIO_1_1` | Square photos | 1080×1080 |

**Example:**

```kotlin
// Instagram story mode
cameraConfiguration = {
    setAspectRatio(AspectRatio.RATIO_9_16)
}
```

## Resolution

Optionally set specific resolution:

```kotlin
cameraConfiguration = {
    setResolution(1920 to 1080)  // width × height
}
```

**Common resolutions:**
- `1920 to 1080` — Full HD
- `1280 to 720` — HD
- `3840 to 2160` — 4K (if supported)

**Note:** If device doesn't support exact resolution, closest match is used.

## Flash Mode

Control flash behavior:

```kotlin
cameraConfiguration = {
    setFlashMode(FlashMode.AUTO)
}
```

**Options:**
- `FlashMode.OFF` — Flash disabled
- `FlashMode.ON` — Flash always fires
- `FlashMode.AUTO` — Flash fires in low light (default)

**Runtime control:**

```kotlin
controller.setFlashMode(FlashMode.ON)
controller.toggleFlashMode()  // Cycles: OFF → ON → AUTO

val currentMode = controller.getFlashMode()  // returns FlashMode?
```

## Image Format

Set output format:

```kotlin
cameraConfiguration = {
    setImageFormat(ImageFormat.JPEG)
}
```

**Options:**

| Format | Compression | File Size | Use Case |
|--------|-------------|-----------|----------|
| `JPEG` | Lossy | Smaller | Web, sharing (default) |
| `PNG` | Lossless | Larger | Transparency, archival |

**Example:**

```kotlin
// High-quality archival photos
cameraConfiguration = {
    setImageFormat(ImageFormat.PNG)
}
```

## Save Directory

Choose where captured images are saved:

```kotlin
cameraConfiguration = {
    setDirectory(Directory.PICTURES)
}
```

**Options:**

| Directory | Android Path | iOS Path |
|-----------|--------------|----------|
| `PICTURES` | `Environment.DIRECTORY_PICTURES` | Photo Library (default) |
| `DCIM` | `Environment.DIRECTORY_DCIM` | Photo Library |
| `DOCUMENTS` | `getExternalFilesDir(DOCUMENTS)` | Documents folder |
| `DOWNLOADS` | `Environment.DIRECTORY_DOWNLOADS` | Downloads folder |
| `CACHE` | `cacheDir` | Temporary cache |

**Example:**

```kotlin
// Save to downloads for easy access
cameraConfiguration = {
    setDirectory(Directory.DOWNLOADS)
}
```

## Quality Prioritization

Balance between capture speed and image quality:

```kotlin
cameraConfiguration = {
    setQualityPrioritization(QualityPrioritization.BALANCED)
}
```

**Options:**
- `QualityPrioritization.QUALITY` — Best quality, slower capture
- `QualityPrioritization.BALANCED` — Good balance (default)
- `QualityPrioritization.SPEED` — Fast capture, lower quality

**Use cases:**
- `QUALITY`: Professional photography, print media
- `BALANCED`: General-purpose apps
- `SPEED`: Burst mode, document scanning

## Complete Example

```kotlin
@Composable
fun ProfessionalCameraScreen() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    
    val stateHolder = rememberCameraKState(
        permissions = permissions,
        cameraConfiguration = {
            // Camera hardware
            setCameraLens(CameraLens.BACK)
            setCameraDeviceType(CameraDeviceType.TELEPHOTO)  // iOS only
            
            // Image properties
            setAspectRatio(AspectRatio.RATIO_4_3)
            setResolution(3840 to 2160)  // 4K
            setImageFormat(ImageFormat.PNG)
            
            // Quality settings
            setQualityPrioritization(QualityPrioritization.QUALITY)
            
            // Flash and storage
            setFlashMode(FlashMode.AUTO)
            setDirectory(Directory.PICTURES)
        }
    )
    
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            when (val result = controller.takePictureToFile()) {
                                is ImageCaptureResult.SuccessWithFile -> {
                                    println("Saved 4K PNG: ${result.filePath}")
                                }
                                is ImageCaptureResult.Error -> {
                                    println("Error: ${result.exception.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, "Capture")
                }
            }
            
            is CameraKState.Error -> {
                Text("Camera error")
            }
            
            CameraKState.Initializing -> {
                CircularProgressIndicator()
            }
        }
    }
}
```

## Runtime Configuration

Some settings can be changed after initialization:

### Zoom

```kotlin
val maxZoom = controller.getMaxZoom()  // e.g., 10.0
controller.setZoom(2.5f)  // 2.5x zoom
val currentZoom = controller.getZoom()  // returns 2.5
```

### Flash

```kotlin
controller.setFlashMode(FlashMode.ON)
controller.toggleFlashMode()  // OFF → ON → AUTO → OFF
```

### Torch (Flashlight)

```kotlin
controller.toggleTorchMode()  // Toggle continuous light
```

### Camera Lens

```kotlin
controller.setCameraLens(CameraLens.FRONT)
controller.toggleCameraLens()  // BACK ↔ FRONT
```

## Platform-Specific Notes

### Android

- CameraX handles resolution selection automatically if not specified
- Flash modes depend on device hardware
- Some devices don't support all aspect ratios

### iOS

- `CameraDeviceType` only works on devices with multiple cameras
- Resolution is constrained by selected `AVCaptureSessionPreset`
- Flash availability depends on camera type (front cameras often lack flash)

### Desktop

- Resolution depends on webcam capabilities
- Flash and torch are usually not available
- Camera switching depends on multiple webcam availability

## Configuration Reference

| Method | Parameter Type | Default | Platforms |
|--------|----------------|---------|-----------|
| `setCameraLens()` | `CameraLens` | `BACK` | All |
| `setCameraDeviceType()` | `CameraDeviceType` | `DEFAULT` | iOS only |
| `setAspectRatio()` | `AspectRatio` | `RATIO_16_9` | All |
| `setResolution()` | `Pair<Int, Int>` | Device default | All |
| `setFlashMode()` | `FlashMode` | `AUTO` | Android, iOS |
| `setImageFormat()` | `ImageFormat` | `JPEG` | All |
| `setDirectory()` | `Directory` | `PICTURES` | All |
| `setQualityPrioritization()` | `QualityPrioritization` | `BALANCED` | All |

## Next Steps

- [Camera Capture](../guides/camera-capture.md) — Capture photos and handle results
- [Zoom Control](../guides/zoom-control.md) — Implement pinch-to-zoom
- [Flash and Torch](../guides/flash-and-torch.md) — Control lighting
