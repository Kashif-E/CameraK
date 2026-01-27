package com.kashif.cameraK.controller

import com.kashif.cameraK.enums.AspectRatio
import com.kashif.cameraK.enums.CameraDeviceType
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.QualityPrioritization
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.plugins.CameraPlugin
import com.kashif.cameraK.result.ImageCaptureResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.FrameGrabber
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Interface defining the core functionalities of the CameraController.
 */
actual class CameraController(
    internal var plugins: MutableList<CameraPlugin>,
    private val imageFormat: ImageFormat,
    private val directory: Directory,
    private val horizontalFlip: Boolean = false,
    private val customGrabber: FrameGrabber? = null,
    private val targetResolution: Pair<Int, Int>? = null
) {
    private var cameraGrabber: CameraGrabber? = null
    private val frameChannel = Channel<BufferedImage>(Channel.CONFLATED)
    private val qualityPriority: QualityPrioritization = QualityPrioritization.NONE

    private var listener: (ByteArray) -> Unit = {
        // default no-op listener
    }

    /**
     * Captures an image.
     *
     * @return The result of the image capture operation.
     */
    @Deprecated(
        message = "Use takePictureToFile() instead for better performance",
        replaceWith = ReplaceWith("takePictureToFile()"),
        level = DeprecationLevel.WARNING
    )
    actual suspend fun takePicture(): ImageCaptureResult {
        TODO("Not yet implemented")
    }

    actual suspend fun takePictureToFile(): ImageCaptureResult {
        return withContext(Dispatchers.IO) {
            val currentImage = cameraGrabber?.grabCurrentFrame()

            if (currentImage == null) {
                return@withContext ImageCaptureResult.Error(IllegalStateException("No image available"))
            }

            val outputStream = ByteArrayOutputStream()
            return@withContext try {
                ImageIO.write(currentImage, "jpg", outputStream)
                listener(outputStream.toByteArray())
                ImageCaptureResult.Success(outputStream.toByteArray())
            } catch (e: Exception) {
                e.printStackTrace()
                ImageCaptureResult.Error(e)
            } finally {
                outputStream.close()
            }
        }
    }

    /**
     * Toggles the flash mode between ON, OFF, and AUTO.
     */
    actual fun toggleFlashMode() {
        // flash mode not available on desktop
    }

    /**
     * Sets the flash mode of the camera
     *
     * @param mode The desired [FlashMode]
     */
    actual fun setFlashMode(mode: FlashMode) {
        // flash mode not available on desktop
    }

    /**
     * @return the current [FlashMode] of the camera, if available
     */
    actual fun getFlashMode(): FlashMode? {
        return FlashMode.OFF
    }

    /**
     * Toggles the torch mode between ON, OFF, and AUTO.
     *
     * Note: Torch is not available on desktop hardware.
     */
    actual fun toggleTorchMode() {
        // torch not available on desktop
    }

    /**
     * Sets the torch mode of the camera
     *
     * @param mode The desired [TorchMode]
     * 
     * Note: Torch is not available on desktop hardware.
     */
    actual fun setTorchMode(mode: TorchMode) {
        // torch not available on desktop
    }
    
    /**
     * Gets the current torch mode.
     * 
     * @return null as Desktop doesn't support torch mode
     */
    actual fun getTorchMode(): TorchMode? {
        return null
    }

    /**
     * Toggles the camera lens between FRONT and BACK.
     */
    actual fun toggleCameraLens() {
        // camera lens not available on desktop
    }
    
    /**
     * Gets the current camera lens.
     * 
     * @return null as Desktop doesn't support camera lens switching
     */
    actual fun getCameraLens(): CameraLens? {
        return null
    }
    
    /**
     * Gets the current image format.
     * 
     * @return The configured [ImageFormat]
     */
    actual fun getImageFormat(): ImageFormat {
        return imageFormat
    }
    
    /**
     * Gets the current quality prioritization setting.
     * 
     * @return The configured [QualityPrioritization] (always NONE on Desktop)
     */
    actual fun getQualityPrioritization(): QualityPrioritization {
        return qualityPriority
    }
    
    /**
     * Gets the current camera device type.
     * 
     * @return The configured [CameraDeviceType] (always DEFAULT on Desktop)
     */
    actual fun getPreferredCameraDeviceType(): CameraDeviceType {
        return CameraDeviceType.DEFAULT
    }

    /**
     * Sets the zoom level.
     * 
     * Note: Zoom is not supported on Desktop.
     */
    actual fun setZoom(zoomRatio: Float) {
        // zoom not available on desktop
    }
    
    /**
     * Gets the current zoom ratio.
     * 
     * @return 1.0 as Desktop doesn't support zoom
     */
    actual fun getZoom(): Float {
        return 1.0f
    }
    
    /**
     * Gets the maximum zoom ratio.
     * 
     * @return 1.0 as Desktop doesn't support zoom
     */
    actual fun getMaxZoom(): Float {
        return 1.0f
    }

    /**
     * Starts the camera session.
     */
    actual fun startSession() {
        CoroutineScope(Dispatchers.Default).launch {
            // If there is a custom grabber, use it, else use the default camera grabber
            // Which attempts to use the default camera
            cameraGrabber = CameraGrabber(frameChannel, {
                System.err.println("CameraK: Camera error: ${it.message}")
                it.printStackTrace()
            }, targetResolution).apply {
                setHorizontalFlip(horizontalFlip)
                start(this@launch, customGrabber)
            }
        }
    }

    /**
     * Stops the camera session.
     */
    actual fun stopSession() {
        cameraGrabber?.stop()
        frameChannel.close()
    }

    /**
     * Adds a listener for image capture events.
     *
     * @param listener The listener to add, receiving image data as [ByteArray].
     */
    actual fun addImageCaptureListener(listener: (ByteArray) -> Unit) {
        this.listener = listener
    }

    actual fun initializeControllerPlugins() {
        plugins.forEach {
            it.initialize(this)
        }
    }
    
    actual fun cleanup() {
        cameraGrabber?.stop()
        frameChannel.close()
    }

    fun getFrameChannel() = frameChannel
}