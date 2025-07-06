# CameraK Troubleshooting Guide

## 🔧 Common Issues and Solutions

This guide helps resolve common issues encountered while using or developing CameraK.

## 🏗️ Build and Setup Issues

### Gradle Build Failures

#### Issue: `Plugin [id: 'com.android.library'] was not found`

**Symptoms:**
```
Plugin [id: 'com.android.library', version: '8.5.2', apply: false] was not found
```

**Solutions:**
1. **Install Android SDK:**
   ```bash
   # Set ANDROID_HOME environment variable
   export ANDROID_HOME=/path/to/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
   ```

2. **Update local.properties:**
   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```

3. **Install Android Studio and SDK:**
   - Download Android Studio
   - Install Android SDK Platform 21+
   - Install Android Build Tools

#### Issue: Kotlin Multiplatform compilation errors

**Symptoms:**
```
Could not determine the dependencies of task ':cameraK:compileKotlinAndroid'
```

**Solutions:**
1. **Clean and rebuild:**
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

2. **Invalidate caches (Android Studio):**
   - File → Invalidate Caches and Restart

3. **Update Gradle wrapper:**
   ```bash
   ./gradlew wrapper --gradle-version=8.9
   ```

#### Issue: iOS framework compilation fails

**Symptoms:**
```
Task ':cameraK:linkDebugFrameworkIosArm64' failed
```

**Solutions:**
1. **Ensure Xcode is installed:**
   ```bash
   xcode-select --install
   ```

2. **Accept Xcode license:**
   ```bash
   sudo xcodebuild -license accept
   ```

3. **Clean iOS build:**
   ```bash
   ./gradlew :cameraK:cleanIosFramework
   ./gradlew :cameraK:linkDebugFrameworkIosArm64
   ```

### Dependency Resolution Issues

#### Issue: Version conflicts

**Symptoms:**
```
Duplicate class found in modules
```

**Solutions:**
1. **Check dependency versions in `libs.versions.toml`**
2. **Use dependency resolution strategy:**
   ```kotlin
   configurations.all {
       resolutionStrategy {
           force("org.jetbrains.kotlin:kotlin-stdlib:1.9.23")
       }
   }
   ```

3. **Check for transitive dependency conflicts:**
   ```bash
   ./gradlew :cameraK:dependencies
   ```

## 📱 Runtime Issues

### Camera Initialization Problems

#### Issue: Camera not starting on Android

**Symptoms:**
- Black camera preview
- `CameraController` not ready callback never called
- SecurityException for camera access

**Solutions:**
1. **Check permissions in AndroidManifest.xml:**
   ```xml
   <uses-permission android:name="android.permission.CAMERA" />
   <uses-feature android:name="android.hardware.camera" android:required="true" />
   ```

2. **Request runtime permissions:**
   ```kotlin
   if (!permissions.hasCameraPermission()) {
       permissions.RequestCameraPermission(
           onGranted = { /* Granted */ },
           onDenied = { /* Denied */ }
       )
   }
   ```

3. **Check device capabilities:**
   ```kotlin
   val packageManager = context.packageManager
   val hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
   ```

4. **Verify CameraX requirements:**
   - Minimum API level 21
   - Device has camera hardware
   - App has camera permission

#### Issue: iOS camera permission denied

**Symptoms:**
- Camera preview shows permission prompt repeatedly
- AVAuthorizationStatus.denied

**Solutions:**
1. **Add Info.plist entries:**
   ```xml
   <key>NSCameraUsageDescription</key>
   <string>This app needs camera access to take photos.</string>
   ```

2. **Check authorization status:**
   ```swift
   let status = AVCaptureDevice.authorizationStatus(for: .video)
   ```

3. **Reset permissions (development):**
   - Device Settings → Privacy & Security → Camera → Your App → Toggle off/on
   - Simulator: Device → Erase All Content and Settings

#### Issue: Desktop camera not found

**Symptoms:**
- JavaCV throws exception
- No camera devices detected
- UnsatisfiedLinkError for native libraries

**Solutions:**
1. **Check camera availability:**
   ```bash
   # Linux
   ls /dev/video*
   
   # macOS
   system_profiler SPCameraDataType
   
   # Windows
   dxdiag
   ```

2. **Install native dependencies:**
   ```bash
   # Linux (Ubuntu/Debian)
   sudo apt-get install libopencv-dev
   
   # macOS
   brew install opencv
   ```

3. **Try different camera indices:**
   ```kotlin
   val grabber = FrameGrabber.createDefault(0) // Try 0, 1, 2, etc.
   ```

### Plugin Issues

#### Issue: QR Scanner not detecting codes

**Symptoms:**
- QR codes visible but not detected
- No callbacks from QRScannerPlugin

**Solutions:**
1. **Ensure plugin is added and initialized:**
   ```kotlin
   CameraPreview(
       cameraConfiguration = {
           addPlugin(qrScannerPlugin)
       }
   ) { controller ->
       qrScannerPlugin.startScanning() // Must call this
   }
   ```

2. **Check QR code quality:**
   - Ensure good lighting
   - QR code is not damaged or distorted
   - Proper distance from camera

3. **Verify supported formats:**
   - QR Code
   - EAN-13, EAN-8
   - Code 128, Code 39, Code 93
   - Data Matrix, PDF417, Aztec

#### Issue: OCR not recognizing text

**Symptoms:**
- Text visible but not detected
- Poor recognition accuracy

**Solutions:**
1. **Improve text visibility:**
   - Good lighting conditions
   - High contrast text
   - Clear, unobstructed text
   - Stable camera position

2. **Check language support:**
   ```kotlin
   // Android ML Kit supports multiple languages
   val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
   ```

3. **Optimize for text recognition:**
   - Use higher resolution
   - Ensure text is right-side up
   - Minimize camera shake

#### Issue: ImageSaver not saving files

**Symptoms:**
- takePicture() succeeds but no file saved
- Permission denied errors

**Solutions:**
1. **Check storage permissions:**
   ```xml
   <!-- Android -->
   <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
   ```

2. **Verify directory access:**
   ```kotlin
   val config = ImageSaverConfig(
       directory = Directory.PICTURES, // Try different directories
       customFolderName = "MyApp"      // Ensure valid folder name
   )
   ```

3. **Check available storage:**
   ```kotlin
   val file = File(Environment.getExternalStorageDirectory())
   val freeSpace = file.freeSpace
   ```

## 🎨 UI and Compose Issues

### Camera Preview Problems

#### Issue: Camera preview stretched or distorted

**Symptoms:**
- Preview doesn't match actual camera aspect ratio
- Image appears squished or stretched

**Solutions:**
1. **Use appropriate ContentScale:**
   ```kotlin
   // For Android CameraX
   previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
   ```

2. **Set correct aspect ratio:**
   ```kotlin
   CameraPreview(
       modifier = Modifier
           .fillMaxSize()
           .aspectRatio(4f / 3f) // Common camera ratio
   )
   ```

3. **Handle orientation changes properly:**
   ```kotlin
   // Ensure preview adapts to device orientation
   previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE)
   ```

#### Issue: Preview not updating after camera switch

**Symptoms:**
- toggleCameraLens() called but preview shows old camera
- Frozen preview after camera switch

**Solutions:**
1. **Ensure proper camera session restart:**
   ```kotlin
   controller.stopSession()
   controller.toggleCameraLens()
   controller.startSession()
   ```

2. **Check platform-specific implementations:**
   ```kotlin
   // Android: CameraX should handle this automatically
   // iOS: May need to recreate capture session
   // Desktop: Change camera index
   ```

### Performance Issues

#### Issue: Slow camera startup

**Symptoms:**
- Long delay before camera preview appears
- App freezes during camera initialization

**Solutions:**
1. **Initialize camera asynchronously:**
   ```kotlin
   LaunchedEffect(Unit) {
       withContext(Dispatchers.Default) {
           // Initialize camera in background
       }
   }
   ```

2. **Preload camera resources:**
   ```kotlin
   // Initialize CameraX provider early
   val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
   ```

3. **Optimize for platform:**
   - Android: Use CameraX performance mode
   - iOS: Configure session presets appropriately
   - Desktop: Choose optimal camera resolution

#### Issue: High memory usage

**Symptoms:**
- App crashes with OutOfMemoryError
- Increasing memory consumption over time

**Solutions:**
1. **Implement proper cleanup:**
   ```kotlin
   DisposableEffect(Unit) {
       onDispose {
           cameraController.stopSession()
           // Clean up resources
       }
   }
   ```

2. **Optimize image processing:**
   ```kotlin
   // Reduce image resolution for processing
   val resizedImage = Bitmap.createScaledBitmap(original, width/2, height/2, true)
   ```

3. **Use memory-efficient formats:**
   ```kotlin
   // Use JPEG instead of PNG for photos
   setImageFormat(ImageFormat.JPEG)
   ```

## 🔍 Debugging Techniques

### Logging and Diagnostics

#### Enable detailed logging

```kotlin
// Use Kermit for cross-platform logging
import co.touchlab.kermit.Logger

