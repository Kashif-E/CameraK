# CameraK App

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
implementation("io.github.kashif-mehmood-km:camerak:0.0.4")

```

## Usage

This is a simple example of how to use the CameraK library in your app.

```Kotlin
 LaunchedEffect(Unit) {
        if (controller.isPermissionGranted()) {
            controller.bindCamera()
        } else {
            requestCameraPermission({}, {})
            requestStoragePermission({}, {})
        }
    }


    Box {

        CameraKPreview(
            modifier = Modifier.fillMaxSize(),
            cameraController = controller
        )
        Row(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart)) {
            Row {
                // Button to toggle flash
                Button(onClick = { controller.toggleFlashMode() }) {
                    Text("Toggle Flash")
                }
                // Button to toggle camera lens
                Button(onClick = { controller.toggleCameraLens() }) {
                    Text("Toggle Camera Lens")
                }
            }

        }
        Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
            Row {

                Button(onClick = {
                    scope.launch {
                        val imageResult = controller.takePicture(ImageFormat.JPEG)

                        when (imageResult) {
                            is ImageCaptureResult.Success -> {
                                imageBitmap = imageResult.image.decodeToImageBitmap()
                                controller.savePicture(imageResult.path, imageResult.image, Directory.PICTURES)
                            }

                            is ImageCaptureResult.Error -> {
                               println("Error: ${imageResult.exception.message}")
                            }
                        }
                    }
                }) {
                    Text("Toggle Flash")
                }
                // Button to toggle camera lens
                Button(onClick = { controller.toggleCameraLens() }) {
                    Text("Toggle Camera Lens")
                }
            }

            imageBitmap?.let {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = "Captured Image",
                    modifier = Modifier.size(50.dp)
                )
            }


        }
    }

}
```
You can check the sample for more details.

## License

```
MIT License
```