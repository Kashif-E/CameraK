# Zoom Control

Implement digital zoom with pinch gestures.

## Basic Zoom Control

```kotlin
// Set zoom level
controller.setZoom(2.0f)  // 2x zoom

// Get current zoom
val currentZoom = controller.getZoom()  // returns Float

// Get maximum zoom
val maxZoom = controller.getMaxZoom()  // e.g., 10.0
```

**Zoom range:** `1.0` (no zoom) to `maxZoom` (device-dependent, typically 4-10x)

## Pinch-to-Zoom Implementation

```kotlin
@Composable
fun CameraWithPinchZoom() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    var currentZoom by remember { mutableStateOf(1.0f) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                val maxZoom = remember { controller.getMaxZoom() }
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                // Calculate new zoom level
                                val newZoom = (currentZoom * zoom).coerceIn(1f, maxZoom)
                                currentZoom = newZoom
                                controller.setZoom(newZoom)
                            }
                        }
                )
                
                // Zoom indicator
                Text(
                    text = "${String.format("%.1f", currentZoom)}x",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 16.sp
                )
                
                // Capture button
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            controller.takePictureToFile()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, "Capture")
                }
            }
            
            is CameraKState.Error -> Text("Camera error")
            CameraKState.Initializing -> CircularProgressIndicator()
        }
    }
}
```

## Zoom Slider

Alternative to pinch gesture:

```kotlin
@Composable
fun CameraWithZoomSlider() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    var zoomLevel by remember { mutableStateOf(1.0f) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                val maxZoom = remember { controller.getMaxZoom() }
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Zoom slider
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", zoomLevel)}x",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = zoomLevel,
                        onValueChange = { newZoom ->
                            zoomLevel = newZoom
                            controller.setZoom(newZoom)
                        },
                        valueRange = 1f..maxZoom,
                        modifier = Modifier
                            .height(200.dp)
                            .graphicsLayer {
                                rotationZ = 270f  // Vertical slider
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                    )
                }
                
                // Capture button
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            controller.takePictureToFile()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, "Capture")
                }
            }
            
            is CameraKState.Error -> Text("Camera error")
            CameraKState.Initializing -> CircularProgressIndicator()
        }
    }
}
```

## Quick Zoom Buttons

Preset zoom levels:

```kotlin
@Composable
fun CameraWithQuickZoom() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    var currentZoom by remember { mutableStateOf(1.0f) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                val maxZoom = remember { controller.getMaxZoom() }
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Quick zoom buttons
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1.0f, 2.0f, 5.0f).forEach { zoom ->
                        if (zoom <= maxZoom) {
                            Button(
                                onClick = {
                                    currentZoom = zoom
                                    controller.setZoom(zoom)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentZoom == zoom) 
                                        Color.White else Color.Gray
                                )
                            ) {
                                Text(
                                    text = "${zoom.toInt()}x",
                                    color = if (currentZoom == zoom) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
                
                // Capture button
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            controller.takePictureToFile()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, "Capture")
                }
            }
            
            is CameraKState.Error -> Text("Camera error")
            CameraKState.Initializing -> CircularProgressIndicator()
        }
    }
}
```

## Smooth Zoom Animation

Animate zoom changes:

```kotlin
@Composable
fun CameraWithSmoothZoom() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    var targetZoom by remember { mutableStateOf(1.0f) }
    val animatedZoom by animateFloatAsState(
        targetValue = targetZoom,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                val maxZoom = remember { controller.getMaxZoom() }
                
                // Apply animated zoom
                LaunchedEffect(animatedZoom) {
                    controller.setZoom(animatedZoom)
                }
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                val newZoom = (targetZoom * zoom).coerceIn(1f, maxZoom)
                                targetZoom = newZoom
                            }
                        }
                )
                
                // Zoom indicator with animation
                Text(
                    text = "${String.format("%.1f", animatedZoom)}x",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White
                )
            }
            
            is CameraKState.Error -> Text("Camera error")
            CameraKState.Initializing -> CircularProgressIndicator()
        }
    }
}
```

## Double-Tap Zoom

Zoom in/out on double tap:

```kotlin
@Composable
fun CameraWithDoubleTapZoom() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    var isZoomedIn by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    isZoomedIn = !isZoomedIn
                                    val newZoom = if (isZoomedIn) 2.0f else 1.0f
                                    controller.setZoom(newZoom)
                                }
                            )
                        }
                )
                
                // Hint
                if (controller.getZoom() == 1.0f) {
                    Text(
                        text = "Double tap to zoom",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White
                    )
                }
            }
            
            is CameraKState.Error -> Text("Camera error")
            CameraKState.Initializing -> CircularProgressIndicator()
        }
    }
}
```

## Zoom Limits

```kotlin
val maxZoom = controller.getMaxZoom()

// Constrain zoom to valid range
fun setZoomSafely(zoom: Float) {
    val clampedZoom = zoom.coerceIn(1f, maxZoom)
    controller.setZoom(clampedZoom)
}
```

**Typical max zoom values:**
- Budget Android: 4x-8x
- Flagship Android: 10x-30x (digital + optical)
- iPhone: 5x-15x (varies by model)
- Desktop webcam: 1x-4x

## iOS: Optical vs Digital Zoom

On iOS, combine hardware camera switching with digital zoom:

```kotlin
// Configure for telephoto (2x optical)
val stateHolder = rememberCameraKState(
    permissions = permissions,
    cameraConfiguration = {
        setCameraDeviceType(CameraDeviceType.TELEPHOTO)
    }
)

// Then apply digital zoom on top
controller.setZoom(2.0f)  // Total 4x zoom (2x optical + 2x digital)
```

**iOS camera types:**
- `CameraDeviceType.ULTRA_WIDE` — 0.5x zoom
- `CameraDeviceType.WIDE_ANGLE` — 1x zoom (default)
- `CameraDeviceType.TELEPHOTO` — 2x-3x zoom

## Performance Tips

1. **Avoid rapid zoom changes** — Debounce gesture input
2. **Use integer zoom levels** — 1x, 2x, 5x perform better than 1.37x
3. **Reset zoom when switching cameras** — Front/back may have different limits
4. **Test on real devices** — Emulators have limited zoom support

## Common Issues

### Zoom Resets When Switching Cameras

**Cause:** Each camera has independent zoom state.

**Solution:** Store and reapply zoom after switch:

```kotlin
val savedZoom = controller.getZoom()
controller.toggleCameraLens()
delay(100)  // Wait for camera switch
controller.setZoom(savedZoom.coerceIn(1f, controller.getMaxZoom()))
```

### Zoom Doesn't Work on Front Camera

**Cause:** Some front cameras don't support zoom.

**Solution:** Check and disable zoom UI:

```kotlin
val maxZoom = controller.getMaxZoom()
if (maxZoom <= 1.0f) {
    // No zoom available, hide zoom controls
}
```

### Pinch Gesture Conflicts with Scrolling

**Cause:** Parent scrollable container intercepts gestures.

**Solution:** Use `Modifier.pointerInput` on camera preview only, not parent.

## Next Steps

- [Camera Switching](camera-switching.md) — Switch between front/back cameras
- [Flash and Torch](flash-and-torch.md) — Control lighting
- [Camera Capture](camera-capture.md) — Capture photos at current zoom
