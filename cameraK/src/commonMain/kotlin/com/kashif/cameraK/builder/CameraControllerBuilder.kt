package com.kashif.cameraK.builder

import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.Rotation
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.plugins.CameraPlugin

/**
 * Builder interface for constructing a [CameraController] with customizable configurations and plugins.
 */
interface CameraControllerBuilder {
    fun setFlashMode(flashMode: FlashMode): CameraControllerBuilder
    fun setCameraLens(cameraLens: CameraLens): CameraControllerBuilder
    fun setRotation(rotation: Rotation): CameraControllerBuilder
    fun setImageFormat(imageFormat: ImageFormat): CameraControllerBuilder
    fun setDirectory(directory: Directory): CameraControllerBuilder

    /**
     * Adds a [CameraPlugin] to the [CameraController].
     *
     * @param plugin The plugin to add.
     * @return The current instance of [CameraControllerBuilder].
     */
    fun addPlugin(plugin: CameraPlugin): CameraControllerBuilder

    /**
     * Builds and returns a configured instance of [CameraController].
     *
     * @throws InvalidConfigurationException If mandatory parameters are missing or configurations are incompatible.
     * @return A fully configured [CameraController] instance.
     */
    fun build(): CameraController
    fun setTorchMode(torchMode: TorchMode): CameraControllerBuilder
}