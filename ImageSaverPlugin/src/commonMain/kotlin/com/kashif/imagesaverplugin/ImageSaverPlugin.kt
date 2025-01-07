package com.kashif.imagesaverplugin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.plugins.CameraPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

/**
 * Configuration for the ImageSaverPlugin.
 *
 * @param isAutoSave Determines if images should be saved automatically upon capture.
 * @param prefix Optional prefix for image filenames when auto-saving.
 * @param directory Specifies the directory where images will be saved when auto-saving.
 * @param customFolderName Optional custom folder name within the specified directory.
 */
data class ImageSaverConfig(
    val isAutoSave: Boolean = false,
    val prefix: String? = null,
    val directory: Directory = Directory.PICTURES,
    val customFolderName: String? = null,
    val imageFormat : ImageFormat= ImageFormat.JPEG
)

/**
 * Abstract plugin to save captured images.
 *
 * Provides methods to save images either manually or automatically based on configuration.
 *
 * @param config Configuration settings for the plugin.
 */
abstract class ImageSaverPlugin(
    val config: ImageSaverConfig
) : CameraPlugin {

    /**
     * Saves the captured image data to storage manually.
     *
     * @param byteArray The image data as a [ByteArray].
     * @param imageName Optional custom name for the image. If not provided, a default name is generated.
     */
    abstract suspend fun saveImage(byteArray: ByteArray, imageName: String? = null): String?

    /**
     * Initializes the plugin. If auto-save is enabled, sets up listeners to save images automatically.
     *
     * @param cameraController The [CameraController] instance to attach listeners.
     */
    override fun initialize(cameraController: com.kashif.cameraK.controller.CameraController) {
        if (config.isAutoSave) {
            cameraController.addImageCaptureListener { byteArray ->
                 CoroutineScope(Dispatchers.IO).launch {
                    val imageName = config.prefix?.let { "CameraK" }
                    saveImage(byteArray, imageName)
                }
            }
        }
    }

    abstract fun getByteArrayFrom(path: String): ByteArray
}

/**
 * Factory function to create a platform-specific [ImageSaverPlugin].
 *
 * @param config Configuration settings for the plugin.
 * @return An instance of [ImageSaverPlugin].
 */
@Composable
fun rememberImageSaverPlugin(
    config: ImageSaverConfig,
    context: PlatformContext = LocalPlatformContext.current
): ImageSaverPlugin {
    return remember(config) {
        createPlatformImageSaverPlugin(context, config)
    }
}

/**
 * Platform-specific implementation of the [rememberImageSaverPlugin] factory function.
 */

expect fun createPlatformImageSaverPlugin(
    context: PlatformContext,
    config: ImageSaverConfig
): ImageSaverPlugin