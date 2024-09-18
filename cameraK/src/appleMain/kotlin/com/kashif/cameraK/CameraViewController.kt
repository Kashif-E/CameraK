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

    // Output handling closures
    var onPhotoCapture: ((UIImage?) -> Unit)? = null
    var onError: ((Exception) -> Unit)? = null

    // Flash mode: Can be auto, on, or off
    var flashMode: AVCaptureFlashMode = AVCaptureFlashModeAuto


    fun setupSession() {
        captureSession = AVCaptureSession()
        captureSession?.beginConfiguration()

        // Setup inputs
        setupInputs()

        // Setup photo output
        photoOutput = AVCapturePhotoOutput()
        photoOutput?.setHighResolutionCaptureEnabled(true)
        captureSession?.addOutput(photoOutput!!)

        captureSession?.commitConfiguration()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupInputs() {
        // Find the back and front cameras
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

        // Set the back camera as default
        currentCamera = backCamera

        // Add input to session
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

    // Flash Handling
    fun setFlashMode(mode: AVCaptureFlashMode) {
        flashMode = mode
    }

    // Capture Image
    fun captureImage() {
        val settings = AVCapturePhotoSettings()
        settings.flashMode = flashMode
        settings.isHighResolutionPhotoEnabled()
        photoOutput?.capturePhotoWithSettings(settings, delegate = this)
    }

    // Switch Camera
    @OptIn(ExperimentalForeignApi::class)
    fun switchCamera() {
        captureSession?.beginConfiguration()

        // Remove current input
        val currentInput = captureSession?.inputs?.first() as? AVCaptureDeviceInput
        if (currentInput != null) {
            captureSession?.removeInput(currentInput)
        }

        // Toggle between front and back camera
        isUsingFrontCamera = !isUsingFrontCamera
        currentCamera = if (isUsingFrontCamera) frontCamera else backCamera

        // Add new input
        try {
            val newInput = AVCaptureDeviceInput.deviceInputWithDevice(currentCamera!!, null) as AVCaptureDeviceInput
            if (captureSession?.canAddInput(newInput) == true) {
                captureSession?.addInput(newInput)
            }
        } catch (e: Exception) {
            onError?.invoke(e)
        }

        // Adjust connection settings for mirroring
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

    // MARK: - Camera Functionality Exposure

    // Exposes a method to capture the image externally
    fun captureImage() {
        cameraController.captureImage()
    }

    // Exposes a method to switch the camera externally
    fun switchCamera() {
        cameraController.switchCamera()
    }

    // Exposes a method to control the flash mode externally
    fun setFlashMode(mode: AVCaptureFlashMode) {
        cameraController.setFlashMode(mode)
    }

    // Optionally, you can handle the photo capture callback and error handling
    private fun configureCameraCallbacks() {
        cameraController.onPhotoCapture = { image ->
            // Handle the captured image as needed, or pass it to another view controller
            // For example, you could notify another component that the photo has been taken
            println("Photo captured: $image")
        }

        cameraController.onError = { error ->
            // Handle the error, or notify another component
            println("Camera Error: $error")
        }
    }
}