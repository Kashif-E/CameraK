package com.kashif.cameraK

import platform.AVFoundation.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.*
import kotlinx.cinterop.*

class CustomCameraController : NSObject(), AVCapturePhotoCaptureDelegateProtocol {

    private var captureSession: AVCaptureSession? = null
    private var backCamera: AVCaptureDevice? = null
    private var frontCamera: AVCaptureDevice? = null
    var currentCamera: AVCaptureDevice? = null
    private var photoOutput: AVCapturePhotoOutput? = null
    var cameraPreviewLayer: AVCaptureVideoPreviewLayer? = null

    private var isUsingFrontCamera = false

    var onPhotoCapture: ((UIImage?) -> Unit)? = null
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
            val input = AVCaptureDeviceInput.deviceInputWithDevice(backCamera!!, null) as AVCaptureDeviceInput
            if (captureSession?.canAddInput(input) == true) {
                captureSession?.addInput(input)
            }
        } catch (e: Exception) {
            onError?.let { it(e) }
        }
    }

    fun startSession() {
        if (captureSession?.isRunning() == false) {
            captureSession?.startRunning()
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
            val newInput = AVCaptureDeviceInput.deviceInputWithDevice(currentCamera!!, null) as AVCaptureDeviceInput
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
        val image = imageData?.let { UIImage(data = it) }
        onPhotoCapture?.invoke(image)
    }
}


class CameraViewController : UIViewController(nibName = null, bundle = null) {

    private lateinit var cameraController: CustomCameraController

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


    fun captureImage() {
        cameraController.captureImage()
    }


    fun switchCamera() {
        cameraController.switchCamera()
    }


    fun setFlashMode(mode: AVCaptureFlashMode) {
        cameraController.setFlashMode(mode)
    }


    private fun configureCameraCallbacks() {
        cameraController.onPhotoCapture = { image ->

            println("Photo captured: $image")
        }

        cameraController.onError = { error ->

            println("Camera Error: $error")
        }
    }
}