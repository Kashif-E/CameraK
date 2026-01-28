# Advanced Usage

Advanced techniques and patterns for CameraK.

## Custom Camera Effects

Create custom photo effects:

```kotlin
sealed class CustomEffect {
    data class ColorFilter(val hue: Float) : CustomEffect()
    data class Blur(val radius: Float) : CustomEffect()
    data class Sepia(val intensity: Float) : CustomEffect()
}

fun applyEffect(photo: Photo, effect: CustomEffect): Photo {
    return when (effect) {
        is CustomEffect.ColorFilter -> photo.applyColorFilter(effect.hue)
        is CustomEffect.Blur -> photo.applyBlur(effect.radius)
        is CustomEffect.Sepia -> photo.applySepia(effect.intensity)
    }
}
```

## Processing Pipeline

Chain multiple operations:

```kotlin
val processedPhoto = controller.capturePhoto()
    .rotate(90)
    .crop(x = 0, y = 0, width = 1000, height = 1000)
    .resize(800, 800)
    .compress(0.85f)
    .applyFilter(PhotoFilter.SEPIA)
```

## Memory Management

Handle large images efficiently:

```kotlin
// Process in chunks
flow {
    for (i in 0 until totalPhotos) {
        val photo = capturePhoto()
        emit(photo)
        // Optional: clear cache
        System.gc()
    }
}.collect { photo ->
    processPhoto(photo)
}
```

## Platform-Specific Features

### Android: CameraX Extensions

```kotlin
controller.enableExtension(
    ExtensionMode.BEAUTY
)
```

### iOS: Metal Rendering

```kotlin
controller.enableMetalRendering(
    pixelFormat = MTLPixelFormat.RGBA8Unorm
)
```

## Next Steps

- [Troubleshooting](../troubleshooting.md)
- [API Reference](../api/camera-controller.md)
