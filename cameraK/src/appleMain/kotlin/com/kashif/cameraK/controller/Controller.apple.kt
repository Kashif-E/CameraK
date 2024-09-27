package com.kashif.cameraK.controller

import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.Rotation
import com.kashif.cameraK.plugins.CameraPlugin
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.utils.toByteArray
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.AVFoundation.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIViewController
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class CameraController(
    internal var flashMode: FlashMode,
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

    actual suspend fun takePicture(): ImageCaptureResult = suspendCoroutine { cont ->
        customCameraController.captureImage()

        customCameraController.onPhotoCapture = { image ->
            val byteArray = image?.toByteArray() ?: byteArrayOf()
            cont.resume(ImageCaptureResult.Success(byteArray))
        }

        customCameraController.onError = { error ->
            cont.resume(ImageCaptureResult.Error(error))
        }
    }

    actual fun toggleFlashMode() {
        flashMode = when (flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
        customCameraController.setFlashMode(flashMode.toAVCaptureFlashMode())
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
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}

@OptIn(ExperimentalForeignApi::class)

private fun NSData.toByteArray(): ByteArray {
    val bytes = this.bytes?.reinterpret<ByteVar>()
    return ByteArray(this.length.toInt()) { i -> bytes!![i] }
}