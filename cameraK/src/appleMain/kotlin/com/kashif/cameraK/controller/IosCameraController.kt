package com.kashif.cameraK.controller


import androidx.compose.ui.uikit.InterfaceOrientation
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.Rotation
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.utils.InvalidConfigurationException
import com.kashif.cameraK.utils.enums.*
import com.kashif.cameraK.utils.toByteArray
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import platform.AVFoundation.*
import platform.Foundation.*
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS-specific implementation of [CameraController] using AVFoundation.
 *
 * @param flashMode The desired [FlashMode].
 * @param cameraLens The desired [CameraLens].
 * @param rotation The desired [Rotation].
 * @param imageFormat The desired [ImageFormat].
 * @param directory The desired [Directory] to save images.
 */
class IOSCameraController(
    private var flashMode: FlashMode,
    private var cameraLens: CameraLens,
    private var rotation: Rotation,
    private var imageFormat: ImageFormat,
    private var directory: Directory
) : UIViewController(), CameraController {

    private val session = AVCaptureSession()
    private var photoOutput = AVCapturePhotoOutput()
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var imageCaptureListeners = mutableListOf<(ByteArray) -> Unit>()

    override fun viewDidLoad() {
        super.viewDidLoad()
        setupCamera()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupCamera() {
        session.beginConfiguration()

        // Select camera device
        val device = selectCameraDevice(cameraLens)
            ?: throw InvalidConfigurationException("Camera device not found.")

        // Add input
        val input = try {
            AVCaptureDeviceInput.deviceInputWithDevice(device, null)
        } catch (e: Exception) {
            throw InvalidConfigurationException("Failed to create camera input: ${e.message}")
        }

        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
        } else {
            throw InvalidConfigurationException("Cannot add camera input to session.")
        }

        // Add photo output
        if (session.canAddOutput(photoOutput)) {
            session.addOutput(photoOutput)
        } else {
            throw InvalidConfigurationException("Cannot add photo output to session.")
        }

        session.commitConfiguration()

        // Setup preview layer
        previewLayer =
            AVCaptureVideoPreviewLayer.layerWithSession(session) as? AVCaptureVideoPreviewLayer
        previewLayer?.videoGravity = AVLayerVideoGravityResizeAspectFill
        previewLayer?.connection()?.setVideoOrientation(rotation.toAVCaptureVideoOrientation())
        previewLayer?.let { view.layer.addSublayer(it) }
    }

    private fun selectCameraDevice(lens: CameraLens): AVCaptureDevice? {
        val position = when (lens) {
            CameraLens.FRONT -> AVCaptureDevicePositionFront
            CameraLens.BACK -> AVCaptureDevicePositionBack
        }
        val discoverySession =
            AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
                deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
                mediaType = AVMediaTypeVideo,
                position = position
            )
        return discoverySession.devices().firstOrNull() as AVCaptureDevice?
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.setFrame(view.bounds)
    }

    override fun bindCamera(previewView: Any) {
        // No-op since iOS handles the preview internally
    }

    override suspend fun takePicture(): ImageCaptureResult = suspendCoroutine { cont ->
        val settings = AVCapturePhotoSettings.photoSettings()
        settings.flashMode = flashMode.toAVCaptureFlashMode()
        settings.setHighResolutionPhotoEnabled(true)

        photoOutput.capturePhotoWithSettings(
            settings,
            PhotoCaptureDelegate(imageCaptureListeners, cont, imageFormat)
        )
    }

    override fun toggleFlashMode() {
        flashMode = when (flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
    }


    @OptIn(ExperimentalForeignApi::class)
    override fun toggleCameraLens() {
        // Implement camera lens switching by reconfiguring the session with a different device
        val newLens = if (cameraLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        cameraLens = newLens

        session.beginConfiguration()
        // Remove existing input
        session.inputs().forEach { session.removeInput(it as AVCaptureInput) }

        // Add new input
        val newDevice = selectCameraDevice(newLens)
            ?: throw InvalidConfigurationException("Camera device not found.")

        val newInput = try {
            AVCaptureDeviceInput.deviceInputWithDevice(
                newDevice,
                null
            )
        } catch (e: Exception) {
            throw InvalidConfigurationException("Failed to create camera input: ${e.message}")
        }

        if (newInput != null && session.canAddInput(newInput)) {
            session.addInput(newInput)
        } else {
            throw InvalidConfigurationException("Cannot add camera input to session.")
        }

        // Update preview orientation
        previewLayer?.connection()?.setVideoOrientation(rotation.toAVCaptureVideoOrientation())

        session.commitConfiguration()
    }

    override fun setCameraRotation(rotation: Rotation) {
        this.rotation = rotation
        previewLayer?.connection()?.setVideoOrientation(rotation.toAVCaptureVideoOrientation())
    }

    override fun startSession() {
        session.startRunning()
    }

    override fun stopSession() {
        session.stopRunning()
    }

    override fun addImageCaptureListener(listener: (ByteArray) -> Unit) {
        imageCaptureListeners.add(listener)
    }

    /**
     * Converts [FlashMode] to [AVCaptureFlashMode].
     */
    private fun FlashMode.toAVCaptureFlashMode(): AVCaptureFlashMode = when (this) {
        FlashMode.ON -> AVCaptureFlashModeOn
        FlashMode.OFF -> AVCaptureFlashModeOff
        FlashMode.AUTO -> AVCaptureFlashModeAuto
    }

    /**
     * Converts [Rotation] to [AVCaptureVideoOrientation].
     */
    private fun Rotation.toAVCaptureVideoOrientation(): AVCaptureVideoOrientation = when (this) {
        Rotation.ROTATION_0 -> AVCaptureVideoOrientationPortrait
        Rotation.ROTATION_90 -> AVCaptureVideoOrientationLandscapeRight
        Rotation.ROTATION_180 -> AVCaptureVideoOrientationPortraitUpsideDown
        Rotation.ROTATION_270 -> AVCaptureVideoOrientationLandscapeLeft
    }

    /**
     * Delegate class for handling photo capture callbacks.
     */
    private class PhotoCaptureDelegate(
        private val listeners: List<(ByteArray) -> Unit>,
        private val cont: kotlin.coroutines.Continuation<ImageCaptureResult>,
        private val imageFormat: ImageFormat
    ) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {




        override fun captureOutput(
            output: AVCapturePhotoOutput,
            didFinishProcessingPhoto: AVCapturePhoto,
            error: NSError?
        ) {
            if (error != null) {
                cont.resume(ImageCaptureResult.Error(Exception("Photo capture error: ${error.localizedDescription}")))
                return
            }

            val imageData = didFinishProcessingPhoto.fileDataRepresentation()
            if (imageData == null) {
                cont.resume(ImageCaptureResult.Error(Exception("Failed to get image data.")))
                return
            }

            // Convert NSData to ByteArray
            val byteArray = imageData.toByteArray()
            listeners.forEach { it(byteArray) }
            cont.resume(ImageCaptureResult.Success(byteArray))
        }
    }
}