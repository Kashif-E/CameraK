package com.kashif.cameraK.controller

import com.kashif.cameraK.enums.*
import com.kashif.cameraK.plugins.CameraPlugin
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.utils.InvalidConfigurationException
import com.kashif.cameraK.utils.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.sync.Mutex
import platform.AVFoundation.*
import platform.Foundation.NSError
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.experimental.ExperimentalObjCRefinement

class IosCameraController(
    internal var flashMode: FlashMode,
    internal var cameraLens: CameraLens,
    internal var rotation: Rotation,
    internal var imageFormat: ImageFormat,
    internal var directory: Directory,
    internal var plugins: MutableList<CameraPlugin>
) :  CameraController, UIViewController(null, null) {

    private val session = AVCaptureSession()
    private var photoOutput = AVCapturePhotoOutput()
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var imageCaptureListeners = mutableListOf<(ByteArray) -> Unit>()
    private var metadataOutput = AVCaptureMetadataOutput()
    private var metadataObjectsDelegate: AVCaptureMetadataOutputObjectsDelegateProtocol? = null
    private val mutex = Mutex()

    override fun viewDidLoad() {
        super.viewDidLoad()
        setupCamera()
        initializePlugins()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupCamera() {
        session.beginConfiguration()

        val device = selectCameraDevice(cameraLens)
            ?: throw InvalidConfigurationException("Camera device not found.")

        val input = try {
            AVCaptureDeviceInput.deviceInputWithDevice(device, null)
        } catch (e: Exception) {
            throw InvalidConfigurationException("Failed to create camera input: ${e.message}")
        }

        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
        } else {
            throw InvalidConfigurationException("Cannot add camera input to session.")
        }

        if (session.canAddOutput(photoOutput)) {
            session.addOutput(photoOutput)
        } else {
            throw InvalidConfigurationException("Cannot add photo output to session.")
        }

        if (session.canAddOutput(metadataOutput)) {
            session.addOutput(metadataOutput)
            metadataObjectsDelegate?.let { delegate ->
                metadataOutput.setMetadataObjectsDelegate(delegate, null)
            }
            metadataOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
        }

        session.commitConfiguration()

        previewLayer =
            AVCaptureVideoPreviewLayer.layerWithSession(session) as? AVCaptureVideoPreviewLayer
        previewLayer?.videoGravity = AVLayerVideoGravityResizeAspectFill
        view.layer.addSublayer(previewLayer!!)
    }

    private fun selectCameraDevice(lens: CameraLens): AVCaptureDevice? {
        val position = when (lens) {
            CameraLens.FRONT -> AVCaptureDevicePositionFront
            CameraLens.BACK -> AVCaptureDevicePositionBack
        }
        val discoverySession =
            AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
                deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
                mediaType = AVMediaTypeVideo,
                position = position
            )
        return discoverySession.devices().firstOrNull() as AVCaptureDevice?
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    override suspend fun takePicture(): ImageCaptureResult = suspendCoroutine { cont ->
        val settings = AVCapturePhotoSettings.photoSettings()
        settings.flashMode = flashMode.toAVCaptureFlashMode()
        settings.setHighResolutionPhotoEnabled(true)

        photoOutput.capturePhotoWithSettings(
            settings,
            PhotoCaptureDelegate(imageCaptureListeners, cont, imageFormat)
        )
    }



    override fun toggleFlashMode() {
        flashMode = when (flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
    }

    override fun toggleCameraLens() {
        val newLens = if (cameraLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        cameraLens = newLens

        session.beginConfiguration()
        session.inputs().forEach { session.removeInput(it as AVCaptureInput) }

        val newDevice = selectCameraDevice(newLens)
            ?: throw InvalidConfigurationException("Camera device not found.")

        val newInput = try {
            AVCaptureDeviceInput.deviceInputWithDevice(newDevice, null)
        } catch (e: Exception) {
            throw InvalidConfigurationException("Failed to create camera input: ${e.message}")
        }

        if (newInput != null && session.canAddInput(newInput)) {
            session.addInput(newInput)
        }

        previewLayer?.connection()?.setVideoOrientation(rotation.toAVCaptureVideoOrientation())
        session.commitConfiguration()
    }

    override fun setCameraRotation(rotation: Rotation) {
        this.rotation = rotation
        previewLayer?.connection()?.setVideoOrientation(rotation.toAVCaptureVideoOrientation())
    }

    override fun startSession() {
        if (!session.isRunning()) {
            session.startRunning()
        }
    }

    override fun stopSession() {
        if (session.isRunning()) {
            session.stopRunning()
        }
    }

    override fun addImageCaptureListener(listener: (ByteArray) -> Unit) {
        imageCaptureListeners.add(listener)
    }

    fun setMetadataObjectsDelegate(delegate: AVCaptureMetadataOutputObjectsDelegateProtocol) {
        metadataObjectsDelegate = delegate
        metadataOutput.setMetadataObjectsDelegate(delegate, null)
    }

    override fun initializePlugins() {
        plugins.forEach {
            it.initialize(this)
        }
    }

    fun getSession(): AVCaptureSession {
        return session
    }

    private fun FlashMode.toAVCaptureFlashMode(): AVCaptureFlashMode = when (this) {
        FlashMode.ON -> AVCaptureFlashModeOn
        FlashMode.OFF -> AVCaptureFlashModeOff
        FlashMode.AUTO -> AVCaptureFlashModeAuto
    }

    private fun Rotation.toAVCaptureVideoOrientation(): AVCaptureVideoOrientation = when (this) {
        Rotation.ROTATION_0 -> AVCaptureVideoOrientationPortrait
        Rotation.ROTATION_90 -> AVCaptureVideoOrientationLandscapeRight
        Rotation.ROTATION_180 -> AVCaptureVideoOrientationPortraitUpsideDown
        Rotation.ROTATION_270 -> AVCaptureVideoOrientationLandscapeLeft
    }

    private class PhotoCaptureDelegate(
        private val listeners: List<(ByteArray) -> Unit>,
        private val cont: kotlin.coroutines.Continuation<ImageCaptureResult>,
        private val imageFormat: ImageFormat
    ) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
        override fun captureOutput(
            output: AVCapturePhotoOutput,
            didFinishProcessingPhoto: AVCapturePhoto,
            error: NSError?
        ) {
            if (error != null) {
                cont.resume(ImageCaptureResult.Error(Exception("Photo capture error: ${error.localizedDescription}")))
                return
            }

            val imageData = didFinishProcessingPhoto.fileDataRepresentation()
            if (imageData == null) {
                cont.resume(ImageCaptureResult.Error(Exception("Failed to get image data.")))
                return
            }

            val byteArray = imageData.toByteArray()
            listeners.forEach { it(byteArray) }
            cont.resume(ImageCaptureResult.Success(byteArray))
        }
    }
}
