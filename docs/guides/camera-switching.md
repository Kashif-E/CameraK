# Camera Switching

Switch between front and back cameras.

## Toggle Camera Lens

Switch between front and back cameras:

```kotlin
controller.toggleCameraLens()  // BACK ↔ FRONT
```

## Set Specific Camera

```kotlin
controller.setCameraLens(CameraLens.FRONT)  // Front camera
controller.setCameraLens(CameraLens.BACK)   // Back camera
```

## Get Current Camera

```kotlin
val currentLens = controller.getCameraLens()
when (currentLens) {
    CameraLens.FRONT -> println("Using front camera")
    CameraLens.BACK -> println("Using back camera")
    null -> println("Camera not initialized")
}
```

## UI Example

```kotlin
@Composable
fun CameraWithSwitching() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Camera switch button
                IconButton(
                    onClick = { controller.toggleCameraLens() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
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

## Configuration: Set Initial Camera

```kotlin
val stateHolder = rememberCameraKState(
    permissions = permissions,
    cameraConfiguration = {
        setCameraLens(CameraLens.FRONT)  // Start with selfie camera
    }
)
```

## Smooth Camera Switch

Add loading indicator during switch:

```kotlin
@Composable
fun CameraWithSmoothSwitch() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    var isSwitching by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Switch button
                IconButton(
                    onClick = {
                        scope.launch {
                            isSwitching = true
                            delay(100)  // Brief delay for smooth animation
                            controller.toggleCameraLens()
                            delay(300)  // Wait for camera switch
                            isSwitching = false
                        }
                    },
                    enabled = !isSwitching,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    if (isSwitching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White
                        )
                    }
                }
            }
            
            is CameraKState.Error -> Text("Camera error")
            CameraKState.Initializing -> CircularProgressIndicator()
        }
    }
}
```

## Platform-Specific Behavior

### Android
- Seamless switching via CameraX
- Preview may pause briefly (<100ms)
- Flash settings reset per camera

### iOS
- Swift AVFoundation switch
- Preview updates smoothly
- Flash/torch settings persist per camera

### Desktop
- Requires multiple webcams
- Switching cycles through available devices
- Single webcam: no effect

## Preserve Settings After Switch

Save and restore settings:

```kotlin
fun switchCameraPreserveSettings(controller: CameraController) {
    // Save current settings
    val savedFlashMode = controller.getFlashMode()
    val savedZoom = controller.getZoom()
    
    // Switch camera
    controller.toggleCameraLens()
    
    // Wait for switch to complete
    scope.launch {
        delay(200)
        
        // Restore settings (if supported on new camera)
        savedFlashMode?.let { controller.setFlashMode(it) }
        controller.setZoom(savedZoom.coerceIn(1f, controller.getMaxZoom()))
    }
}
```

## Disable Flash When Switching to Front

Most front cameras don't have flash:

```kotlin
fun switchToFront(controller: CameraController) {
    controller.setCameraLens(CameraLens.FRONT)
    controller.setFlashMode(FlashMode.OFF)  // Disable flash
}

fun switchToBack(controller: CameraController) {
    controller.setCameraLens(CameraLens.BACK)
    controller.setFlashMode(FlashMode.AUTO)  // Re-enable flash
}
```

## Animated Camera Icon

Show which camera is active:

```kotlin
@Composable
fun AnimatedCameraSwitch() {
    val permissions = providePermissions()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    var isFrontCamera by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFrontCamera) 180f else 0f,
        animationSpec = tween(durationMillis = 300)
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                
                IconButton(
                    onClick = {
                        controller.toggleCameraLens()
                        isFrontCamera = !isFrontCamera
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.graphicsLayer { rotationY = rotation }
                    )
                }
            }
            
            is CameraKState.Error -> Text("Camera error")
            CameraKState.Initializing -> CircularProgressIndicator()
        }
    }
}
```

## Common Issues

### Preview Freezes After Switch

**Cause:** Camera needs time to reinitialize.

**Solution:** Add small delay after switch:

```kotlin
controller.toggleCameraLens()
delay(300)
// Camera ready
```

### Flash Doesn't Work After Switching

**Cause:** Front cameras usually lack flash.

**Solution:** Disable flash UI for front camera:

```kotlin
val currentLens = controller.getCameraLens()
val showFlashButton = currentLens == CameraLens.BACK
```

### Settings Don't Persist

**Cause:** Each camera has independent settings.

**Solution:** Store settings externally and reapply after switch.

## Best Practices

1. **Disable button during switch** — Prevent multiple rapid switches
2. **Show loading indicator** — Provide visual feedback
3. **Reset zoom** — Front camera may have different zoom range
4. **Disable flash for front camera** — Avoid confusion
5. **Test on real devices** — Emulators have limited camera support

## Next Steps

- [Flash and Torch](flash-and-torch.md) — Control lighting per camera
- [Zoom Control](zoom-control.md) — Handle different zoom ranges
- [Configuration](../getting-started/configuration.md) — Set initial camera
