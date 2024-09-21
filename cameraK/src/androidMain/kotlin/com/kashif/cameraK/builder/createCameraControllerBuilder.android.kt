package com.kashif.cameraK.builder

import coil3.PlatformContext
import com.kashif.cameraK.utils.getActivityOrNull

/**
 * iOS-specific implementation of [CameraControllerBuilder].
 */
actual fun createCameraControllerBuilder(context: PlatformContext) =
    createAndroidCameraControllerBuilder(context = context, lifecycleOwner = context.getActivityOrNull()!!.lif)