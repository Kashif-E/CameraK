# CameraK Library

<p align="center">
  <a href="https://github.com/Kashif-E/CameraK/releases/tag/0.0.5">
    <img src="https://img.shields.io/github/v/release/kashif-e/camerak" alt="Version">
  </a>
  <a href="https://mailchi.mp/kotlinweekly/kotlin-weekly-425">
    <img src="https://img.shields.io/badge/Kotlin_Weekly-425-blue" alt="Kotlin Weekly #425">
  </a>
</p>

## Overview

The **CameraK Library** is a camera solution designed for Compose Multiplatform, currently
supporting both **Android** and **iOS**. While there are plans to expand support to additional
platforms, this will take time.

CameraK offers features such as **Camera Preview**, **Image Capture**, **Saving Images Locally**,
and **Exposing Images as ByteArrays**. It has a plugin-based API that allows developers to extend
and enhance its functionality. Currently, two plugins are available: one for saving images and
another for scanning QR codes.

***Like What you see?***

**Buy me a coffee**

<a href="https://www.buymeacoffee.com/kashifmehmood"><img src="https://img.buymeacoffee.com/button-api/?text=Buy me a coffee&emoji=&slug=kashifmehmood&button_colour=FFDD00&font_colour=000000&font_family=Cookie&outline_colour=000000&coffee_colour=ffffff" /></a>


## Installation

```Kotlin  
implementation("io.github.kashif-mehmood-km:camerak:+")  
```  

if you want to save images to device add the plugin:

```Kotlin
implementation("io.github.kashif-mehmood-km:image_saver_plugin:0.0.1")
````

if you want to add QR scanning capability then you need the QR scanner plugin:

```Kotlin
implementation("io.github.kashif-mehmood-km:qr_scanner_plugin:0.0.2")
```

## Supported Platforms

- Android
- IOS
- More will be added later

## Usage

This is a simple example of how to use the CameraK library in your app.

#### Permissions

Add the following permissions to your `AndroidManifest.xml` file:

```xml  

<uses-permission android:name="android.permission.CAMERA" /><uses-permission
android:name="android.permission.WRITE_EXTERNAL_STORAGE" />  
```  

Add the following to your info.plist file for iOS

```xml  

<key>NSCameraUsageDescription</key><string>Camera permission is required for the app to work.
</string><key>NSPhotoLibraryUsageDescription</key><string>Photo Library permission is required for
the app to work.
</string>  
```  

Checking for permission and asking if needed

```Kotlin
// Initialize Camera Permission State based on current permission status  
val cameraPermissionState = remember {
    mutableStateOf(
        permissions.hasCameraPermission()
    )
}

// Initialize Storage Permission State  
val storagePermissionState = remember {
    mutableStateOf(
        permissions.hasStoragePermission()
    )
}

if (!cameraPermissionState.value) {
    permissions.RequestStoragePermission(onGranted = { cameraPermissionState.value = true },
        onDenied = {
            println("Camera Permission Denied")
        })
}


if (!storagePermissionState.value) {
    permissions.RequestStoragePermission(onGranted = { storagePermissionState.value = true },
        onDenied = {
            println("Storage Permission Denied")
        })
}

// Initialize CameraController only when permissions are granted  
if (cameraPermissionState.value && storagePermissionState.value) {
}

```

If permissions are granted we first create a camera controller

```Kotlin
val cameraController = remember { mutableStateOf<CameraController?>(null) }
```

After this if needed, create plugins

```Kotlin
val imageSaverPlugin = createImageSaverPlugin(
    config = ImageSaverConfig(
        isAutoSave = false, // Set to true to enable automatic saving  
        prefix = "MyApp", // Prefix for image names when auto-saving  
        directory = Directory.PICTURES, // Directory to save images  
        customFolderName = "CustomFolder" // Custom folder name within the directory, only works on android for now  
    )
)
val qrScannerPlugin = createQRScannerPlugin(coroutineScope = coroutineScope)

LaunchedEffect(Unit) {
    qrScannerPlugin.getQrCodeFlow(500)
        .collectLatest { qrCode ->
            println("QR Code Detected flow: $qrCode")
            snackbarHostState.showSnackbar("QR Code Detected flow: $qrCode")
            qrScannerPlugin.pauseScanning()
        }
}
```

After this we can create a Camera Preview and pass camera configuration, the it will create a
`CameraController` and we can get it from the `onCameraControllerReady` callback. Once we get the
controller we can then show the camera screen.

```Kotlin
CameraPreview(modifier = Modifier.fillMaxSize(), cameraConfiguration = {
    setCameraLens(CameraLens.BACK)
    setFlashMode(FlashMode.OFF)
    setImageFormat(ImageFormat.JPEG)
    setDirectory(Directory.PICTURES)
    addPlugin(imageSaverPlugin)
    addPlugin(qrScannerPlugin)
}, onCameraControllerReady = {
    cameraController.value = it
    println("Camera Controller Ready ${cameraController.value}")
    qrScannerPlugin.startScanning()
})
cameraController.value?.let { controller ->
    CameraScreen(cameraController = controller, imageSaverPlugin)
}
```

Here is a sample camera screen that is in the `Sample` module

```Kotlin
@OptIn(ExperimentalResourceApi::class, ExperimentalUuidApi::class)
@Composable
fun CameraScreen(cameraController: CameraController, imageSaverPlugin: ImageSaverPlugin) {
    val scope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Flash Mode Switch  
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Flash")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = isFlashOn, onCheckedChange = {
                    isFlashOn = it
                    cameraController.toggleFlashMode()
                })
            }

            // Camera Lens Toggle Button  
            Button(onClick = { cameraController.toggleCameraLens() }) {
                Text(text = "Toggle Lens")
            }
        }
        // Capture Button at the Bottom Center  
        Button(
            onClick = {
                scope.launch {
                    when (val result = cameraController.takePicture()) {
                        is ImageCaptureResult.Success -> {

                            imageBitmap = result.byteArray.decodeToImageBitmap()
                            // If auto-save is disabled, manually save the image  
                            if (!imageSaverPlugin.config.isAutoSave) {
                                // Generate a custom name or use default  
                                val customName = "Manual_${Uuid.random().toHexString()}"

                                imageSaverPlugin.saveImage(
                                    byteArray = result.byteArray, imageName = customName
                                )
                            }
                        }

                        is ImageCaptureResult.Error -> {
                            println("Image Capture Error: ${result.exception.message}")
                        }
                    }
                }
            }, modifier = Modifier.size(70.dp).clip(CircleShape).align(Alignment.BottomCenter)

        ) {
            Text(text = "Capture")
        }

        // Display the captured image  
        imageBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Captured Image",
                modifier = Modifier.fillMaxSize().padding(16.dp)
            )

            LaunchedEffect(bitmap) {
                delay(3000)
                imageBitmap = null
            }
        }
    }
}
```

You can check the `Sample` for more details.

The library is in an experimental stage, APIs can change/break.

### Plugin API

CameraK has a plugin api which can be used by devs to enhance the capabilities of the library for their own needs, The design for it is not final but you can check `qrScannerPlugin` or `imageSaverPlugin` to check how you can build your own plugins.


**Contributions**

We welcome contributions! Before submitting a pull request, please open an issue so we can discuss
the proposed changes and collaborate on improving the project.

**Feature Requests**
Feature requests are encouraged, and I’ll do my best to address them as quickly as possible.

## License

```  
MIT License  
```
