package com.kashif.cameraK

import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.*
import platform.CoreMedia.CMSampleBufferRef
import platform.Foundation.NSCoder
import platform.Foundation.NSError
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIView
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class CameraView : UIView(NSCoder()), AVCapturePhotoCaptureDelegateProtocol {
    private val captureSession = AVCaptureSession()
    private val photoOutput = AVCapturePhotoOutput()

    init {
        setupCamera()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupCamera() {
        val videoDevice = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        val videoDeviceInput = AVCaptureDeviceInput.deviceInputWithDevice(videoDevice!!, null) as AVCaptureDeviceInput
        captureSession.addInput(videoDeviceInput)
        captureSession.addOutput(photoOutput)

        val previewLayer = AVCaptureVideoPreviewLayer(session = captureSession).apply {
            frame = bounds
            videoGravity = AVLayerVideoGravityResizeAspectFill
        }
        layer.addSublayer(previewLayer)

        captureSession.startRunning()
    }


    @ExperimentalForeignApi
    suspend fun capturePhoto(): UIImage? = suspendCancellableCoroutine { continuation ->
        val settings = AVCapturePhotoSettings.photoSettings()
        photoOutput.capturePhotoWithSettings(settings, object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
            override fun captureOutput(
                output: AVCapturePhotoOutput,
                didFinishProcessingPhotoSampleBuffer: CMSampleBufferRef?,
                previewPhotoSampleBuffer: CMSampleBufferRef?,
                resolvedSettings: AVCaptureResolvedPhotoSettings,
                bracketSettings: AVCaptureBracketedStillImageSettings?,
                error: NSError?
            ) {
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return
                }

                val imageData = AVCapturePhotoOutput.JPEGPhotoDataRepresentationForJPEGSampleBuffer(
                    didFinishProcessingPhotoSampleBuffer, previewPhotoSampleBuffer
                )
                val image = UIImage(data = imageData!!)
                continuation.resume(image)
            }
        })
    }

    fun setFlashMode(flashMode: FlashMode) {
        when (flashMode) {
            FlashMode.OFF -> {
                photoOutput.capturePhotoWithSettings(AVCapturePhotoSettings().also {
                    it.flashMode = AVCaptureFlashModeOff
                }, delegate = this)
            }

            FlashMode.ON -> {
                photoOutput.capturePhotoWithSettings(AVCapturePhotoSettings().also {
                    it.flashMode = AVCaptureFlashModeOn
                }, delegate = this)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun setCameraLens(lens: CameraLens) {
        captureSession.beginConfiguration()
        captureSession.inputs.forEach { input ->
            captureSession.removeInput(input as AVCaptureInput)
        }

        val newDevice: AVCaptureDevice? = when (lens) {
            CameraLens.DEFAULT, CameraLens.BACK -> AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            CameraLens.FRONT -> AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo).first() as? AVCaptureDevice
            else -> null
        }

        newDevice?.let {
            val newInput = AVCaptureDeviceInput.deviceInputWithDevice(it, null) as AVCaptureDeviceInput
            captureSession.addInput(newInput)
        }

        captureSession.commitConfiguration()
        captureSession.startRunning()
    }

    fun setRotation(rotation: Rotation) {
        val orientation = when {
            (rotation == Rotation.ROTATION_0 || rotation == Rotation.ROTATION_270) -> AVCaptureVideoOrientationPortrait
            else -> AVCaptureVideoOrientationLandscapeRight
        }
        (layer as? AVCaptureVideoPreviewLayer)?.connection?.videoOrientation = orientation
    }
}

@OptIn(ExperimentalForeignApi::class)
fun UIImage.toByteArray(): ByteArray {
    val imageData = UIImageJPEGRepresentation(this, 0.3) ?: throw IllegalArgumentException("image data is null")
    val bytes = imageData.bytes ?: throw IllegalArgumentException("image bytes is null")
    val length = imageData.length

    val data: CPointer<ByteVar> = bytes.reinterpret()
    return ByteArray(length.toInt()) { index ->
        data[index]
    }
}