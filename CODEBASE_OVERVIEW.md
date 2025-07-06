# CameraK Codebase Overview

## 📖 Introduction

CameraK is a comprehensive cross-platform camera library designed for Compose Multiplatform applications. It provides camera functionality across Android, iOS, and JVM (Desktop) platforms with a plugin-based architecture for extensibility.

## 🏗️ Project Architecture

### Repository Structure

```
CameraK/
├── cameraK/                 # Core camera library
├── Sample/                  # Demo application
├── ImageSaverPlugin/        # Image saving plugin
├── qrScannerPlugin/         # QR code scanning plugin
├── ocrPlugin/              # OCR text recognition plugin
├── iosApp/                 # iOS sample app
├── convention-plugins/      # Build configuration plugins
└── gradle/                 # Gradle configuration
```

### Core Components

#### 1. CameraK Core Library (`cameraK/`)

The main library is organized using Kotlin Multiplatform structure:

```
cameraK/src/
├── commonMain/             # Shared code across platforms
│   ├── controller/         # Camera controller interfaces
│   ├── ui/                # Composable UI components
│   ├── enums/             # Platform-agnostic enumerations
│   ├── plugins/           # Plugin interface definitions
│   ├── builder/           # Builder pattern implementations
│   └── result/            # Result types for operations
├── androidMain/           # Android-specific implementations
├── appleMain/             # iOS-specific implementations
└── desktopMain/           # JVM Desktop implementations
```

**Key Classes:**

- **`CameraController`** (expect/actual): Main interface for camera operations
- **`CameraPreview`** (Composable): Cross-platform camera preview component
- **`CameraControllerBuilder`**: Builder pattern for configuring camera settings
- **`CameraPlugin`**: Interface for extending camera functionality

#### 2. Platform-Specific Implementations

**Android (`androidMain/`):**
- Uses CameraX library for camera operations
- Implements Android-specific permissions handling
- Leverages ML Kit for text recognition

**iOS (`appleMain/`):**
- Uses AVFoundation framework
- Implements iOS-specific permission handling
- Custom camera controller for iOS-specific features

**Desktop (`desktopMain/`):**
- Uses JavaCV for camera access
- Implements desktop-specific UI components
- Uses Tesseract for OCR functionality

#### 3. Plugin System

CameraK implements a flexible plugin architecture:

```kotlin
interface CameraPlugin {
    fun initialize(cameraController: CameraController)
}
```

**Available Plugins:**

1. **ImageSaverPlugin**: Handles saving captured images to device storage
2. **QRScannerPlugin**: Provides QR code and barcode scanning capabilities
3. **OcrPlugin**: Enables text recognition from camera feed

## 🔧 Key Features

### Camera Operations

```kotlin
// Camera controller provides core functionality
suspend fun takePicture(): ImageCaptureResult
fun toggleFlashMode()
fun setFlashMode(mode: FlashMode)
fun toggleCameraLens() // Switch between front/back
fun startSession()
fun stopSession()
```

### Configuration Options

```kotlin
// Available camera settings
enum class CameraLens { FRONT, BACK }
enum class FlashMode { ON, OFF, AUTO }
enum class TorchMode { ON, OFF, AUTO }
enum class ImageFormat { JPEG, PNG }
enum class Directory { PICTURES, DCIM, DOWNLOADS }
enum class QualityPrioritization { QUALITY, SPEED }
```

### Cross-Platform UI

```kotlin
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraConfiguration: CameraControllerBuilder.() -> Unit,
    onCameraControllerReady: (CameraController) -> Unit
)
```

## 📱 Platform-Specific Details

### Android Implementation

**Dependencies:**
- CameraX for camera operations
- ML Kit for text recognition
- Coroutines for async operations

**Key Files:**
- `Controller.android.kt`: Android camera controller implementation
- `CameraPreview.android.kt`: Android camera preview component
- `Permissions.android.kt`: Android permission handling

### iOS Implementation

**Dependencies:**
- AVFoundation for camera operations
- CoreML for text recognition
- Platform interop for Swift integration

**Key Files:**
- `Controller.apple.kt`: iOS camera controller implementation
- `CustomCamera.kt`: Custom iOS camera implementation
- `PermissionHandler.apple.kt`: iOS permission handling

### Desktop Implementation

**Dependencies:**
- JavaCV for camera access
- Tesseract for OCR
- Swing integration for desktop UI

**Key Files:**
- `Controller.desktop.kt`: Desktop camera controller implementation
- `CameraGrabber.kt`: Camera frame grabbing implementation
- `ImagePanel.kt`: Desktop-specific UI components

## 🔌 Plugin Development

### Creating a Custom Plugin

```kotlin
class MyCustomPlugin : CameraPlugin {
    override fun initialize(cameraController: CameraController) {
        // Plugin initialization logic
        cameraController.addImageCaptureListener { imageData ->
            // Handle captured image data
        }
    }
}

// Usage in Compose
@Composable
fun rememberMyCustomPlugin(): MyCustomPlugin {
    return remember { MyCustomPlugin() }
}
```

