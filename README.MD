# CameraK App

[![Version](https://img.shields.io/github/v/release/kashif-e/camerak)](https://github.com/Kashif-E/CameraK/releases/tag/0.0.5)
## Overview

The CameraK App is a sample application demonstrating the use of the CameraX library in a Jetpack Compose environment. It provides functionalities to capture images, toggle flash mode, and switch between front and back cameras.

## Features

- **Camera Preview**: Displays a live camera feed using `CameraKPreview`.
- **Capture Image**: Allows users to capture images in JPEG format.
- **Toggle Flash**: Users can toggle the flash mode (on/off).
- **Switch Camera**: Users can switch between the front and back cameras.
- **Save Captured Images**: Captured images are saved to the device's storage.


## Installation

```Kotlin
implementation("io.github.kashif-mehmood-km:camerak:+")
```

## Usage

This is a simple example of how to use the CameraK library in your app.

### Permissions

Add the following permissions to your `AndroidManifest.xml` file:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```
Add the following to your info.plist file for iOS
```xml
<key>NSCameraUsageDescription</key>
<string>Camera permission is required for the app to work.</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>Photo Library permission is required for the app to work.</string>
```

### Compose Implementation
```Kotlin
@Composable
private fun CameraK(
    controller: CameraController,
    scope: CoroutineScope,
) {
    var imageBitmap: ImageBitmap? by remember { mutableStateOf(null) }

    val flashMode = remember(controller.getFlashMode()) {
        controller.getFlashMode() == FlashMode.ON
    }
    Box(modifier = Modifier.fillMaxSize()) {

        CameraKPreview(
            modifier = Modifier.fillMaxSize(),
            cameraController = controller
        )
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Switch(
                checked = flashMode,
                onCheckedChange = { controller.toggleFlashMode() }
            )

            Button(onClick = { controller.toggleCameraLens() }) {
                Text("Toggle Camera Lens")
            }
        }

        Button(onClick = {
            scope.launch {
                when (val result = controller.takePicture(ImageFormat.PNG)) {
                    is ImageCaptureResult.Success -> {
                        imageBitmap = result.image.decodeToImageBitmap()

                        controller.savePicture("image.png", result.image, Directory.PICTURES)
                    }

                    is ImageCaptureResult.Error -> {
                        println(result.exception.message ?: "Error")
                    }
                }
            }

        }, modifier = Modifier.clip(CircleShape).align(Alignment.BottomCenter).padding(16.dp)) {
            Text("Capture")
        }
    }
}
```
You can check the `Sample` for more details.

## License

```
MIT License
```