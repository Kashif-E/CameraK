# CameraPlugin

Plugin system for extending CameraK functionality.

## Plugin Interface

```kotlin
interface CameraPlugin {
    fun onCameraInitialized(controller: CameraController)
    fun onPhotoCapture(photo: Photo)
    fun onVideoCapture(video: Video)
    fun onError(exception: CameraException)
    fun onCleanup()
}
```

## Creating Custom Plugins

```kotlin
class AnalyticsPlugin : CameraPlugin {
    override fun onCameraInitialized(controller: CameraController) {
        println("Camera initialized - logging to analytics")
    }
    
    override fun onPhotoCapture(photo: Photo) {
        // Send analytics event
        Analytics.logEvent("photo_captured", bundleOf(
            "size" to photo.size,
            "resolution" to "${photo.width}x${photo.height}"
        ))
    }
    
    override fun onVideoCapture(video: Video) {
        Analytics.logEvent("video_captured")
    }
    
    override fun onError(exception: CameraException) {
        Analytics.logError(exception)
    }
    
    override fun onCleanup() {}
}
```

## Registering Plugins

```kotlin
val controller = CameraController.Builder()
    .addPlugin(AnalyticsPlugin())
    .addPlugin(StoragePlugin())
    .build()
```

## See Also

- [CameraController](camera-controller.md)
- [Core Concepts](../guide/core-concepts.md)
