package com.kashif.cameraK.controller

import com.kashif.cameraK.capture.BurstCaptureManager
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.QualityPrioritization
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.plugins.CameraPlugin
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.utils.MemoryManager
import com.kashif.cameraK.utils.toByteArray
import com.kashif.cameraK.utils.toUIImage
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
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
import platform.Foundation.NSData
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIViewController
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.DISPATCH_QUEUE_PRIORITY_HIGH
import kotlin.coroutines.resume

actual class CameraController(
    internal var flashMode: FlashMode,
    internal var torchMode: TorchMode,
    internal var cameraLens: CameraLens,
    internal var imageFormat: ImageFormat,
    internal var qualityPriority: QualityPrioritization,
    internal var directory: Directory,
    internal var plugins: MutableList<CameraPlugin>
) : UIViewController(null, null) {
    private var isCapturing = atomic(false)
    private val customCameraController = CustomCameraController(qualityPriority)
    private var imageCaptureListeners = mutableListOf<(ByteArray) -> Unit>()
    private var metadataOutput = AVCaptureMetadataOutput()
    private var metadataObjectsDelegate: AVCaptureMetadataOutputObjectsDelegateProtocol? = null


    private val memoryManager = MemoryManager
    private val burstCaptureManager = BurstCaptureManager()

    override fun viewDidLoad() {
        super.viewDidLoad()

        memoryManager.initialize()
        setupCamera()


        burstCaptureManager.setQueueReadyListener {
            if (customCameraController.captureSession?.isRunning() == true) {
                val quality = burstCaptureManager.getOptimalQuality()
                customCameraController.captureImage(quality)
            }
        }
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        memoryManager.updateMemoryStatus()
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)

        memoryManager.clearBufferPools()
        burstCaptureManager.reset()
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
                processImageCapture(it)
            }
        }

        customCameraController.onError = { error ->
            println("Camera Error: $error")
        }
    }


    private fun processImageCapture(imageData: NSData) {

        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0u)) {
            autoreleasepool {

                memoryManager.updateMemoryStatus()

                try {

                    val estimatedSize = imageData.length.toInt()
                    val buffer = if (estimatedSize > 0) {
                        memoryManager.getBuffer(estimatedSize)
                    } else {

                        ByteArray(imageData.length.toInt())
                    }


                    val data = imageData.toByteArray(reuseBuffer = buffer)


                    dispatch_async(dispatch_get_main_queue()) {
                        imageCaptureListeners.forEach { it(data) }
                    }


                    if (buffer.size >= estimatedSize) {
                        memoryManager.recycleBuffer(buffer)
                    }

                } catch (e: Exception) {
                    println("Error processing image data: ${e.message}")
                }
            }
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

        if (!isCapturing.compareAndSet(expect = false, update = true)) {
            continuation.resume(ImageCaptureResult.Error(Exception("Capture in progress")))
            return@suspendCancellableCoroutine
        }


        memoryManager.updateMemoryStatus()

        val captureHandler = object {
            var completed = false

            fun process(image: NSData?, error: String?) {
                if (completed) return
                completed = true

                if (image != null) {

                    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0u)) {
                        try {
                            autoreleasepool {

                                val quality = if (memoryManager.isUnderMemoryPressure()) 0.6 else 0.9

                                val result = when (imageFormat) {
                                    ImageFormat.JPEG -> {
                                        UIImageJPEGRepresentation(image.toUIImage(), quality)?.toByteArray()?.let {
                                            ImageCaptureResult.Success(it)
                                        }
                                    }
                                    ImageFormat.PNG -> {
                                        UIImagePNGRepresentation(image.toUIImage())?.toByteArray()?.let {
                                            ImageCaptureResult.Success(it)
                                        }
                                    }
                                    else -> null
                                }


                                dispatch_async(dispatch_get_main_queue()) {
                                    isCapturing.value = false
                                    continuation.resume(result ?: ImageCaptureResult.Error(Exception("Image processing failed")))
                                }
                            }
                        } catch (e: Exception) {
                            dispatch_async(dispatch_get_main_queue()) {
                                isCapturing.value = false
                                continuation.resume(ImageCaptureResult.Error(e))
                            }
                        }
                    }
                } else {
                    isCapturing.value = false
                    continuation.resume(ImageCaptureResult.Error(Exception(error ?: "Capture failed")))
                }
            }
        }

        customCameraController.onPhotoCapture = { image ->
            captureHandler.process(image, null)
        }

        customCameraController.onError = { error ->
            captureHandler.process(null, error.toString())
        }

        continuation.invokeOnCancellation {
            captureHandler.process(null, "Capture cancelled")
        }


        val requested = burstCaptureManager.requestCapture(
            captureFunction = {

                val quality = burstCaptureManager.getOptimalQuality()
                customCameraController.captureImage(quality)
            },
            onComplete = {

            }
        )

        if (!requested) {
            isCapturing.value = false
            continuation.resume(ImageCaptureResult.Error(Exception("Too many captures in progress")))
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

    actual fun setFlashMode(mode: FlashMode) {
        flashMode = mode
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
        torchMode = mode
        customCameraController.setTorchMode(mode.toAVCaptureTorchMode())
    }

    actual fun toggleCameraLens() {

        memoryManager.updateMemoryStatus()


        if (memoryManager.isUnderMemoryPressure()) {
            memoryManager.clearBufferPools()
        }

        customCameraController.switchCamera()
    }

    actual fun startSession() {

        memoryManager.clearBufferPools()
        customCameraController.startSession()
        initializeControllerPlugins()
    }

    actual fun stopSession() {
        customCameraController.stopSession()

        burstCaptureManager.reset()
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