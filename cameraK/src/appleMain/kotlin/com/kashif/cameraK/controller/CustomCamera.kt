package com.kashif.cameraK.controller

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.UIKit.UIView
import platform.darwin.*

class CustomCameraController : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
    var captureSession: AVCaptureSession? = null
    private var backCamera: AVCaptureDevice? = null
    private var frontCamera: AVCaptureDevice? = null
    private var currentCamera: AVCaptureDevice? = null
    private var photoOutput: AVCapturePhotoOutput? = null
    var cameraPreviewLayer: AVCaptureVideoPreviewLayer? = null
    private var isUsingFrontCamera = false


    var onPhotoCapture: ((NSData?) -> Unit)? = null
    var onError: ((CameraException) -> Unit)? = null

    var flashMode: AVCaptureFlashMode = AVCaptureFlashModeAuto
    var torchMode: AVCaptureTorchMode = AVCaptureTorchModeAuto

    sealed class CameraException : Exception() {
        class DeviceNotAvailable : CameraException()
        class ConfigurationError(message: String) : CameraException()
        class CaptureError(message: String) : CameraException()
    }

    fun setupSession() {
        try {
            captureSession = AVCaptureSession()
            captureSession?.beginConfiguration()

            if (!setupInputs()) {
                throw CameraException.DeviceNotAvailable()
            }

            setupPhotoOutput()
            captureSession?.commitConfiguration()
        } catch (e: CameraException) {
            cleanupSession()
            onError?.invoke(e)
        }
    }

    private fun setupPhotoOutput() {
        photoOutput = AVCapturePhotoOutput()
        photoOutput?.setHighResolutionCaptureEnabled(true)
        if (captureSession?.canAddOutput(photoOutput!!) == true) {
            captureSession?.addOutput(photoOutput!!)
        } else {
            throw CameraException.ConfigurationError("Cannot add photo output")
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupInputs(): Boolean {
        val availableDevices = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
            AVMediaTypeVideo,
            AVCaptureDevicePositionUnspecified
        ).devices

        if (availableDevices.isEmpty()) return false

        for (device in availableDevices) {
            when ((device as AVCaptureDevice).position) {
                AVCaptureDevicePositionBack -> backCamera = device
                AVCaptureDevicePositionFront -> frontCamera = device
            }
        }

        currentCamera = backCamera ?: frontCamera ?: return false

        try {
            val input = AVCaptureDeviceInput.deviceInputWithDevice(
                currentCamera!!,
                null
            ) ?: return false

            if (captureSession?.canAddInput(input) == true) {
                captureSession?.addInput(input)
                return true
            }
        } catch (e: Exception) {
            throw CameraException.ConfigurationError(e.message ?: "Unknown error")
        }
        return false
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

    fun cleanupSession() {
        stopSession()
        cameraPreviewLayer?.removeFromSuperlayer()
        cameraPreviewLayer = null
        captureSession = null
        photoOutput = null
        currentCamera = null
        backCamera = null
        frontCamera = null
    }

    @OptIn(ExperimentalForeignApi::class)
    fun setupPreviewLayer(view: UIView) {
        captureSession?.let { session ->
            cameraPreviewLayer = AVCaptureVideoPreviewLayer(session = session)
            cameraPreviewLayer?.videoGravity = AVLayerVideoGravityResizeAspectFill
            cameraPreviewLayer?.setFrame(view.bounds)
            view.layer.addSublayer(cameraPreviewLayer ?: return)
        }
    }

    fun setFlashMode(mode: AVCaptureFlashMode) {
        flashMode = mode
    }

    @OptIn(ExperimentalForeignApi::class)
    fun setTorchMode(mode: AVCaptureTorchMode) {
        torchMode = mode
        currentCamera?.let { camera ->
            if (camera.hasTorch) {
                try {
                    camera.lockForConfiguration(null)
                    camera.torchMode = mode
                    camera.unlockForConfiguration()
                } catch (e: Exception) {
                    onError?.invoke(CameraException.ConfigurationError("Failed to set torch mode"))
                }
            }
        }
    }

    fun captureImage() {
        val settings = AVCapturePhotoSettings()
        settings.flashMode = flashMode
        settings.isHighResolutionPhotoEnabled()
        photoOutput?.capturePhotoWithSettings(settings, delegate = this)
    }

    @OptIn(ExperimentalForeignApi::class)
    fun switchCamera() {
        guard(captureSession != null) { return@guard }

        captureSession?.beginConfiguration()

        try {
            // Remove existing input
            captureSession?.inputs?.firstOrNull()?.let { input ->
                captureSession?.removeInput(input as AVCaptureInput)
            }

            // Switch camera
            isUsingFrontCamera = !isUsingFrontCamera
            currentCamera = if (isUsingFrontCamera) frontCamera else backCamera

            // Validate camera availability
            val newCamera = currentCamera ?: throw CameraException.DeviceNotAvailable()

            // Add new input
            val newInput = AVCaptureDeviceInput.deviceInputWithDevice(
                newCamera,
                null
            ) ?: throw CameraException.ConfigurationError("Failed to create input")

            if (captureSession?.canAddInput(newInput) == true) {
                captureSession?.addInput(newInput)
            } else {
                throw CameraException.ConfigurationError("Cannot add input")
            }

            // Update preview layer mirroring
            cameraPreviewLayer?.connection?.let { connection ->
                if (connection.isVideoMirroringSupported()) {
                    connection.automaticallyAdjustsVideoMirroring = false
                    connection.setVideoMirrored(isUsingFrontCamera)
                }
            }

            captureSession?.commitConfiguration()
        } catch (e: CameraException) {
            captureSession?.commitConfiguration()
            onError?.invoke(e)
        } catch (e: Exception) {
            captureSession?.commitConfiguration()
            onError?.invoke(CameraException.ConfigurationError(e.message ?: "Unknown error"))
        }
    }

    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
    ) {
        if (error != null) {
            onError?.invoke(CameraException.CaptureError(error.localizedDescription))
            return
        }

        val imageData = didFinishProcessingPhoto.fileDataRepresentation()
        onPhotoCapture?.invoke(imageData)
    }

    private inline fun guard(condition: Boolean, crossinline block: () -> Unit) {
        if (!condition) block()
    }
}