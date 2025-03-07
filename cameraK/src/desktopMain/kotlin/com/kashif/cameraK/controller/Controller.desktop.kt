package com.kashif.cameraK.controller

import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
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
    imageFormat: ImageFormat,
    directory: Directory,
    private val horizontalFlip: Boolean = false,
    private val customGrabber: FrameGrabber? = null
) {
    private var cameraGrabber: CameraGrabber? = null
    private val frameChannel = Channel<BufferedImage>(Channel.CONFLATED)

    private var listener: (ByteArray) -> Unit = {
        // default no-op listener
    }

    /**
     * Captures an image.
     *
     * @return The result of the image capture operation.
     */
    actual suspend fun takePicture(): ImageCaptureResult {
        return withContext(Dispatchers.IO) {
            val currentImage = cameraGrabber?.grabCurrentFrame()

            if (currentImage == null) {
                println("No image available")
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
     * Toggles the torch mode between ON, OFF
     *
     * In IOS, torch mode include AUTO.
     */
    actual fun toggleTorchMode() {
        // torch not available on desktop
    }

    /**
     * Sets the torch mode of the camera
     *
     * @param mode The desired [TorchMode]
     */
    actual fun setTorchMode(mode: TorchMode) {
        //torch not available on desktop
    }

    /**
     * Toggles the camera lens between FRONT and BACK.
     */
    actual fun toggleCameraLens() {
        // camera lens not available on desktop
    }

    /**
     * Starts the camera session.
     */
    actual fun startSession() {
        CoroutineScope(Dispatchers.Default).launch {
            // If there is a custom grabber, use it, else use the default camera grabber
            // Which attempts to use the default camera
            cameraGrabber = CameraGrabber(frameChannel, {
                println("Camera error: ${it.message}")
                it.printStackTrace()
            }).apply {
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

    fun getFrameChannel() = frameChannel
}