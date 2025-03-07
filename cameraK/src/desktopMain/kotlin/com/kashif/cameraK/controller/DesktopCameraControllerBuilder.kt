package com.kashif.cameraK.controller

import com.kashif.cameraK.builder.CameraControllerBuilder
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.plugins.CameraPlugin
import com.kashif.cameraK.utils.InvalidConfigurationException
import org.bytedeco.javacv.FrameGrabber

/**
 * Desktop-specific implementation of [CameraControllerBuilder].
 */
class DesktopCameraControllerBuilder : CameraControllerBuilder {

    private var grabber: FrameGrabber? = null
    private var horizontalFlip: Boolean = false
    private var flashMode: FlashMode = FlashMode.OFF
    private var torchMode: TorchMode = TorchMode.OFF
    private var cameraLens: CameraLens = CameraLens.BACK
    private var imageFormat: ImageFormat? = null
    private var directory: Directory? = null
    private val plugins = mutableListOf<CameraPlugin>()

    fun setGrabber(grabber: FrameGrabber): CameraControllerBuilder {
        this.grabber = grabber
        return this
    }

    fun setHorizontalFlip(horizontalFlip: Boolean): CameraControllerBuilder {
        this.horizontalFlip = horizontalFlip
        return this
    }

    override fun setFlashMode(flashMode: FlashMode): CameraControllerBuilder {
        this.flashMode = flashMode
        return this
    }

    override fun setCameraLens(cameraLens: CameraLens): CameraControllerBuilder {
        this.cameraLens = cameraLens
        return this
    }

    override fun setImageFormat(imageFormat: ImageFormat): CameraControllerBuilder {
        this.imageFormat = imageFormat
        return this
    }

    override fun setTorchMode(torchMode: TorchMode): CameraControllerBuilder {
        this.torchMode = torchMode
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

        val cameraController = CameraController(
            imageFormat = format,
            directory = dir,
            plugins = plugins,
            horizontalFlip = horizontalFlip,
            customGrabber = grabber
        )

        return cameraController
    }
}
