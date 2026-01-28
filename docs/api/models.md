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
    JPEG, PNG, WEBP, RAW
}
```

## WhiteBalance

```kotlin
enum class WhiteBalance {
    AUTO,
    DAYLIGHT,
    CLOUDY,
    TUNGSTEN,
    FLUORESCENT
}
```

## FocusMode

```kotlin
enum class FocusMode {
    AUTO,
    MANUAL,
    CONTINUOUS
}
```

## Exceptions

```kotlin
open class CameraException(message: String) : Exception(message)

class PermissionDeniedException(message: String) : CameraException(message)
class CameraNotAvailableException(message: String) : CameraException(message)
class CameraTimeoutException(message: String) : CameraException(message)
class StorageException(message: String) : CameraException(message)
```
