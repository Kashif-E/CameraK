package com.kashif.cameraK.controller

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDevicePositionUnspecified
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureFlashMode
import platform.AVFoundation.AVCaptureFlashModeAuto
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.position
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue

class CustomCameraController : NSObject(), AVCapturePhotoCaptureDelegateProtocol {

    var captureSession: AVCaptureSession? = null
    private var backCamera: AVCaptureDevice? = null
    private var frontCamera: AVCaptureDevice? = null
    var currentCamera: AVCaptureDevice? = null
    private var photoOutput: AVCapturePhotoOutput? = null
    var cameraPreviewLayer: AVCaptureVideoPreviewLayer? = null

    private var isUsingFrontCamera = false

    var onPhotoCapture: ((NSData?) -> Unit)? = null
    var onError: ((Exception) -> Unit)? = null

    // Flash mode: Can be auto, on, or off
    var flashMode: AVCaptureFlashMode = AVCaptureFlashModeAuto


    fun setupSession() {
        captureSession = AVCaptureSession()
        captureSession?.beginConfiguration()

        setupInputs()

        photoOutput = AVCapturePhotoOutput()
        photoOutput?.setHighResolutionCaptureEnabled(true)
        captureSession?.addOutput(photoOutput!!)

        captureSession?.commitConfiguration()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupInputs() {

        val availableDevices = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
            AVMediaTypeVideo,
            AVCaptureDevicePositionUnspecified
        ).devices

        for (device in availableDevices) {
            when ((device as AVCaptureDevice).position) {
                AVCaptureDevicePositionBack -> backCamera = device
                AVCaptureDevicePositionFront -> frontCamera = device
            }
        }

        currentCamera = backCamera


        try {
            val input = AVCaptureDeviceInput.deviceInputWithDevice(
                backCamera!!,
                null
            ) as AVCaptureDeviceInput
            if (captureSession?.canAddInput(input) == true) {
                captureSession?.addInput(input)
            }
        } catch (e: Exception) {
            onError?.let { it(e) }
        }
    }

    fun startSession() {
        if (captureSession?.isRunning() == false) {
            dispatch_async(
                dispatch_get_global_queue(
                    DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(),
                    0u
                )
            ) {
                captureSession?.startRunning()
            }
        }
    }

    fun stopSession() {
        if (captureSession?.isRunning() == true) {
            captureSession?.stopRunning()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun setupPreviewLayer(view: UIView) {
        captureSession?.let { captureSession ->
            cameraPreviewLayer = AVCaptureVideoPreviewLayer(session = captureSession)
            cameraPreviewLayer?.videoGravity = AVLayerVideoGravityResizeAspectFill
            cameraPreviewLayer?.setFrame(view.bounds)
            view.layer.addSublayer(cameraPreviewLayer!!)
        }

    }


    fun setFlashMode(mode: AVCaptureFlashMode) {
        flashMode = mode
    }


    fun captureImage() {
        val settings = AVCapturePhotoSettings()
        settings.flashMode = flashMode
        settings.isHighResolutionPhotoEnabled()
        photoOutput?.capturePhotoWithSettings(settings, delegate = this)
    }


    @OptIn(ExperimentalForeignApi::class)
    fun switchCamera() {
        captureSession?.beginConfiguration()


        val currentInput = captureSession?.inputs?.first() as? AVCaptureDeviceInput
        if (currentInput != null) {
            captureSession?.removeInput(currentInput)
        }


        isUsingFrontCamera = !isUsingFrontCamera
        currentCamera = if (isUsingFrontCamera) frontCamera else backCamera


        try {
            val newInput = AVCaptureDeviceInput.deviceInputWithDevice(
                currentCamera!!,
                null
            ) as AVCaptureDeviceInput
            if (captureSession?.canAddInput(newInput) == true) {
                captureSession?.addInput(newInput)
            }
        } catch (e: Exception) {
            onError?.invoke(e)
        }


        val connection = cameraPreviewLayer?.connection
        if (connection?.isVideoMirroringSupported() == true) {
            connection.automaticallyAdjustsVideoMirroring = false
            connection.setVideoMirrored(isUsingFrontCamera)
        }

        captureSession?.commitConfiguration()
    }

    // AVCapturePhotoCaptureDelegate
    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
    ) {
        if (error != null) {
            onError?.invoke(Exception(error.localizedDescription))
            return
        }

        val imageData = didFinishProcessingPhoto.fileDataRepresentation()

        onPhotoCapture?.invoke(imageData)
    }
}
