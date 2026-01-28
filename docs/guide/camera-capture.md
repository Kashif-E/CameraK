# Camera Capture

Capture photos and videos with CameraK.

## Photo Capture

### Basic Photo Capture

```kotlin
@Composable
fun CapturePhotoScreen() {
    val cameraState = rememberCameraKState()
    var capturedPhoto by remember { mutableStateOf<Photo?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Preview
        CameraPreviewComposable(
            controller = cameraState.controller,
            modifier = Modifier.weight(1f)
        )
        
        // Capture button
        Button(
            onClick = {
                viewModelScope.launch {
                    try {
                        capturedPhoto = cameraState.controller.capturePhoto()
                        println("Photo saved: ${capturedPhoto?.uri}")
                    } catch (e: CameraException) {
                        println("Capture failed: ${e.message}")
                    }
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("📸 Capture Photo")
        }
    }
}
```

### Photo Properties

```kotlin
data class Photo(
    val uri: Uri,               // File location
    val width: Int,             // Image width
    val height: Int,            // Image height
    val rotation: Int,          // Rotation in degrees
    val timestamp: Long,        // Capture timestamp
    val flash: CameraFlash,     // Flash mode used
    val exposure: Float,        // Exposure value
    val zoom: Float             // Zoom ratio used
)
```

### Capture Options

```kotlin
// With metadata
val photo = controller.capturePhoto(
    metadata = PhotoMetadata(
        description = "Birthday photo",
        location = GeoLocation(latitude = 40.7128, longitude = -74.0060)
    )
)

// With effects
val photo = controller.capturePhotoWithEffect(
    effect = PhotoEffect.BEAUTY_FILTER
)

// RAW format (when supported)
val photo = controller.capturePhoto(
    format = ImageFormat.RAW
)
```

## Video Recording

### Start Recording

```kotlin
@Composable
fun VideoRecordingScreen() {
    val cameraState = rememberCameraKState()
    var isRecording by remember { mutableStateOf(false) }
    var recordedVideo by remember { mutableStateOf<Video?>(null) }
    
    Column {
        // Preview
        CameraPreviewComposable(cameraState.controller)
        
        // Record button
        Button(
            onClick = {
                if (!isRecording) {
                    cameraState.controller.startVideoRecording()
                    isRecording = true
                } else {
                    viewModelScope.launch {
                        recordedVideo = cameraState.controller.stopVideoRecording()
                        isRecording = false
                        println("Video saved: ${recordedVideo?.uri}")
                    }
                }
            }
        ) {
            Text(if (isRecording) "⏹ Stop" else "🎥 Record")
        }
    }
}
```

### Video Properties

```kotlin
data class Video(
    val uri: Uri,               // File location
    val duration: Long,         // Duration in milliseconds
    val width: Int,             // Video width
    val height: Int,            // Video height
    val fps: Int,               // Frames per second
    val bitrate: Int,           // Bitrate in bps
    val size: Long,             // File size in bytes
    val codec: VideoCodec,      // Video codec used
    val timestamp: Long         // Recording timestamp
)
```

### Video Configuration

```kotlin
val controller = CameraController.Builder()
    .setVideoResolution(1920, 1080)
    .setVideoFrameRate(30)
    .setVideoBitrate(8_000_000)  // 8 Mbps
    .setAudioEnabled(true)
    .setAudioBitrate(128_000)     // 128 kbps
    .build()
```

## Burst Capture

Capture multiple photos rapidly:

```kotlin
// Capture 10 photos in rapid succession
val photos = controller.captureBurst(
    count = 10,
    interval = 100  // 100ms between shots
)
```

## Time-Lapse

Create time-lapse sequences:

```kotlin
controller.startTimeLapse(
    interval = 1000,            // 1 second between frames
    duration = 3600000,         // 1 hour total
    outputResolution = (1920 to 1080),
    onProgress = { progress ->
        println("${progress}% complete")
    }
)
```

## Slow Motion Video

Record at high frame rate:

```kotlin
val controller = CameraController.Builder()
    .setVideoFrameRate(120)     // 120 FPS for 4x slow motion
    .setVideoResolution(1920, 1080)
    .build()

// Record high-speed video
controller.startVideoRecording()
```

## Effects & Filters

Apply effects to captures:

```kotlin
// Beauty mode
val beautyPhoto = controller.capturePhotoWithEffect(
    effect = PhotoEffect.BEAUTY_FILTER
)

// B&W filter
val bwPhoto = controller.capturePhotoWithEffect(
    effect = PhotoEffect.BLACK_AND_WHITE
)

// HDR mode
val hdrPhoto = controller.capturePhotoWithEffect(
    effect = PhotoEffect.HDR
)
```

## Focus & Metering

Control focus and exposure metering:

```kotlin
// Tap to focus
controller.tapToFocus(
    x = 0.5f,   // Center X (0.0-1.0)
    y = 0.5f    // Center Y (0.0-1.0)
)

// Manual focus
controller.setFocusDistance(distance = 0.5f)

// Metering area
controller.setMeteringArea(
    x = 0.5f,
    y = 0.5f,
    width = 0.2f,
    height = 0.2f
)
```

## Image Processing

Process captured images:

```kotlin
// Rotate image
val rotatedPhoto = photo.rotate(degrees = 90)

// Crop image
val croppedPhoto = photo.crop(
    x = 0,
    y = 0,
    width = 1000,
    height = 1000
)

// Resize image
val resizedPhoto = photo.resize(
    width = 800,
    height = 600
)

// Compress for sharing
val compressedPhoto = photo.compress(quality = 0.85f)
```

## Saving Captures

### Save to Gallery

```kotlin
viewModelScope.launch {
    val photo = controller.capturePhoto()
    photo.saveToGallery(context, album = "CameraK")
}
```

### Save to Custom Location

```kotlin
val photo = controller.capturePhoto()
val file = File(context.getExternalFilesDir(null), "photo_${System.currentTimeMillis()}.jpg")
photo.saveTo(file)
```

### Share Directly

```kotlin
val photo = controller.capturePhoto()
val shareIntent = Intent.createChooser(
    Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, photo.uri)
        type = "image/jpeg"
    },
    "Share Photo"
)
startActivity(shareIntent)
```

## Error Handling

```kotlin
try {
    val photo = controller.capturePhoto()
} catch (e: CameraException) {
    when (e) {
        is CameraNotAvailableException -> {
            println("Camera device not available")
        }
        is PermissionDeniedException -> {
            println("Camera permission denied")
        }
        is CameraTimeoutException -> {
            println("Capture timeout - retry")
        }
        else -> {
            println("Unexpected error: ${e.message}")
        }
    }
}
```

## Performance Tips

1. **Resolution** – Use 1920x1080 for balanced quality/performance
2. **Framerate** – 30 FPS is sufficient for most videos
3. **Format** – JPEG is faster than PNG
4. **Burst** – Limit to 10-20 photos to avoid memory issues
5. **Effects** – Apply effects judiciously on lower-end devices

## Best Practices

1. ✅ Always wrap capture in try-catch
2. ✅ Use `viewModelScope.launch` for coroutines
3. ✅ Show progress feedback for long captures
4. ✅ Handle out-of-storage gracefully
5. ✅ Release resources after captures
6. ✅ Test on real devices for actual performance

## Next Steps

- [Permission Handling](permissions.md)
- [Advanced Usage](advanced.md)
- [Examples](../examples/android.md)
