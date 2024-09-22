Here's a comprehensive `README.md` for your CameraK library:

---

# CameraK

**CameraK** is a Kotlin Multiplatform library that provides a unified API for integrating camera functionalities across Android and iOS platforms. With CameraK, you can easily capture photos in your Compose Multiplatform applications. The library simplifies permission handling, camera configuration, and image saving, making it a powerful tool for developers looking to add camera capabilities to their apps.

## Features

- **Multiplatform Support**: Write once, deploy on both Android and iOS.
- **Jetpack Compose Integration**: Seamless integration with Compose-based UIs.
- **Image Capture**: Capture high-quality photos.
- **Permission Handling**: Simplified APIs for managing camera and storage permissions.
- **Customizable Image Saving**: Choose between manual and automatic image saving with customizable storage destinations.
- **Scoped Storage Compliance (Android)**: Adheres to Android’s Scoped Storage guidelines.
- **Extensible Architecture**: Easily extend and customize functionalities via plugins.

## Getting Started

### Installation

CameraK is available on Maven Central. To include it in your project, add the following dependencies to your module's `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("io.github.kashif-mehmood-km:camerak:0.0.6")
}
```

### Configuration

#### Android

1. Ensure that your `compileSdk` and `minSdk` versions are set correctly in your `build.gradle.kts`:

    ```kotlin
    android {
        compileSdk = 34
        defaultConfig {
            minSdk = 21
        }
    }
    ```

2. Add the required permissions to your `AndroidManifest.xml`:

    ```xml
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    ```

#### iOS

1. Add the following keys to your `Info.plist`:

    ```xml
    <key>NSCameraUsageDescription</key>
    <string>This app requires access to the camera to take photos.</string>
    <key>NSPhotoLibraryAddUsageDescription</key>
    <string>This app requires access to your photo library to save captured images.</string>
    ```

2. Ensure that your Kotlin Multiplatform setup includes the necessary iOS targets.

### Permissions

CameraK provides a `Permissions` interface to handle camera and storage permissions for both Android and iOS platforms.

#### Example Usage

```kotlin
@Composable
fun CameraScreen() {
    val permissions: Permissions = providePermissions()
    var hasCameraPermission by remember { mutableStateOf(permissions.hasCameraPermission()) }
    var hasStoragePermission by remember { mutableStateOf(permissions.hasStoragePermission()) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissions.requestCameraPermission(
                onGranted = { hasCameraPermission = true },
                onDenied = { /* Handle denial */ }
            )
        }
        if (!hasStoragePermission && Build.VERSION.SDK_INT < 32) {
            permissions.requestStoragePermission(
                onGranted = { hasStoragePermission = true },
                onDenied = { /* Handle denial */ }
            )
        }
    }

    if (hasCameraPermission && hasStoragePermission) {
        // Initialize and display camera preview
    } else {
        // Display permission denied UI
    }
}
```

## Usage

### Capturing Photos

Once permissions are granted, you can configure and capture photos using the `CameraController` within your Compose UI.

```kotlin
@Composable
fun CameraInterface() {
    var cameraController by remember { mutableStateOf<CameraController?>(null) }
    val scope = rememberCoroutineScope()

    CameraPreview(
        modifier = Modifier.fillMaxSize(),
        cameraConfiguration = {
            setCameraLens(CameraLens.BACK)
            setFlashMode(FlashMode.ON)
            setImageFormat(ImageFormat.PNG)
            setDirectory(Directory.DCIM)
        },
        onCameraControllerReady = { controller ->
            cameraController = controller
        }
    )

    Button(
        onClick = {
            scope.launch {
                cameraController?.takePicture()?.let { result ->
                    if (result is ImageCaptureResult.Success) {
                        // Handle image capture success
                    } else if (result is ImageCaptureResult.Error) {
                        // Handle image capture error
                    }
                }
            }
        },
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .align(Alignment.BottomCenter)
            .padding(bottom = 32.dp)
    ) {
        Text(text = "Capture")
    }
}
```

### Handling Image Saving

CameraK allows you to save captured images either manually or automatically using the `ImageSaverPlugin`.

#### Manual Saving

```kotlin
cameraController?.takePicture()?.let { result ->
    if (result is ImageCaptureResult.Success) {
        imageSaverPlugin.saveImage(result.byteArray, "CustomImageName")
    }
}
```

#### Automatic Saving

```kotlin
val imageSaverConfig = ImageSaverConfig(
    isAutoSave = true,
    prefix = "MyApp",
    directory = Directory.PICTURES,
    customFolderName = "CustomFolder"
)

val imageSaverPlugin: ImageSaverPlugin = createImageSaverPlugin(config = imageSaverConfig)
```

## Advanced Features

### Automatic vs. Manual Image Saving

You can choose between automatic and manual image saving based on your application’s needs. Automatic saving is ideal for apps that require all captured images to be stored immediately, while manual saving gives you more control over when and where images are saved.

### Customizing Storage Destination

Customize where and how images are saved by setting a prefix, directory, and custom folder name in the `ImageSaverConfig`.

## Integration with Jetpack Compose

CameraK is designed to integrate seamlessly with Jetpack Compose, allowing you to build declarative, responsive UIs with camera functionalities.

### Example

```kotlin
@Composable
fun CameraInterface() {
    val permissions: Permissions = providePermissions()
    var hasCameraPermission by remember { mutableStateOf(permissions.hasCameraPermission()) }
    var hasStoragePermission by remember { mutableStateOf(permissions.hasStoragePermission()) }

    if (hasCameraPermission && hasStoragePermission) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            cameraConfiguration = {
                setCameraLens(CameraLens.BACK)
                setFlashMode(FlashMode.ON)
                setImageFormat(ImageFormat.PNG)
                setDirectory(Directory.DCIM)
            }
        )
    } else {
        // Display UI to request permissions
    }
}
```

## Troubleshooting

If you encounter any issues, please check the [Troubleshooting Guide](docs/Troubleshooting.md) or refer to the [Frequently Asked Questions (FAQ)](#faq).

## FAQ

- **Does CameraK support video recording?**
    - No, CameraK currently only supports image capture.

- **Is CameraK compatible with both Kotlin/JVM and Kotlin/Native projects?**
    - Yes, CameraK is designed for Kotlin Multiplatform projects, ensuring compatibility across JVM (Android) and Native (iOS) targets.

- **How do I update CameraK to the latest version?**
    - Update the version number in your `build.gradle.kts` dependency declaration and sync your project.

## Contributing

We welcome contributions to CameraK! Please see the [Contributing Guide](CONTRIBUTING.md) for more details.

## License

CameraK is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

**CameraK** simplifies adding camera functionalities to your Kotlin Multiplatform projects, allowing you to focus on building great user experiences. If you encounter any issues or have suggestions for improvement, please feel free to contribute or raise an issue on our GitHub repository.