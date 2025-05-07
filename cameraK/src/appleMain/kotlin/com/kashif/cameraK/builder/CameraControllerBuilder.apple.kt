package com.kashif.cameraK.builder


import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.*
import com.kashif.cameraK.plugins.CameraPlugin
import com.kashif.cameraK.utils.InvalidConfigurationException

/**
 * iOS-specific implementation of [CameraControllerBuilder].
 */
class IOSCameraControllerBuilder : CameraControllerBuilder {

    private var flashMode: FlashMode = FlashMode.OFF
    private var torchMode: TorchMode = TorchMode.OFF
    private var cameraLens: CameraLens = CameraLens.BACK
    private var imageFormat: ImageFormat? = null
    private var directory: Directory? = null
    private var qualityPriority: QualityPrioritization = QualityPrioritization.NONE
    private val plugins = mutableListOf<CameraPlugin>()

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

    override fun setQualityPrioritization(prioritization: QualityPrioritization): CameraControllerBuilder {
        this.qualityPriority = prioritization
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
            flashMode = flashMode,
            torchMode = torchMode,
            cameraLens = cameraLens,
            imageFormat = format,
            directory = dir,
            plugins = plugins,
            qualityPriority = qualityPriority
        )

        return cameraController
    }
}
