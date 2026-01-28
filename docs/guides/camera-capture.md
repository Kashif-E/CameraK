# Camera Capture

Capture photos with type-safe result handling.

## Basic Capture

```kotlin
@Composable
fun CaptureExample() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    
    when (cameraState) {
        is CameraKState.Ready -> {
            val controller = (cameraState as CameraKState.Ready).controller
            
            Button(onClick = {
                scope.launch {
                    when (val result = controller.takePictureToFile()) {
                        is ImageCaptureResult.SuccessWithFile -> {
                            println("Saved to: ${result.filePath}")
                        }
                        is ImageCaptureResult.Error -> {
                            println("Error: ${result.exception.message}")
                        }
                    }
                }
            }) {
                Text("Capture Photo")
            }
        }
    }
}
```

## Recommended: `takePictureToFile()`

**Fast, efficient direct file capture** — saves directly to disk without memory overhead.

```kotlin
suspend fun takePictureToFile(): ImageCaptureResult
```

**Returns:**
- `ImageCaptureResult.SuccessWithFile(filePath: String)`
- `ImageCaptureResult.Error(exception: Exception)`

**Benefits:**
- ⚡ **2-3 seconds faster** than `takePicture()`
- 💾 **No ByteArray conversion** — direct disk write
- 🧠 **Lower memory usage** — no decode/encode cycles

**Example with error handling:**

```kotlin
scope.launch {
    try {
        when (val result = controller.takePictureToFile()) {
            is ImageCaptureResult.SuccessWithFile -> {
                val file = File(result.filePath)
                println("Photo saved: ${file.absolutePath}")
                println("File size: ${file.length()} bytes")
                
                // Load into image viewer
                loadImage(result.filePath)
            }
            is ImageCaptureResult.Error -> {
                showError("Capture failed: ${result.exception.message}")
            }
        }
    } catch (e: CancellationException) {
        println("Capture was cancelled")
        throw e  // Re-throw cancellation
    }
}
```

## Legacy: `takePicture()`

**Deprecated** — Returns image as `ByteArray`. Use `takePictureToFile()` instead.

```kotlin
@Deprecated("Use takePictureToFile() for better performance")
suspend fun takePicture(): ImageCaptureResult
```

**Returns:**
- `ImageCaptureResult.Success(byteArray: ByteArray)`
- `ImageCaptureResult.Error(exception: Exception)`

**Only use if:**
- You need to process the image in memory before saving
- You're uploading directly to a server without saving locally
- You're applying immediate image transformations

**Example:**

```kotlin
scope.launch {
    when (val result = controller.takePicture()) {
        is ImageCaptureResult.Success -> {
            val imageData = result.byteArray
            uploadToServer(imageData)
        }
        is ImageCaptureResult.Error -> {
            showError(result.exception.message)
        }
    }
}
```

## Result Types

Sealed class ensures exhaustive pattern matching:

```kotlin
sealed class ImageCaptureResult {
    data class SuccessWithFile(val filePath: String) : ImageCaptureResult()
    data class Success(val byteArray: ByteArray) : ImageCaptureResult()
    data class Error(val exception: Exception) : ImageCaptureResult()
}
```

**Handle all cases:**

```kotlin
when (result) {
    is ImageCaptureResult.SuccessWithFile -> {
        // File saved, path available
        val path = result.filePath
    }
    is ImageCaptureResult.Success -> {
        // ByteArray available (deprecated path)
        val data = result.byteArray
    }
    is ImageCaptureResult.Error -> {
        // Error occurred
        val error = result.exception
    }
}
```

## Capture with UI Feedback

Show loading state during capture:

```kotlin
@Composable
fun CaptureWithFeedback() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    var isCapturing by remember { mutableStateOf(false) }
    var lastCapturedPath by remember { mutableStateOf<String?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Capture button with loading
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            isCapturing = true
                            when (val result = controller.takePictureToFile()) {
                                is ImageCaptureResult.SuccessWithFile -> {
                                    lastCapturedPath = result.filePath
                                }
                                is ImageCaptureResult.Error -> {
                                    println("Error: ${result.exception.message}")
                                }
                            }
                            isCapturing = false
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.CameraAlt, "Capture")
                    }
                }
                
                // Show thumbnail of last capture
                lastCapturedPath?.let { path ->
                    AsyncImage(
                        model = path,
                        contentDescription = "Last capture",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
            
            is CameraKState.Error -> Text("Camera error")
            CameraKState.Initializing -> CircularProgressIndicator()
        }
    }
}
```

