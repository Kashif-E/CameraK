package com.kashif.cameraK.controller

import com.kashif.cameraK.enums.Rotation
import com.kashif.cameraK.result.ImageCaptureResult

/**
 * Interface defining the core functionalities of the CameraController.
 */
interface CameraController {
    /**
     * Binds the camera to the provided preview view.
     *
     * @param previewView The preview view to display the camera feed.
     */
    fun bindCamera(previewView: Any) // 'Any' can be replaced with platform-specific types using expect/actual

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
}