Logger.setTag("CameraK")
Logger.d { "Camera initialization started" }
Logger.e { "Camera error: ${exception.message}" }
```

#### Platform-specific debugging

**Android:**
```bash
# View logs
adb logcat | grep CameraK

# Check device info
adb shell getprop | grep camera
```

**iOS:**
```bash
# View device logs
xcrun simctl spawn booted log stream --predicate 'subsystem contains "CameraK"'
```

**Desktop:**
```kotlin
// Enable JavaCV debugging
System.setProperty("org.bytedeco.javacv.logger", "debug")
```

### Testing Camera Functionality

#### Manual testing checklist

- [ ] Camera preview loads
- [ ] Take picture works
- [ ] Flash controls function
- [ ] Camera lens toggle works
- [ ] Plugins initialize and work
- [ ] Permissions requested properly
- [ ] App handles orientation changes
- [ ] Memory usage remains stable

#### Automated testing

```kotlin
@Test
fun testCameraInitialization() {
    val controller = createCameraControllerBuilder()
        .setCameraLens(CameraLens.BACK)
        .build()
    
    assertNotNull(controller)
    // Add more assertions
}
```

## 📞 Getting Help

### Community Resources

1. **GitHub Issues:** Report bugs and request features
2. **GitHub Discussions:** Ask questions and share experiences
3. **Stack Overflow:** Tag questions with `camerak` and `kotlin-multiplatform`

### Debug Information to Include

When reporting issues, include:

```kotlin
// Platform information
println("Platform: ${Platform.osFamily}")
println("Kotlin version: ${KotlinVersion.CURRENT}")

// Camera information
println("Available cameras: ${controller.getAvailableCameras()}")
println("Current camera: ${controller.getCurrentCamera()}")

// Error details
try {
    controller.takePicture()
} catch (e: Exception) {
    println("Error: ${e.message}")
    println("Stack trace: ${e.stackTraceToString()}")
}
```

### Performance Profiling

**Android:**
- Use Android Studio Profiler
- Monitor memory and CPU usage
- Check for memory leaks

**iOS:**
- Use Xcode Instruments
- Profile memory and performance
- Check for retain cycles

**Desktop:**
- Use JVM profiling tools
- Monitor heap usage
- Check for memory leaks

This troubleshooting guide should help resolve most common issues encountered with CameraK. For issues not covered here, please check the GitHub repository or create a new issue with detailed information.