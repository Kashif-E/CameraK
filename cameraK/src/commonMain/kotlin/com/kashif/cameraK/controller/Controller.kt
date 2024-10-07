package com.kashif.cameraK.controller

import com.kashif.cameraK.enums.Rotation
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
     * Toggles the torch mode between ON, OFF
     *
     * In IOS, torch mode include AUTO.
     */
    fun toggleTorchMode()

    /**
     * Toggles the camera lens between FRONT and BACK.
     */
    fun toggleCameraLens()

    /**
     * Sets the rotation of the camera preview and image capture.
     *
     * @param rotation The desired [Rotation].
     */
    fun setCameraRotation(rotation: Rotation)

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