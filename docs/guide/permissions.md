# Permission Handling

Managing camera permissions across platforms.

## Permission Overview

CameraK requires permissions to access device cameras:

| Permission | Purpose | Platforms |
|-----------|---------|-----------|
| `CAMERA` | Access camera device | Android, iOS, Desktop |
| `RECORD_AUDIO` | Record audio with video | Android, iOS, Desktop |
| `WRITE_EXTERNAL_STORAGE` | Save photos/videos | Android |
| `READ_EXTERNAL_STORAGE` | Read gallery photos | Android |

## Android Permissions

### Manifest Declaration

Add to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### Runtime Permission Requests

CameraK automatically handles runtime permissions with Compose:

```kotlin
@Composable
fun CameraPermissionScreen() {
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    
    when {
        cameraPermissionState.status.isGranted -> {
            // Permission granted
            CameraScreen()
        }
        cameraPermissionState.status.shouldShowRationale -> {
            // Show explanation
            Column {
                Text("Camera permission is required for this feature")
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        }
        else -> {
            // Permission not requested yet
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }
}
```

### Multiple Permissions

```kotlin
@Composable
fun MultiplePermissionsScreen() {
    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    )
    
    when {
        multiplePermissionsState.allPermissionsGranted -> {
            CameraScreen()
        }
        multiplePermissionsState.shouldShowRationale -> {
            Text("Camera and audio permissions are required")
            Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                Text("Grant All Permissions")
            }
        }
        else -> {
            LaunchedEffect(Unit) {
                multiplePermissionsState.launchMultiplePermissionRequest()
            }
        }
    }
}
```

## iOS Permissions

### Info.plist Configuration

Add to your `Info.plist`:

```xml
<dict>
    <key>NSCameraUsageDescription</key>
    <string>We need camera access to capture photos and videos for your app</string>
    
    <key>NSMicrophoneUsageDescription</key>
    <string>We need microphone access to record audio with videos</string>
    
    <key>NSPhotoLibraryUsageDescription</key>
    <string>We need access to your photo library to save photos</string>
    
    <key>NSPhotoLibraryAddUsageDescription</key>
    <string>We need access to save photos to your library</string>
</dict>
```

### Runtime Permission Handling

```kotlin
// iOS permissions are requested automatically when needed
@Composable
fun IosCamera() {
    val cameraState = rememberCameraKState()
    
    when (cameraState.permissionState) {
        PermissionState.GRANTED -> {
            CameraPreviewComposable(cameraState.controller)
        }
        PermissionState.DENIED -> {
            Text("Camera permission denied. Please enable in Settings.")
        }
        PermissionState.PENDING -> {
            CircularProgressIndicator()
        }
    }
}
```

## Desktop Permissions

Desktop platforms typically don't require runtime permissions but may show system dialogs:

```kotlin
@Composable
fun DesktopCamera() {
    val cameraState = rememberCameraKState()
    
    LaunchedEffect(Unit) {
        try {
            cameraState.controller.startPreview()
        } catch (e: PermissionDeniedException) {
            println("User denied camera access in system dialog")
        }
    }
}
```

## Permission State Management

### Check Permission Status

```kotlin
@Composable
fun PermissionAwareCamera() {
    val permissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val cameraState = rememberCameraKState()
    
    Column {
        when {
            permissionState.status.isGranted -> {
                CameraPreviewComposable(cameraState.controller)
            }
            permissionState.status.shouldShowRationale -> {
                RationalDialog(
                    onConfirm = { permissionState.launchPermissionRequest() }
                )
            }
            else -> {
                EmptyState(
                    message = "Camera permission required",
                    action = "Request Permission",
                    onClick = { permissionState.launchPermissionRequest() }
                )
            }
        }
    }
}
```

### Store Permission Preferences

```kotlin
@Composable
fun RememberPermissionPreference() {
    var hasAskedForPermission by rememberSaveable { mutableStateOf(false) }
    val permissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    if (!hasAskedForPermission && !permissionState.status.isGranted) {
        LaunchedEffect(Unit) {
            permissionState.launchPermissionRequest()
            hasAskedForPermission = true
        }
    }
}
```

## Scoped Storage (Android 11+)

Modern Android requires scoped storage:

```kotlin
// Save to app-specific directory (no permission needed)
val file = File(context.getExternalFilesDir(null), "photo.jpg")

// Save to Pictures with MediaStore (for Android 11+)
val contentValues = ContentValues().apply {
    put(MediaStore.MediaColumns.DISPLAY_NAME, "photo.jpg")
    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
}

val uri = context.contentResolver.insert(
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    contentValues
)
```

## Error Handling

```kotlin
try {
    val photo = controller.capturePhoto()
} catch (e: PermissionDeniedException) {
    // Handle permission denial
    showPermissionDenialDialog()
} catch (e: CameraNotAvailableException) {
    // Handle camera not available
    showCameraNotAvailableDialog()
} catch (e: Exception) {
    // Handle other errors
    showErrorDialog(e.message ?: "Unknown error")
}
```

## Best Practices

1. **Request Early** – Request permissions when first opening camera
2. **Explain Rationale** – Show why permissions are needed
3. **Graceful Fallback** – Provide UI when permissions denied
4. **Don't Over-request** – Only request needed permissions
5. **Honor User Choice** – Respect permission denials
6. **Test on Device** – Simulator may not reflect actual behavior

## Troubleshooting

### Permission Stuck in Denied State

Clear app data and reinstall:

```bash
adb shell pm clear com.yourapp.package
adb install app-debug.apk
```

### Camera Still Not Working

Check logcat:

```bash
adb logcat | grep -i camera
adb logcat | grep -i permission
```

### iOS Permission Dialog Not Showing

Ensure Info.plist keys are correct and app has been reinstalled after changes.

## Next Steps

- [Camera Capture](camera-capture.md)
- [Advanced Usage](advanced.md)
- [Troubleshooting](../troubleshooting.md)
