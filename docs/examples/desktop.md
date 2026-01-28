# Desktop Examples

## Desktop Camera App

```kotlin
@Composable
fun DesktopCameraScreen() {
    val cameraState = rememberCameraKState()
    var capturedImage by remember { mutableStateOf<Uri?>(null) }
    
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Desktop Camera",
            style = MaterialTheme.typography.headlineMedium
        )
        
        CameraPreviewComposable(
            controller = cameraState.controller,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                viewModelScope.launch {
                    capturedImage = cameraState.controller.capturePhoto().uri
                }
            }) {
                Text("Capture Photo")
            }
        }
        
        if (capturedImage != null) {
            Text("Saved: $capturedImage")
        }
    }
}
```

## Multiple Camera Selection

```kotlin
@Composable
fun MultiCameraDesktop() {
    var selectedCamera by remember { mutableStateOf(0) }
    val cameraState = rememberCameraKState()
    
    Column {
        // Camera selector
        Dropdown(
            items = (0..3).map { "Camera $it" },
            onSelect = { cameraState.controller.setCameraIndex(it) }
        )
        
        CameraPreviewComposable(
            controller = cameraState.controller,
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

## With Controls

```kotlin
@Composable
fun DesktopCameraWithControls() {
    val cameraState = rememberCameraKState()
    var zoom by remember { mutableStateOf(1.0f) }
    
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Zoom Control
        Slider(
            value = zoom,
            onValueChange = { 
                zoom = it
                cameraState.controller.setZoom(it)
            },
            valueRange = 1.0f..3.0f,
            modifier = Modifier.fillMaxWidth()
        )
        
        CameraPreviewComposable(
            controller = cameraState.controller,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        
        Button(
            onClick = {
                viewModelScope.launch {
                    cameraState.controller.capturePhoto()
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("📸 Capture")
        }
    }
}
```
