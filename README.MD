# CameraK

<p align="center">
  <a href="https://github.com/Kashif-E/CameraK/releases/tag/0.0.11">
    <img src="https://img.shields.io/github/v/release/kashif-e/camerak" alt="Version">
  </a>
  <a href="https://mailchi.mp/kotlinweekly/kotlin-weekly-425">
    <img src="https://img.shields.io/badge/Kotlin_Weekly-425-blue" alt="Kotlin Weekly #425">
  </a>
</p>

## A Modern Camera Solution for Compose Multiplatform

CameraK is a comprehensive camera library designed specifically for Compose Multiplatform applications. It currently supports Android, iOS, and JVM platforms, with plans for expansion.

### Key Features

- 📱 **Cross-Platform Support**: Works on Android, iOS, and JVM
- 📸 **Camera Preview & Image Capture**: High-quality preview and capture capabilities
- 💾 **Image Processing**: Save images locally or process them as ByteArrays
- 🧩 **Plugin Architecture**: Extend functionality through modular plugins
- 🔍 **QR Scanning**: Quick and accurate QR code detection with dedicated plugin
- 📝 **OCR Support**: Text recognition capabilities with OCR plugin

## Installation

Add the core library to your project:

```kotlin
implementation("io.github.kashif-mehmood-km:camerak:+")
```

### Optional Plugins

Image saving plugin:
```kotlin
implementation("io.github.kashif-mehmood-km:image_saver_plugin:0.0.6")
```

QR scanning plugin:
```kotlin
implementation("io.github.kashif-mehmood-km:qr_scanner_plugin:0.0.7")
```

OCR plugin:
```kotlin
implementation("io.github.kashif-mehmood-km:ocr_plugin:0.0.2")
```

## Platform Setup

### Android

Add these permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### iOS

Add these keys to your `Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>Camera permission is required for the app to work.</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>Photo Library permission is required for the app to work.</string>
```

## Quick Start

### 1. Handle Permissions

```kotlin
// Initialize and check permissions
val cameraPermissionState = remember { mutableStateOf(permissions.hasCameraPermission()) }
val storagePermissionState = remember { mutableStateOf(permissions.hasStoragePermission()) }

// Request permissions if needed
if (!cameraPermissionState.value) {
    permissions.RequestCameraPermission(
        onGranted = { cameraPermissionState.value = true },
        onDenied = { println("Camera Permission Denied") }
    )
}

if (!storagePermissionState.value) {
    permissions.RequestStoragePermission(
        onGranted = { storagePermissionState.value = true },
        onDenied = { println("Storage Permission Denied") }
    )
}
```

### 2. Set Up the Camera Controller

```kotlin
val cameraController = remember { mutableStateOf<CameraController?>(null) }

// Configure plugins if needed
val imageSaverPlugin = rememberImageSaverPlugin(
    config = ImageSaverConfig(
        isAutoSave = false,
        prefix = "MyApp",
        directory = Directory.PICTURES,
        customFolderName = "MyAppPhotos"  // Android only
    )
)

val qrScannerPlugin = rememberQRScannerPlugin(coroutineScope = coroutineScope)

// Set up QR code detection
LaunchedEffect(Unit) {
    qrScannerPlugin.getQrCodeFlow().distinctUntilChanged()
        .collectLatest { qrCode ->
            println("QR Code Detected: $qrCode")
            qrScannerPlugin.pauseScanning()
        }
}
```

### 3. Implement the Camera Preview

```kotlin
CameraPreview(
    modifier = Modifier.fillMaxSize(),
    cameraConfiguration = {
        setCameraLens(CameraLens.BACK)
        setFlashMode(FlashMode.OFF)
        setImageFormat(ImageFormat.JPEG)
        setDirectory(Directory.PICTURES)
        addPlugin(imageSaverPlugin)
        addPlugin(qrScannerPlugin)
    },
    onCameraControllerReady = {
        cameraController.value = it
        qrScannerPlugin.startScanning()
    }
)

// Display your custom camera UI once controller is ready
cameraController.value?.let { controller ->
    CameraScreen(cameraController = controller, imageSaverPlugin)
}
```

### 4. Capture and Save Images

```kotlin
Button(
    onClick = {
        scope.launch {
            when (val result = cameraController.takePicture()) {
                is ImageCaptureResult.Success -> {
                    // Handle the captured image
                    val bitmap = result.byteArray.decodeToImageBitmap()
                    
                    // Manually save the image if auto-save is disabled
                    if (!imageSaverPlugin.config.isAutoSave) {
                        imageSaverPlugin.saveImage(
                            byteArray = result.byteArray,
                            imageName = "Photo_${System.currentTimeMillis()}"
                        )
                    }
                }
                is ImageCaptureResult.Error -> {
                    println("Image Capture Error: ${result.exception.message}")
                }
            }
        }
    }
) {
    Text("Capture")
}
```

## Advanced Usage

Check the sample app in the repository for a complete implementation showcasing all features, including:

- Toggling between front and back cameras
- Managing flash modes
- Processing captured images
- Handling QR code scanning results
- OCR implementation

## Creating Custom Plugins

CameraK features a powerful plugin API that allows developers to extend its functionality. Explore the `qrScannerPlugin` or `imageSaverPlugin` source code for examples of how to build your own plugins.

## Status

This library is currently in experimental stage. APIs may change in future releases.

## Contributing

Contributions are welcome! Before submitting a pull request:

1. Open an issue to discuss your proposed changes
2. Fork the repository and create your branch
3. Implement your feature or fix
4. Ensure tests pass
5. Submit your pull request

## Support

If you find this library useful, consider supporting the developer:

<a href="https://www.buymeacoffee.com/kashifmehmood"><img src="https://img.buymeacoffee.com/button-api/?text=Buy me a coffee&emoji=&slug=kashifmehmood&button_colour=FFDD00&font_colour=000000&font_family=Cookie&outline_colour=000000&coffee_colour=ffffff" /></a>

## License

```
MIT License
```
