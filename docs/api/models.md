# Data Models

Core data classes used by CameraK.

## Photo

```kotlin
data class Photo(
    val uri: Uri,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val timestamp: Long,
    val flash: CameraFlash,
    val exposure: Float,
    val zoom: Float
)
```

## Video

```kotlin
data class Video(
    val uri: Uri,
    val duration: Long,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int,
    val size: Long,
    val codec: VideoCodec,
    val timestamp: Long
)
```

## CameraSelector

```kotlin
enum class CameraSelector {
    BACK,
    FRONT
}
```

## CameraFlash

```kotlin
enum class CameraFlash {
    ON, OFF, AUTO
}
```

## ImageFormat

```kotlin
enum class ImageFormat {
    JPEG, PNG
}
```

## TorchMode

```kotlin
enum class TorchMode {
    ON,
    OFF,
    AUTO
}
```

**Platform notes:**
- Android: AUTO is not supported by CameraX and will be treated as ON
- iOS: AUTO is fully supported
- Desktop: Not available

## QualityPrioritization

```kotlin
enum class QualityPrioritization {
    QUALITY,    // Best quality, slower
    SPEED,      // Faster capture, lower quality
    BALANCED,   // Balanced approach
    NONE        // Platform default
}
```

## CameraDeviceType

```kotlin
enum class CameraDeviceType {
    DEFAULT,
    WIDE_ANGLE,
    TELEPHOTO,
    ULTRA_WIDE
}
```

**Note:** Availability depends on device hardware. If requested type is not available, platform falls back to DEFAULT.

## Exceptions

```kotlin
open class CameraException(message: String) : Exception(message)

class PermissionDeniedException(message: String) : CameraException(message)
class CameraNotAvailableException(message: String) : CameraException(message)
class CameraTimeoutException(message: String) : CameraException(message)
class StorageException(message: String) : CameraException(message)
```
