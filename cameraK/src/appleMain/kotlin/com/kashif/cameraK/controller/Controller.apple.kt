package com.kashif.cameraK.controller

import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.plugins.CameraPlugin
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.utils.toByteArray
import com.kashif.cameraK.utils.toUIImage
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVCaptureFlashMode
import platform.AVFoundation.AVCaptureFlashModeAuto
import platform.AVFoundation.AVCaptureFlashModeOff
import platform.AVFoundation.AVCaptureFlashModeOn
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureTorchMode
import platform.AVFoundation.AVCaptureTorchModeAuto
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoOrientation
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.Foundation.NSDate
import platform.Foundation.NSTimeInterval
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIViewController
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

actual class CameraController(
    internal var flashMode: FlashMode,
    internal var torchMode: TorchMode,
    internal var cameraLens: CameraLens,
    internal var imageFormat: ImageFormat,
    internal var directory: Directory,
    internal var plugins: MutableList<CameraPlugin>
) : UIViewController(null, null) {
    private val photoDebounceInterval = 500L
    private var lastCaptureTime: NSTimeInterval = 0.0
    private var isCapturing = atomic(false)
    private val customCameraController = CustomCameraController()
    private var imageCaptureListeners = mutableListOf<(ByteArray) -> Unit>()
    private var metadataOutput = AVCaptureMetadataOutput()
    private var metadataObjectsDelegate: AVCaptureMetadataOutputObjectsDelegateProtocol? = null

    override fun viewDidLoad() {
        super.viewDidLoad()
        setupCamera()
    }

    fun getCameraPreviewLayer() = customCameraController.cameraPreviewLayer

    internal fun currentVideoOrientation(): AVCaptureVideoOrientation {

        val orientation = UIDevice.currentDevice.orientation
        return when (orientation) {
            UIDeviceOrientation.UIDeviceOrientationPortrait -> AVCaptureVideoOrientationPortrait
            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeRight
            UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeLeft
            else -> AVCaptureVideoOrientationPortrait
        }
    }
    private fun setupCamera() {
        customCameraController.setupSession()
        customCameraController.setupPreviewLayer(view)


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
            metadataOutput.metadataObjectTypes += newTypes
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
        val currentTime = NSDate.date().timeIntervalSince1970()
        if (currentTime - lastCaptureTime < photoDebounceInterval) {
            continuation.resume(ImageCaptureResult.Error(Exception("Capture too frequent")))
            return@suspendCancellableCoroutine
        }

        if (!isCapturing.compareAndSet(false, true)) {
            continuation.resume(ImageCaptureResult.Error(Exception("Capture already in progress")))
            return@suspendCancellableCoroutine
        }

        customCameraController.onPhotoCapture = { image ->
            try {
                if (image != null) {
                    autoreleasepool {
                        when (imageFormat) {
                            ImageFormat.JPEG -> {
                                UIImageJPEGRepresentation(image.toUIImage(), 0.9)?.toByteArray()
                                    ?.let { imageData ->
                                        continuation.resume(ImageCaptureResult.Success(imageData))
                                    } ?: run {
                                    continuation.resume(ImageCaptureResult.Error(Exception("JPEG conversion failed")))
                                }
                            }

                            ImageFormat.PNG -> {
                                UIImagePNGRepresentation(image.toUIImage())?.toByteArray()
                                    ?.let { imageData ->
                                        continuation.resume(ImageCaptureResult.Success(imageData))
                                    } ?: run {
                                    continuation.resume(ImageCaptureResult.Error(Exception("PNG conversion failed")))
                                }
                            }

                            else -> {
                                continuation.resume(ImageCaptureResult.Error(Exception("Unsupported format")))
                            }
                        }
                    }
                } else {
                    continuation.resume(ImageCaptureResult.Error(Exception("Capture failed - null image")))
                }
            } finally {
                lastCaptureTime = NSDate.date().timeIntervalSince1970()
                isCapturing.compareAndSet(expect = true, update = false)
                customCameraController.onPhotoCapture = null
            }
        }

        continuation.invokeOnCancellation {
            customCameraController.onPhotoCapture = null
            isCapturing.compareAndSet(expect = true, update = false)
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

    actual fun setTorchMode(mode: TorchMode) {
        customCameraController.setTorchMode(mode.toAVCaptureTorchMode())
    }

    actual fun toggleCameraLens() {
        customCameraController.switchCamera()
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


    private fun FlashMode.toAVCaptureFlashMode(): AVCaptureFlashMode = when (this) {
        FlashMode.ON -> AVCaptureFlashModeOn
        FlashMode.OFF -> AVCaptureFlashModeOff
        FlashMode.AUTO -> AVCaptureFlashModeAuto
    }


    private fun TorchMode.toAVCaptureTorchMode(): AVCaptureTorchMode = when (this) {
        TorchMode.ON -> AVCaptureTorchModeOn
        TorchMode.OFF -> AVCaptureTorchModeOff
        TorchMode.AUTO -> AVCaptureTorchModeAuto
    }
}


