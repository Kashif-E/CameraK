package com.kashif.imagesaverplugin

import coil3.PlatformContext
import com.kashif.imageSaverPlugin.ImageSaverConfig
import com.kashif.imageSaverPlugin.ImageSaverPlugin

/**
 * Platform-specific implementation of the [createImageSaverPlugin] factory function.
 */
actual fun createPlatformImageSaverPlugin(
    context: PlatformContext,
    config: ImageSaverConfig
): ImageSaverPlugin {
    TODO("Not yet implemented")
}