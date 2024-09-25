package com.kashif.cameraK.builder

import com.kashif.cameraK.utils.InvalidConfigurationException
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.controller.AndroidCameraController
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.Rotation
import com.kashif.cameraK.plugins.CameraPlugin


/**
 * Android-specific implementation of [CameraControllerBuilder].
 *
 * @param context The Android [Context], typically an Activity or Application context.
 * @param lifecycleOwner The [LifecycleOwner], usually the hosting Activity or Fragment.
 */
class AndroidCameraControllerBuilder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : CameraControllerBuilder {

    private var flashMode: FlashMode = FlashMode.OFF
    private var cameraLens: CameraLens = CameraLens.BACK
    private var rotation: Rotation = Rotation.ROTATION_0
    private var imageFormat: ImageFormat? = null
    private var directory: Directory? = null
    private val plugins = mutableListOf<CameraPlugin>()

    override fun setFlashMode(flashMode: FlashMode): CameraControllerBuilder {
        this.flashMode = flashMode
        return this
    }

    override fun setCameraLens(cameraLens: CameraLens): CameraControllerBuilder {
        this.cameraLens = cameraLens
        return this
    }

    override fun setRotation(rotation: Rotation): CameraControllerBuilder {
        this.rotation = rotation
        return this
    }

    override fun setImageFormat(imageFormat: ImageFormat): CameraControllerBuilder {
        this.imageFormat = imageFormat
        return this
    }

    override fun setDirectory(directory: Directory): CameraControllerBuilder {
        this.directory = directory
        return this
    }

    override fun addPlugin(plugin: CameraPlugin): CameraControllerBuilder {
        plugins.add(plugin)
        return this
    }

    override fun build(): CameraController {

        val format = imageFormat ?: throw InvalidConfigurationException("ImageFormat must be set.")
        val dir = directory ?: throw InvalidConfigurationException("Directory must be set.")


        if (flashMode == FlashMode.ON && cameraLens == CameraLens.FRONT) {
            throw InvalidConfigurationException("Flash mode ON is not supported with the front camera.")
        }
        val cameraController = AndroidCameraController(
            context = context,
            lifecycleOwner = lifecycleOwner,
            flashMode = flashMode,
            cameraLens = cameraLens,
            rotation = rotation,
            imageFormat = format,
            directory = dir,
            plugins = plugins
        )

        return cameraController
    }
}