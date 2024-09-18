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
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHPhotoLibrary
import platform.UIKit.*

actual class CameraController : UIViewController(nibName = null, bundle = null) {

    private lateinit var cameraController: CustomCameraController

    // Variables to store the current camera settings
    private var currentFlashMode: FlashMode = FlashMode.OFF
    private var currentCameraLens: CameraLens = CameraLens.BACK
    private var currentRotation: Rotation = Rotation.ROTATION_0

    override fun viewDidLoad() {
        super.viewDidLoad()
        cameraController = CustomCameraController()
        cameraController.setupSession()
        cameraController.setupPreviewLayer(view)
        cameraController.startSession()
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
                    when (imageFormat) {
                        ImageFormat.JPEG -> {
                            UIImageJPEGRepresentation(image, 0.9)?.toByteArray()?.let { imageData ->
                                continuation.resume(
                                    ImageCaptureResult.Success(
                                        imageData,
                                        "${NSDate().timeIntervalSince1970 * 1000}.jpg"
                                    )
                                )

                            }
                        }

                        ImageFormat.PNG -> {
                            UIImagePNGRepresentation(image)?.toByteArray()?.let { imageData ->
                                continuation.resume(
                                    ImageCaptureResult.Success(
                                        imageData,
                                        "${NSDate().timeIntervalSince1970 * 1000}.png"
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
            cameraController.captureImage()
        }
    }


    actual fun savePicture(fileName: String, fileData: ByteArray, directory: Directory) {
        val fileManager = NSFileManager.defaultManager
        val urls = fileManager.URLsForDirectory(directory.toNSSearchPathDirectory(), inDomains = NSUserDomainMask)
        val documentsDirectory = urls.first() as? NSURL

        documentsDirectory?.let {
            val fileURL = it.URLByAppendingPathComponent(fileName)
            val nsData = fileData.toNSData()

            val success = nsData.writeToURL(fileURL!!, atomically = true)
            if (!success) {
                println("Failed to save image to ($fileURL)")
            }
        }
    }

    actual fun toggleFlashMode() {
        currentFlashMode = if (currentFlashMode == FlashMode.OFF) FlashMode.ON else FlashMode.OFF
        val avFlashMode = currentFlashMode.toAVCaptureFlashMode()
        cameraController.setFlashMode(avFlashMode)
    }

    actual fun toggleCameraLens() {
        currentCameraLens = if (currentCameraLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        cameraController.switchCamera()
    }

    actual fun getFlashMode(): Int {
        return currentFlashMode.value
    }

    actual fun getCameraLens(): Int {
        return currentCameraLens.value
    }

    actual fun getCameraRotation(): Int {
        return currentRotation.value
    }

    actual fun setCameraRotation(rotation: Rotation) {
        currentRotation = rotation
        val orientation = rotation.toAVCaptureVideoOrientation()
        cameraController.cameraPreviewLayer?.connection?.videoOrientation = orientation
    }

    private fun configureCameraCallbacks() {
        cameraController.onError = { error ->
            println("Camera Error: $error")
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

    actual fun isPermissionGranted(): Boolean {
        return checkCameraPermission() && checkPhotoLibraryPermission()
    }

    actual fun bindCamera() {
        // Binding is handled in viewDidLoad
    }

    private fun checkCameraPermission(): Boolean {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        return status == AVAuthorizationStatusAuthorized
    }

    private fun checkPhotoLibraryPermission(): Boolean {
        val status = PHPhotoLibrary.authorizationStatus()
        return status == PHAuthorizationStatusAuthorized
    }

    private fun FlashMode.toAVCaptureFlashMode(): AVCaptureFlashMode {
        return when (this) {
            FlashMode.ON -> AVCaptureFlashModeOn
            FlashMode.OFF -> AVCaptureFlashModeOff
        }
    }

    private fun Rotation.toAVCaptureVideoOrientation(): AVCaptureVideoOrientation {
        return when (this) {
            Rotation.ROTATION_0 -> AVCaptureVideoOrientationPortrait
            Rotation.ROTATION_90 -> AVCaptureVideoOrientationLandscapeRight
            Rotation.ROTATION_180 -> AVCaptureVideoOrientationPortraitUpsideDown
            Rotation.ROTATION_270 -> AVCaptureVideoOrientationLandscapeLeft
        }
    }

    private fun Directory.toNSSearchPathDirectory(): NSSearchPathDirectory {
        return when (this) {
            Directory.PICTURES -> NSDocumentDirectory
            Directory.DCIM -> NSPicturesDirectory
        }
    }
}