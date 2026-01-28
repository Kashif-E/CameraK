# iOS Examples

## Basic Camera App (iOS)

```kotlin
@Composable
fun IOSCameraScreen() {
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
            }
        ) {
            Text("📸 Capture")
        }
    }
}
```

## Info.plist Configuration

Add to your iOS app's Info.plist:

```xml
<key>NSCameraUsageDescription</key>
<string>We need camera access to capture photos</string>
<key>NSMicrophoneUsageDescription</key>
<string>We need microphone access for videos</string>
```

## Video Recording

```kotlin
@Composable
fun IOSVideoRecorder() {
    val cameraState = rememberCameraKState()
    var isRecording by remember { mutableStateOf(false) }
    
    Column(Modifier.fillMaxSize()) {
        CameraPreviewComposable(cameraState.controller)
        
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
            Text(if (isRecording) "🛑 Stop" else "🎥 Record")
        }
    }
}
```
