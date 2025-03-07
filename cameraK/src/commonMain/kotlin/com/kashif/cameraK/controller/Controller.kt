package com.kashif.cameraK.controller

import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.result.ImageCaptureResult

/**
 * Interface defining the core functionalities of the CameraController.
 */
expect class CameraController {

    /**
     * Captures an image.
     *
     * @return The result of the image capture operation.
     */
    suspend fun takePicture(): ImageCaptureResult

    /**
     * Toggles the flash mode between ON, OFF, and AUTO.
     */
    fun toggleFlashMode()

    /**
     * Sets the flash mode of the camera
     *
     * @param mode The desired [FlashMode]
     */
    fun setFlashMode(mode: FlashMode)

    /**
     * @return the current [FlashMode] of the camera, if available
     */
    fun getFlashMode(): FlashMode?

    /**
     * Toggles the torch mode between ON, OFF
     *
     * In IOS, torch mode include AUTO.
     */
    fun toggleTorchMode()

    /**
     * Sets the torch mode of the camera
     *
     * @param mode The desired [TorchMode]
     */
    fun setTorchMode(mode: TorchMode)

    /**
     * Toggles the camera lens between FRONT and BACK.
     */
    fun toggleCameraLens()


    /**
     * Starts the camera session.
     */
    fun startSession()

    /**
     * Stops the camera session.
     */
    fun stopSession()

    /**
     * Adds a listener for image capture events.
     *
     * @param listener The listener to add, receiving image data as [ByteArray].
     */
    fun addImageCaptureListener(listener: (ByteArray) -> Unit)
    fun initializeControllerPlugins()
}