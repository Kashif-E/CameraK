# CameraKStateHolder API

Core state management for CameraK. Manages camera lifecycle, exposes reactive state, and handles plugin coordination.

## Overview

`CameraKStateHolder` is the primary interface for camera operations. It manages:
- Camera lifecycle (initialization → ready → cleanup)
- Reactive state via `StateFlow`
- Plugin lifecycle and coordination
- One-shot events via `SharedFlow`

## Creation

```kotlin
@Composable
fun rememberCameraKState(
    permissions: PermissionController,
    cameraConfiguration: CameraConfiguration.() -> Unit = {},
    plugins: List<CameraKPlugin> = emptyList(),
    onStateChange: (CameraKState) -> Unit = {}
): CameraKStateHolder
```

**Parameters:**
- `permissions` — Platform-specific permission controller from `providePermissions()`
- `cameraConfiguration` — DSL for camera configuration
- `plugins` — List of plugins to attach
- `onStateChange` — Callback for state changes (optional)

**Example:**

```kotlin
val stateHolder = rememberCameraKState(
    permissions = providePermissions(),
    cameraConfiguration = {
        setCameraLens(CameraLens.BACK)
        setFlashMode(FlashMode.AUTO)
        setAspectRatio(AspectRatio.RATIO_16_9)
    },
    plugins = listOf(
        rememberQRScannerPlugin(),
        rememberOcrPlugin()
    )
)
```

## Properties

### cameraState

```kotlin
val cameraState: StateFlow<CameraKState>
```

Observable camera lifecycle state. Emits:
- `CameraKState.Initializing` — Camera starting
- `CameraKState.Ready(controller)` — Camera operational
- `CameraKState.Error(exception)` — Initialization failed

**Usage:**

```kotlin
val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()

when (cameraState) {
    is CameraKState.Initializing -> CircularProgressIndicator()
    is CameraKState.Ready -> {
        val controller = (cameraState as CameraKState.Ready).controller
        CameraPreviewComposable(controller = controller)
    }
    is CameraKState.Error -> {
        val error = (cameraState as CameraKState.Error).exception
        Text("Error: ${error.message}")
    }
}
```

### uiState

```kotlin
val uiState: StateFlow<CameraUIState>
```

Observable UI properties (zoom level, flash mode, capturing state, etc.)

**CameraUIState structure:**

```kotlin
data class CameraUIState(
    val zoomLevel: Float = 1.0f,
    val maxZoom: Float = 1.0f,
    val flashMode: FlashMode = FlashMode.AUTO,
    val cameraLens: CameraLens = CameraLens.BACK,
    val isCapturing: Boolean = false,
    val lastError: String? = null
)
```

**Usage:**

```kotlin
val uiState by stateHolder.uiState.collectAsStateWithLifecycle()

Text("Zoom: ${uiState.zoomLevel}x")
Text("Max: ${uiState.maxZoom}x")
```

### events

```kotlin
val events: SharedFlow<CameraKEvent>
```

One-shot events (not persisted). Collect to handle events like capture success/failure.

**CameraKEvent types:**

```kotlin
sealed class CameraKEvent {
    data class ImageCaptured(val result: ImageCaptureResult) : CameraKEvent()
    data class CaptureFailed(val exception: Exception) : CameraKEvent()
    data class ZoomChanged(val zoom: Float) : CameraKEvent()
    data class FlashModeChanged(val mode: FlashMode) : CameraKEvent()
}
```

**Usage:**

```kotlin
LaunchedEffect(Unit) {
    stateHolder.events.collect { event ->
        when (event) {
            is CameraKEvent.ImageCaptured -> {
                showToast("Photo captured!")
            }
            is CameraKEvent.CaptureFailed -> {
                showToast("Capture failed: ${event.exception.message}")
            }
            else -> {}
        }
    }
}
```

### qrCodeFlow

```kotlin
val qrCodeFlow: StateFlow<List<String>>
```

Scanned QR codes. Populated automatically by QR Scanner Plugin.

**Usage:**

```kotlin
val qrCodes by stateHolder.qrCodeFlow.collectAsStateWithLifecycle(initial = emptyList())

if (qrCodes.isNotEmpty()) {
    Text("Latest QR: ${qrCodes.last()}")
}
```

### recognizedTextFlow

```kotlin
val recognizedTextFlow: StateFlow<String>
```

Recognized text via OCR. Populated automatically by OCR Plugin.

**Usage:**

```kotlin
val recognizedText by stateHolder.recognizedTextFlow.collectAsStateWithLifecycle(initial = "")

if (recognizedText.isNotEmpty()) {
    Text("OCR: $recognizedText")
}
```

### pluginScope

```kotlin
val pluginScope: CoroutineScope
```

CoroutineScope for plugin operations. Automatically cancelled when state holder is disposed.

## Methods

### getController()

```kotlin
fun getController(): CameraController?
```

