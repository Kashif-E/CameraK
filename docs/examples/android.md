# Android Examples

## Basic Camera App

```kotlin
@Composable
fun AndroidCameraScreen() {
    val cameraState = rememberCameraKState()
    var capturedImage by remember { mutableStateOf<Uri?>(null) }
    
    Column(Modifier.fillMaxSize()) {
        CameraPreviewComposable(
            controller = cameraState.controller,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        
        Button(
            onClick = {
                viewModelScope.launch {
                    capturedImage = cameraState.controller.capturePhoto().uri
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Capture")
        }
    }
}
```

## With Permissions

```kotlin
@Composable
fun CameraWithPermissions() {
    val permissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    
    when {
        permissionState.status.isGranted -> {
            AndroidCameraScreen()
        }
        else -> {
            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }
        }
    }
}
```

## Video Recording

```kotlin
@Composable
fun VideoRecorderScreen() {
    val cameraState = rememberCameraKState()
    var isRecording by remember { mutableStateOf(false) }
    
    Column(Modifier.fillMaxSize()) {
        CameraPreviewComposable(
            controller = cameraState.controller,
            modifier = Modifier.weight(1f)
        )
        
        Button(
            onClick = {
                if (!isRecording) {
                    cameraState.controller.startVideoRecording()
                    isRecording = true
                } else {
                    viewModelScope.launch {
                        cameraState.controller.stopVideoRecording()
                        isRecording = false
                    }
                }
            }
        ) {
            Text(if (isRecording) "Stop Recording" else "Start Recording")
        }
    }
}
```
