package com.kashif.cameraK

import kotlinx.cinterop.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.*
import platform.Foundation.*
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import kotlin.coroutines.resume


actual class CameraController : UIViewController(nibName = null, bundle = null) {
    actual var cameraState: MutableStateFlow<CameraState> = MutableStateFlow(CameraState())
    private lateinit var cameraController: CustomCameraController

    override fun viewDidLoad() {
        super.viewDidLoad()

        // Initialize camera controller
        cameraController = CustomCameraController()
        cameraController.setupSession()

        // Set the preview layer to a full-screen view
        cameraController.setupPreviewLayer(view)

        // Start the session after everything is set up
        cameraController.startSession()

        // Configure the callbacks if needed
        configureCameraCallbacks()
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        cameraController.cameraPreviewLayer?.setFrame(view.bounds)
    }


    actual suspend fun takePicture(imageFormat: ImageFormat): ImageCaptureResult {
        return suspendCancellableCoroutine { continuation ->
            cameraController.onPhotoCapture = { image ->
                if (image != null) {
                    val imageData = UIImageJPEGRepresentation(image, 0.9)
                    val byteArray = UIImage(data = imageData!!).toByteArray()
                    continuation.resume(ImageCaptureResult.Success(byteArray, "captured_image.jpg"))
                } else {
                    continuation.resume(ImageCaptureResult.Error(Exception("Failed to capture image")))
                }
            }
            cameraController.captureImage()
        }
    }

    actual fun savePicture(name: String, file: ByteArray, directory: Directory) {
        val fileManager = NSFileManager.defaultManager
        val paths = fileManager.URLsForDirectory(NSDocumentDirectory, inDomains = NSUserDomainMask)
        val documentsDirectory = paths.first() as NSURL
        val fileURL = documentsDirectory.URLByAppendingPathComponent(name)

        try {
            val nsData = file.toNSData()
            fileURL.let { nsData.writeToURL(it!!, atomically = true) }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun changeFlashMode(flashMode: FlashMode) {
        val mode = when (flashMode) {
            FlashMode.ON -> AVCaptureFlashModeOn
            FlashMode.OFF -> AVCaptureFlashModeOff
        }
        cameraController.setFlashMode(mode)
        cameraState.update { it.copy(flashMode = flashMode) }
    }

    actual fun changeCameraLens(lens: CameraLens) {

        cameraController.switchCamera()
        cameraState.update { it.copy(cameraLens = lens) }
    }

    actual fun getFlashMode(): Int {
        return when (cameraController.flashMode) {
            AVCaptureFlashModeOn -> FlashMode.ON.value
            AVCaptureFlashModeOff -> FlashMode.OFF.value
            else -> FlashMode.OFF.value

        }
    }

    actual fun getCameraLens(): Int {
        return when (cameraController.currentCamera?.position) {
            AVCaptureDevicePositionFront -> CameraLens.FRONT.value
            AVCaptureDevicePositionBack -> CameraLens.BACK.value
            else -> CameraLens.BACK.value
        }
    }

    actual fun getCameraRotation(): Int {
        return when (cameraController.cameraPreviewLayer?.connection?.videoOrientation) {
            AVCaptureVideoOrientationPortrait -> Rotation.ROTATION_0.value
            AVCaptureVideoOrientationLandscapeRight -> Rotation.ROTATION_90.value
            AVCaptureVideoOrientationPortraitUpsideDown -> Rotation.ROTATION_180.value
            AVCaptureVideoOrientationLandscapeLeft -> Rotation.ROTATION_270.value
            else -> Rotation.ROTATION_0.value
        }
    }

    actual fun setCameraRotation(rotation: Rotation) {
        val orientation = when (rotation) {
            Rotation.ROTATION_0 -> AVCaptureVideoOrientationPortrait
            Rotation.ROTATION_90 -> AVCaptureVideoOrientationLandscapeRight
            Rotation.ROTATION_180 -> AVCaptureVideoOrientationPortraitUpsideDown
            Rotation.ROTATION_270 -> AVCaptureVideoOrientationLandscapeLeft
        }
        cameraController.cameraPreviewLayer?.connection?.videoOrientation = orientation
        cameraState.update { it.copy(rotation = rotation) }
    }

    private fun configureCameraCallbacks() {
        cameraController.onPhotoCapture = { image ->
            println("Photo captured: $image")
        }

        cameraController.onError = { error ->
            println("Camera Error: $error")
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun ByteArray.toNSData(): NSData = usePinned {
        NSData.create(bytes = it.addressOf(0), length = size.toULong())
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun UIImage.toByteArray(): ByteArray {
        val imageData = UIImageJPEGRepresentation(this, 0.9) ?: throw IllegalArgumentException("image data is null")
        val bytes = imageData.bytes?.reinterpret<ByteVar>()
        return ByteArray(imageData.length.toInt()) { i -> bytes!![i] }
    }
}