Get camera controller if available (state is `Ready`).

**Returns:** `CameraController` or `null` if camera not ready.

**Example:**

```kotlin
val controller = stateHolder.getController()
if (controller != null) {
    controller.setZoom(2.0f)
} else {
    println("Camera not ready")
}
```

### getReadyCameraController()

```kotlin
suspend fun getReadyCameraController(): CameraController?
```

Suspends until camera is ready, then returns controller. Use in plugins.

**Returns:** `CameraController` or `null` if error occurs.

**Example:**

```kotlin
scope.launch {
    val controller = stateHolder.getReadyCameraController()
    controller?.setFlashMode(FlashMode.ON)
}
```

### captureImage()

```kotlin
fun captureImage()
```

Captures an image and emits `CameraKEvent.ImageCaptured` or `CameraKEvent.CaptureFailed`.

**Example:**

```kotlin
Button(onClick = { stateHolder.captureImage() }) {
    Text("Capture")
}

// Listen for result
LaunchedEffect(Unit) {
    stateHolder.events.collect { event ->
        when (event) {
            is CameraKEvent.ImageCaptured -> {
                when (val result = event.result) {
                    is ImageCaptureResult.SuccessWithFile -> {
                        println("Saved: ${result.filePath}")
                    }
                    is ImageCaptureResult.Error -> {
                        println("Error: ${result.exception.message}")
                    }
                }
            }
        }
    }
}
```

### setZoom()

```kotlin
fun setZoom(zoom: Float)
```

Sets zoom level. Updates `uiState.zoomLevel` and emits `CameraKEvent.ZoomChanged`.

**Parameters:**
- `zoom` — Zoom level (1.0 to maxZoom)

**Example:**

```kotlin
stateHolder.setZoom(2.5f)
```

### toggleCameraLens()

```kotlin
fun toggleCameraLens()
```

Switches between front and back cameras.

**Example:**

```kotlin
IconButton(onClick = { stateHolder.toggleCameraLens() }) {
    Icon(Icons.Default.Cameraswitch, "Switch Camera")
}
```

### initialize()

```kotlin
fun initialize()
```

Initializes camera. Called automatically by `rememberCameraKState`. Don't call manually.

### shutdown()

```kotlin
fun shutdown()
```

Cleans up resources. Called automatically when composable leaves composition.

## Complete Example

```kotlin
@Composable
fun CompleteStateHolderExample() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    
    val stateHolder = rememberCameraKState(
        permissions = permissions,
        cameraConfiguration = {
            setCameraLens(CameraLens.BACK)
            setFlashMode(FlashMode.AUTO)
            setAspectRatio(AspectRatio.RATIO_16_9)
        },
        plugins = listOf(rememberQRScannerPlugin())
    )
    
    // Observe state
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    val uiState by stateHolder.uiState.collectAsStateWithLifecycle()
    val qrCodes by stateHolder.qrCodeFlow.collectAsStateWithLifecycle(initial = emptyList())
    
    // Listen for events
    LaunchedEffect(Unit) {
        stateHolder.events.collect { event ->
            when (event) {
                is CameraKEvent.ImageCaptured -> {
                    println("Photo captured!")
                }
                is CameraKEvent.ZoomChanged -> {
                    println("Zoom: ${event.zoom}x")
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                
                // UI State display
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Text("Zoom: ${uiState.zoomLevel}x / ${uiState.maxZoom}x")
                    Text("Flash: ${uiState.flashMode}")
                    Text("Lens: ${uiState.cameraLens}")
                    if (qrCodes.isNotEmpty()) {
                        Text("QR: ${qrCodes.last()}")
                    }
                }
                
                // Capture button
                FloatingActionButton(
                    onClick = { stateHolder.captureImage() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                ) {
                    if (uiState.isCapturing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.CameraAlt, "Capture")
                    }
                }
                
                // Zoom slider
                Slider(
                    value = uiState.zoomLevel,
                    onValueChange = { stateHolder.setZoom(it) },
                    valueRange = 1f..uiState.maxZoom,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(200.dp)
                        .graphicsLayer { rotationZ = 270f }
                )
            }
            
            is CameraKState.Error -> {
                Text("Camera error: ${(cameraState as CameraKState.Error).exception.message}")
            }
            
            CameraKState.Initializing -> {
                CircularProgressIndicator()
            }
        }
    }
}
```

## Thread Safety

All public methods are thread-safe. State updates are synchronized internally.

## Lifecycle

1. **Creation** — `rememberCameraKState()` creates state holder
2. **Initialization** — Camera hardware initialized
3. **Ready** — Camera operational, plugins activated
4. **Cleanup** — Resources released when composable leaves composition

## See Also

- [CameraController API](controller.md) — Low-level camera operations
- [Configuration](../getting-started/configuration.md) — Configuration options
- [Camera Capture Guide](../guides/camera-capture.md) — Capture examples
