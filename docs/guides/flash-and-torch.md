# Flash and Torch Control

Control camera flash for photos and torch for continuous light.

## Flash Modes

Flash fires when capturing photos.

### Set Flash Mode

```kotlin
controller.setFlashMode(FlashMode.ON)
```

**Options:**
- `FlashMode.OFF` — Flash disabled
- `FlashMode.ON` — Flash always fires
- `FlashMode.AUTO` — Flash fires in low light

### Toggle Flash Mode

Cycle through modes: OFF → ON → AUTO → OFF

```kotlin
controller.toggleFlashMode()
```

### Get Current Mode

```kotlin
val currentMode = controller.getFlashMode()
when (currentMode) {
    FlashMode.ON -> println("Flash is enabled")
    FlashMode.OFF -> println("Flash is disabled")
    FlashMode.AUTO -> println("Flash is automatic")
    null -> println("Flash not available")
}
```

## Flash UI Example

```kotlin
@Composable
fun CameraWithFlash() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Flash toggle button
                IconButton(
                    onClick = {
                        controller.toggleFlashMode()
                        flashMode = controller.getFlashMode() ?: FlashMode.OFF
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            FlashMode.ON -> Icons.Default.FlashOn
                            FlashMode.OFF -> Icons.Default.FlashOff
                            FlashMode.AUTO -> Icons.Default.FlashAuto
                        },
                        contentDescription = "Flash: $flashMode",
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

## Torch Mode

Torch provides continuous light (flashlight mode).

### Toggle Torch

```kotlin
controller.toggleTorchMode()  // ON ↔ OFF
```

### Set Torch Mode

```kotlin
controller.setTorchMode(TorchMode.ON)
```

**Options:**
- `TorchMode.ON` — Torch enabled
- `TorchMode.OFF` — Torch disabled

### Get Torch State

```kotlin
val torchState = controller.getTorchMode()
when (torchState) {
    TorchMode.ON -> println("Torch is on")
    TorchMode.OFF -> println("Torch is off")
    null -> println("Torch not available")
}
```

## Torch UI Example

```kotlin
@Composable
fun CameraWithTorch() {
    val permissions = providePermissions()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    var isTorchOn by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Torch toggle
                IconButton(
                    onClick = {
                        controller.toggleTorchMode()
                        isTorchOn = controller.getTorchMode() == TorchMode.ON
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(
                            if (isTorchOn) Color.Yellow.copy(alpha = 0.7f)
                            else Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashlightOn 
                                      else Icons.Default.FlashlightOff,
                        contentDescription = if (isTorchOn) "Turn off torch" else "Turn on torch",
                        tint = if (isTorchOn) Color.Black else Color.White
                    )
                }
            }
            
            is CameraKState.Error -> Text("Camera error")
            CameraKState.Initializing -> CircularProgressIndicator()
        }
    }
}
```

## Flash vs Torch

| Feature | Flash | Torch |
|---------|-------|-------|
| **Use case** | Photo capture | Continuous light |
| **Activation** | Fires during capture | Manual toggle |
| **Duration** | Brief burst | Stays on until disabled |
| **Battery impact** | Minimal | High (use sparingly) |
| **API** | `setFlashMode()` | `setTorchMode()` |

**Best practices:**
- Use **flash** for taking photos
- Use **torch** for video recording or low-light navigation
- Turn off torch when not needed to save battery

## Combined Flash and Torch UI

```kotlin
@Composable
fun CameraWithLighting() {
    val permissions = providePermissions()
    val scope = rememberCoroutineScope()
    val stateHolder = rememberCameraKState(permissions = permissions)
    val cameraState by stateHolder.cameraState.collectAsStateWithLifecycle()
    var flashMode by remember { mutableStateOf(FlashMode.AUTO) }
    var isTorchOn by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraState) {
            is CameraKState.Ready -> {
                val controller = (cameraState as CameraKState.Ready).controller
                
                CameraPreviewComposable(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Controls at top
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Flash mode
                    IconButton(onClick = {
                        controller.toggleFlashMode()
                        flashMode = controller.getFlashMode() ?: FlashMode.AUTO
                    }) {
                        Icon(
                            imageVector = when (flashMode) {
                                FlashMode.ON -> Icons.Default.FlashOn
                                FlashMode.OFF -> Icons.Default.FlashOff
                                FlashMode.AUTO -> Icons.Default.FlashAuto
                            },
                            contentDescription = "Flash: $flashMode",
                            tint = Color.White
                        )
                    }
                    
                    // Torch toggle
                    IconButton(onClick = {
                        controller.toggleTorchMode()
                        isTorchOn = controller.getTorchMode() == TorchMode.ON
                    }) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashlightOn
                                          else Icons.Default.FlashlightOff,
                            contentDescription = "Torch",
                            tint = if (isTorchOn) Color.Yellow else Color.White
                        )
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

## Configuration: Set Default Flash Mode

Set initial flash mode during setup:

```kotlin
val stateHolder = rememberCameraKState(
    permissions = permissions,
    cameraConfiguration = {
        setFlashMode(FlashMode.OFF)  // Start with flash disabled
    }
)
```

## Platform Availability

### Android
- ✅ Flash: Available on rear cameras
- ✅ Torch: Available on rear cameras
- ❌ Front camera: Usually no flash/torch

### iOS
- ✅ Flash: Available on rear cameras
- ✅ Torch: Available on rear cameras  
- ❌ Front camera: iPhone 12+ has "Retina Flash" (screen flash)

### Desktop
- ❌ Flash: Not available
- ❌ Torch: Not available

**Check availability:**

```kotlin
val flashMode = controller.getFlashMode()
if (flashMode != null) {
    // Flash is available
} else {
    // No flash on this camera
}
```

## Common Issues

### Flash Not Working

**Cause:** Front camera selected (no flash hardware).

**Solution:** Switch to rear camera:

```kotlin
controller.setCameraLens(CameraLens.BACK)
```

### Torch Stays On After Closing App

**Cause:** Torch not disabled in cleanup.

**Solution:** Disable torch when leaving camera:

```kotlin
DisposableEffect(Unit) {
    onDispose {
        controller.setTorchMode(TorchMode.OFF)
        controller.cleanup()
    }
}
```

### Flash Mode Resets When Switching Cameras

**Cause:** Each camera has independent flash settings.

**Solution:** Re-apply flash mode after switching:

```kotlin
controller.toggleCameraLens()
controller.setFlashMode(FlashMode.ON)  // Re-apply
```

## Best Practices

1. **Save battery** — Turn off torch when not actively needed
2. **User feedback** — Show visual indicator when torch is on
3. **Cleanup** — Always disable torch in `onDispose` or app background
4. **Test on device** — Emulators don't have flash/torch
5. **Front camera** — Disable flash UI when front camera active

## Next Steps

- [Zoom Control](zoom-control.md) — Implement zoom functionality
- [Camera Switching](camera-switching.md) — Switch between cameras
- [Configuration](../getting-started/configuration.md) — Default flash settings
