package com.kashif.cameraK.controller

import com.kashif.cameraK.enums.CameraDeviceType
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.QualityPrioritization
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.result.ImageCaptureResult

/**
 * Interface defining the core functionalities of the CameraController.
 */
expect class CameraController {

    /**
     * Captures an image and returns it as a ByteArray.
     *
     * @return The result of the image capture operation with ByteArray.
     * @deprecated Use takePictureToFile() for better performance. This method processes images
     *             through decode/encode cycles which adds 2-3 seconds overhead. Will be removed in v2.0.
     */
    @Deprecated(
        message = "Use takePictureToFile() instead for better performance",
        replaceWith = ReplaceWith("takePictureToFile()"),
        level = DeprecationLevel.WARNING,
    )
    suspend fun takePicture(): ImageCaptureResult

    /**
     * Captures an image and saves it directly to a file.
     *
     * This method is significantly faster than takePicture() as it:
     * - Saves directly to disk without ByteArray conversion
     * - Skips decode/encode cycles (2-3 seconds faster)
     * - Avoids memory overhead from ByteArray processing
     *
     * @return ImageCaptureResult.SuccessWithFile containing the file path, or an error result
     */
    suspend fun takePictureToFile(): ImageCaptureResult

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
     * Toggles the torch mode between ON, OFF, and AUTO.
     *
     * Note: On Android, AUTO mode is not natively supported by CameraX and will be treated as ON.
     * iOS supports AUTO mode natively through AVFoundation.
     */
    fun toggleTorchMode()

    /**
     * Sets the torch mode of the camera
     *
     * @param mode The desired [TorchMode]
     *
     * Note: On Android, TorchMode.AUTO is not natively supported by CameraX and will be treated as ON.
     * iOS supports AUTO mode natively through AVFoundation.
     */
    fun setTorchMode(mode: TorchMode)

    /**
     * Gets the current torch mode.
     *
     * @return The current [TorchMode] (ON, OFF, AUTO), or null if not available
     *
     * Note: Desktop does not support torch mode and will always return null.
     */
    fun getTorchMode(): TorchMode?

    /**
     * Toggles the camera lens between FRONT and BACK.
     *
     * Note: Desktop does not support camera lens switching (single camera).
     */
    fun toggleCameraLens()

    /**
     * Gets the current camera lens.
     *
     * @return The current [CameraLens] (FRONT or BACK), or null if not available
     */
    fun getCameraLens(): CameraLens?

    /**
     * Gets the current image format.
     *
     * @return The configured [ImageFormat] (JPEG or PNG)
     */
    fun getImageFormat(): ImageFormat

    /**
     * Gets the current quality prioritization setting.
     *
     * @return The configured [QualityPrioritization]
     */
    fun getQualityPrioritization(): QualityPrioritization

    /**
     * Gets the current camera device type.
     *
     * @return The configured [CameraDeviceType]
     */
    fun getPreferredCameraDeviceType(): CameraDeviceType

    /**
     * Sets the zoom level.
     *
     * @param zoomRatio The zoom ratio to set. 1.0 is no zoom, values > 1.0 zoom in.
     *                  The actual range depends on the camera hardware.
     *                  On Android: typically 1.0 to maxZoomRatio (often 2.0-10.0)
     *                  On iOS: typically 1.0 to device.maxAvailableVideoZoomFactor
     *                  On Desktop: not supported, no-op
     *
     * Note: Zoom is applied gradually/smoothly on supported platforms.
     */
    fun setZoom(zoomRatio: Float)

    /**
     * Gets the current zoom ratio.
     *
     * @return The current zoom ratio, or 1.0 if zoom is not supported
     */
    fun getZoom(): Float

    /**
     * Gets the maximum zoom ratio supported by the camera.
     *
     * @return The maximum zoom ratio, or 1.0 if zoom is not supported
     */
    fun getMaxZoom(): Float

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

    /**
     * Initializes all registered plugins.
     */
    fun initializeControllerPlugins()

    /**
     * Cleans up resources when the controller is no longer needed.
     * Should be called when disposing the controller to prevent memory leaks.
     *
     * Note: After calling cleanup(), the controller should not be used again.
     */
    fun cleanup()
}
