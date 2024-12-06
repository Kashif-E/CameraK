package com.kashif.cameraK.controller

import com.kashif.cameraK.enums.*
import com.kashif.cameraK.plugins.CameraPlugin
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.utils.toByteArray
import com.kashif.cameraK.utils.toUIImage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.*
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIViewController
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

actual class CameraController(
    internal var flashMode: FlashMode,
    internal var torchMode: TorchMode,
    internal var cameraLens: CameraLens,
    internal var rotation: Rotation,
    internal var imageFormat: ImageFormat,
    internal var directory: Directory,
    internal var plugins: MutableList<CameraPlugin>
) : UIViewController(null, null) {

    private val customCameraController = CustomCameraController()
    private var imageCaptureListeners = mutableListOf<(ByteArray) -> Unit>()
    private var metadataOutput = AVCaptureMetadataOutput()
    private var metadataObjectsDelegate: AVCaptureMetadataOutputObjectsDelegateProtocol? = null


    override fun viewDidLoad() {
        super.viewDidLoad()
        setupCamera()
    }

    private fun setupCamera() {
        customCameraController.setupSession()
        customCameraController.setupPreviewLayer(view)

        // Add metadata output to the session
        if (customCameraController.captureSession?.canAddOutput(metadataOutput) == true) {
            customCameraController.captureSession?.addOutput(metadataOutput)
        }

        startSession()

        customCameraController.onPhotoCapture = { image ->
            image?.let {
                val data = it.toByteArray()
                imageCaptureListeners.forEach { it(data) }
            }
        }

        customCameraController.onError = { error ->
            println("Camera Error: $error")
        }

    }

    fun setMetadataObjectsDelegate(delegate: AVCaptureMetadataOutputObjectsDelegateProtocol) {
        metadataObjectsDelegate = delegate
        metadataOutput.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
    }

    fun updateMetadataObjectTypes(newTypes: List<String>) {
        if (customCameraController.captureSession?.isRunning() == true) {
            metadataOutput.metadataObjectTypes = newTypes
        } else {
            println("Camera session is not running.")
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        customCameraController.cameraPreviewLayer?.setFrame(view.bounds)
    }

    actual suspend fun takePicture(): ImageCaptureResult = suspendCancellableCoroutine { continuation ->
        customCameraController.onPhotoCapture = { image ->
            if (image != null) {
                when (imageFormat) {
                    ImageFormat.JPEG -> {
                        UIImageJPEGRepresentation(image.toUIImage(), 0.9)?.toByteArray()?.let { imageData ->
                            continuation.resume(
                                ImageCaptureResult.Success(
                                    imageData,
                                )
                            )

                        }
                    }

                    ImageFormat.PNG -> {
                        UIImagePNGRepresentation(image.toUIImage())?.toByteArray()?.let { imageData ->
                            continuation.resume(
                                ImageCaptureResult.Success(
                                    imageData,
                                )
                            )

                        }
                    }

                    else -> {
                        continuation.resume(ImageCaptureResult.Error(Exception("Failed to capture image")))
                    }
                }
            } else {
                continuation.resume(ImageCaptureResult.Error(Exception("Failed to capture image")))
            }
        }
        customCameraController.captureImage()
    }

    actual fun toggleFlashMode() {
        flashMode = when (flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
        customCameraController.setFlashMode(flashMode.toAVCaptureFlashMode())
    }

    actual fun setFlashMode(mode: FlashMode) {
        customCameraController.setFlashMode(mode.toAVCaptureFlashMode())
    }

    actual fun getFlashMode(): FlashMode? {
        fun AVCaptureFlashMode.toCameraKFlashMode(): FlashMode? {
            return when (this) {
                AVCaptureFlashModeOn -> FlashMode.ON
                AVCaptureFlashModeOff -> FlashMode.OFF
                AVCaptureFlashModeAuto -> FlashMode.AUTO
                else -> null
            }
        }

        return customCameraController.flashMode.toCameraKFlashMode()
    }

    actual fun toggleTorchMode() {
        torchMode = when (torchMode) {
            TorchMode.OFF -> TorchMode.ON
            TorchMode.ON -> TorchMode.AUTO
            TorchMode.AUTO -> TorchMode.OFF
        }
        customCameraController.setTorchMode(torchMode.toAVCaptureTorchMode())
    }

    actual fun toggleCameraLens() {
        customCameraController.switchCamera()
    }

    actual fun setCameraRotation(rotation: Rotation) {
        this.rotation = rotation
        customCameraController.cameraPreviewLayer?.connection()
            ?.setVideoOrientation(rotation.toAVCaptureVideoOrientation())
    }

    actual fun startSession() {
        customCameraController.startSession()

        initializeControllerPlugins()
    }

    actual fun stopSession() {
        customCameraController.stopSession()
    }

    actual fun addImageCaptureListener(listener: (ByteArray) -> Unit) {
        imageCaptureListeners.add(listener)
    }


    actual fun initializeControllerPlugins() {
        plugins.forEach {
            it.initialize(this)
        }
    }

    // Extension function to map FlashMode enum to AVCaptureFlashMode
    private fun FlashMode.toAVCaptureFlashMode(): AVCaptureFlashMode = when (this) {
        FlashMode.ON -> AVCaptureFlashModeOn
        FlashMode.OFF -> AVCaptureFlashModeOff
        FlashMode.AUTO -> AVCaptureFlashModeAuto
    }

    // Extension function to map Rotation enum to AVCaptureVideoOrientation
    private fun Rotation.toAVCaptureVideoOrientation(): AVCaptureVideoOrientation = when (this) {
        Rotation.ROTATION_0 -> AVCaptureVideoOrientationPortrait
        Rotation.ROTATION_90 -> AVCaptureVideoOrientationLandscapeRight
        Rotation.ROTATION_180 -> AVCaptureVideoOrientationPortraitUpsideDown
        Rotation.ROTATION_270 -> AVCaptureVideoOrientationLandscapeLeft
    }

    private fun TorchMode.toAVCaptureTorchMode(): AVCaptureTorchMode = when (this) {
        TorchMode.ON -> AVCaptureTorchModeOn
        TorchMode.OFF -> AVCaptureTorchModeOff
        TorchMode.AUTO -> AVCaptureTorchModeAuto
    }
}