## Burst Capture

Capture multiple photos quickly:

```kotlin
fun captureBurst(controller: CameraController, count: Int) {
    val scope = CoroutineScope(Dispatchers.Default)
    
    scope.launch {
        val results = mutableListOf<String>()
        
        repeat(count) { index ->
            when (val result = controller.takePictureToFile()) {
                is ImageCaptureResult.SuccessWithFile -> {
                    results.add(result.filePath)
                    println("Captured ${index + 1}/$count")
                }
                is ImageCaptureResult.Error -> {
                    println("Burst failed at ${index + 1}: ${result.exception.message}")
                    return@launch
                }
            }
            
            // Small delay between captures
            delay(100)
        }
        
        println("Burst complete: ${results.size} photos")
    }
}
```

**Limitations:**
- Maximum 3 concurrent captures (queued automatically)
- Rapid captures may reduce quality on some devices
- Memory pressure increases with burst count

## Capture with Flash Control

Toggle flash before capture:

```kotlin
Button(onClick = {
    scope.launch {
        // Enable flash
        controller.setFlashMode(FlashMode.ON)
        
        // Capture with flash
        val result = controller.takePictureToFile()
        
        // Disable flash
        controller.setFlashMode(FlashMode.OFF)
        
        when (result) {
            is ImageCaptureResult.SuccessWithFile -> {
                println("Flash photo: ${result.filePath}")
            }
            is ImageCaptureResult.Error -> {
                println("Error: ${result.exception.message}")
            }
        }
    }
}) {
    Text("Capture with Flash")
}
```

## Save to Custom Location

By default, images save to the configured directory. To customize:

```kotlin
// Configure save directory
val stateHolder = rememberCameraKState(
    permissions = permissions,
    cameraConfiguration = {
        setDirectory(Directory.DOWNLOADS)  // Save to Downloads
    }
)
```

**Options:**
- `Directory.PICTURES` — User pictures (default)
- `Directory.DCIM` — Camera roll
- `Directory.DOWNLOADS` — Downloads folder
- `Directory.DOCUMENTS` — Documents
- `Directory.CACHE` — Temporary cache

**File naming:**
Files are automatically named with timestamp:
```
IMG_20240129_143022.jpg
```

## Error Handling

Handle common error scenarios:

```kotlin
scope.launch {
    when (val result = controller.takePictureToFile()) {
        is ImageCaptureResult.SuccessWithFile -> {
            handleSuccess(result.filePath)
        }
        is ImageCaptureResult.Error -> {
            when (result.exception) {
                is IOException -> {
                    // Storage full or permission issue
                    showError("Cannot save photo: storage issue")
                }
                is IllegalStateException -> {
                    // Camera not ready
                    showError("Camera not ready, try again")
                }
                is CancellationException -> {
                    // User cancelled
                    println("Capture cancelled")
                }
                else -> {
                    // Unknown error
                    showError("Capture failed: ${result.exception.message}")
                }
            }
        }
    }
}
```

## Common Issues

### "Camera not initialized"

**Cause:** Attempting capture before camera is ready.

**Solution:** Only capture when state is `Ready`:

```kotlin
when (cameraState) {
    is CameraKState.Ready -> {
        // Safe to capture
        controller.takePictureToFile()
    }
    else -> {
        println("Camera not ready")
    }
}
```

### "Storage permission denied" (Android)

**Cause:** Missing storage permission on Android < 10.

**Solution:** Add permission to manifest:

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

### "Burst queue full"

**Cause:** Too many concurrent capture requests.

**Solution:** Limit to 3 concurrent captures or wait for completion:

```kotlin
scope.launch {
    val result1 = controller.takePictureToFile()
    // Wait for first to complete before next
    val result2 = controller.takePictureToFile()
}
```

## Performance Tips

1. **Use `takePictureToFile()`** — 2-3x faster than `takePicture()`
2. **Lower resolution** — Set `setResolution(1920 to 1080)` for faster capture
3. **JPEG format** — Faster than PNG
4. **Quality prioritization** — Use `QualityPrioritization.SPEED` for rapid capture
5. **Avoid burst on low-end devices** — Limit to 3-5 photos

## Next Steps

- [Flash and Torch](flash-and-torch.md) — Control camera lighting
- [Zoom Control](zoom-control.md) — Implement zoom functionality
- [CameraController API](../api/controller.md) — Full API reference