### Plugin Examples

#### ImageSaverPlugin
```kotlin
val imageSaverPlugin = rememberImageSaverPlugin(
    config = ImageSaverConfig(
        isAutoSave = false,
        prefix = "MyApp",
        directory = Directory.PICTURES,
        customFolderName = "MyAppPhotos"
    )
)
```

#### QRScannerPlugin
```kotlin
val qrScannerPlugin = rememberQRScannerPlugin(coroutineScope = coroutineScope)

// Listen for QR codes
LaunchedEffect(Unit) {
    qrScannerPlugin.getQrCodeFlow().collect { qrCode ->
        println("QR Code detected: $qrCode")
    }
}
```

## 📦 Build System

### Gradle Configuration

The project uses Kotlin Multiplatform with Compose Multiplatform:

```kotlin
// Main build configuration
kotlin {
    androidTarget()
    jvm("desktop")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64())
    
    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(libs.kotlinx.coroutines.core)
        }
        
        androidMain.dependencies {
            api(libs.camera.core)
            api(libs.camera.camera2)
        }
        
        val desktopMain by getting {
            dependencies {
                api(libs.javacv.platform)
            }
        }
    }
}
```

### Publishing

The library is published to Maven Central with the following coordinates:

- **Core**: `io.github.kashif-mehmood-km:camerak:+`
- **ImageSaver**: `io.github.kashif-mehmood-km:image_saver_plugin:+`
- **QRScanner**: `io.github.kashif-mehmood-km:qr_scanner_plugin:+`
- **OCR**: `io.github.kashif-mehmood-km:ocr_plugin:+`

## 🚀 Usage Examples

### Basic Camera Setup

```kotlin
@Composable
fun CameraScreen() {
    val cameraController = remember { mutableStateOf<CameraController?>(null) }
    val imageSaverPlugin = rememberImageSaverPlugin()
    
    CameraPreview(
        modifier = Modifier.fillMaxSize(),
        cameraConfiguration = {
            setCameraLens(CameraLens.BACK)
            setFlashMode(FlashMode.OFF)
            setImageFormat(ImageFormat.JPEG)
            addPlugin(imageSaverPlugin)
        },
        onCameraControllerReady = { controller ->
            cameraController.value = controller
        }
    )
    
    // Camera controls UI
    cameraController.value?.let { controller ->
        CameraControlsUI(controller)
    }
}
```

### Advanced Configuration

```kotlin
CameraPreview(
    cameraConfiguration = {
        setCameraLens(CameraLens.BACK)
        setFlashMode(FlashMode.AUTO)
        setTorchMode(TorchMode.OFF)
        setImageFormat(ImageFormat.JPEG)
        setDirectory(Directory.PICTURES)
        setQualityPrioritization(QualityPrioritization.QUALITY)
        
        // Add multiple plugins
        addPlugin(imageSaverPlugin)
        addPlugin(qrScannerPlugin)
        addPlugin(ocrPlugin)
    }
) { controller ->
    // Controller ready callback
}
```

## 🔍 Testing Strategy

The project includes:

- Unit tests for core functionality
- Platform-specific tests
- Integration tests for plugin system
- UI tests for Compose components

## 📋 Development Guidelines

### Code Organization

1. **Shared Logic**: Place in `commonMain` when possible
2. **Platform-Specific**: Use expect/actual declarations
3. **UI Components**: Compose-based, cross-platform when possible
4. **Plugins**: Implement `CameraPlugin` interface

### Best Practices

1. **Error Handling**: Use sealed classes for result types
2. **Async Operations**: Leverage Kotlin coroutines
3. **State Management**: Use Compose state management
4. **Platform APIs**: Abstract platform differences through common interfaces

## 🛠️ Contributing

The project welcomes contributions following these areas:

1. **New Plugins**: Extend functionality through plugins
2. **Platform Support**: Add support for new platforms
3. **UI Improvements**: Enhance camera preview and controls
4. **Performance**: Optimize camera operations
5. **Documentation**: Improve code documentation and examples

## 📊 Dependencies Overview

### Core Dependencies
- Kotlin Multiplatform
- Compose Multiplatform
- Kotlinx Coroutines
- Kermit (Logging)

### Platform-Specific Dependencies
- **Android**: CameraX, ML Kit, Activity Compose
- **iOS**: AVFoundation, CoreML
- **Desktop**: JavaCV, Tesseract, Swing Coroutines

### Plugin Dependencies
- **QR Scanner**: ZXing
- **OCR**: ML Kit (Android), Tesseract (Desktop)
- **Image Saver**: Platform-specific file systems

This comprehensive overview provides a solid foundation for understanding and contributing to the CameraK project.