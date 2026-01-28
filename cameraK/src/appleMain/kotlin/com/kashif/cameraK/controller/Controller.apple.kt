package com.kashif.cameraK.controller

import com.kashif.cameraK.enums.AspectRatio
import com.kashif.cameraK.enums.CameraDeviceType
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
import com.kashif.cameraK.utils.fixOrientation
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
import platform.AVFoundation.AVCaptureOutput
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
import platform.Foundation.NSFileManager
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import kotlin.random.Random
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIViewController
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.DISPATCH_QUEUE_PRIORITY_HIGH
import platform.Photos.PHPhotoLibrary
import platform.Photos.PHAssetChangeRequest
import kotlin.coroutines.resume

actual class CameraController(
    internal var flashMode: FlashMode,
    internal var torchMode: TorchMode,
    internal var cameraLens: CameraLens,
    internal var imageFormat: ImageFormat,
    internal var qualityPriority: QualityPrioritization,
    internal var directory: Directory,
    internal var cameraDeviceType: CameraDeviceType,
    internal var aspectRatio: AspectRatio,
    internal var returnFilePath: Boolean,
    internal var plugins: MutableList<CameraPlugin>,
    internal var targetResolution: Pair<Int, Int>? = null
) : UIViewController(null, null) {
    private var isCapturing = atomic(false)
    private val customCameraController = CustomCameraController(
        qualityPrioritization = qualityPriority,
        initialCameraLens = cameraLens,
        aspectRatio = aspectRatio,
        targetResolution = targetResolution
    )
    private var imageCaptureListeners = mutableListOf<(ByteArray) -> Unit>()
    private var metadataOutput = AVCaptureMetadataOutput()
    private var metadataObjectsDelegate: AVCaptureMetadataOutputObjectsDelegateProtocol? = null


    private val memoryManager = MemoryManager
    private val pendingCaptures = atomic(0)
    private val maxConcurrentCaptures = 3

    override fun viewDidLoad() {
        super.viewDidLoad()

        memoryManager.initialize()
        setupCamera()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        memoryManager.updateMemoryStatus()
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)

        memoryManager.clearBufferPools()
    }

    fun getCameraPreviewLayer() = customCameraController.cameraPreviewLayer
    
    /**
     * Returns whether the capture session is ready for use.
     * Used by plugins to check if they can add outputs.
     */
    fun isSessionReady(): Boolean = customCameraController.captureSession != null

    /**
     * Queues a configuration change to be applied atomically (plugin outputs, etc).
     * Used by plugins (OCR, QRScanner) to safely add outputs without crashes.
     */
    fun queueConfigurationChange(change: () -> Unit) {
        customCameraController.queueConfigurationChange(change)
    }

    /**
     * Safely adds an output within the queued configuration block.
     */
    fun safeAddOutput(output: AVCaptureOutput) {
        customCameraController.safeAddOutput(output)
    }

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
        customCameraController.onSessionReady = {
            try {
                customCameraController.setupPreviewLayer(view)
            } catch (e: Exception) {
                platform.Foundation.NSLog("CameraK Error: setupPreviewLayer - ${e.message}")
            }

            try {
                if (customCameraController.captureSession?.canAddOutput(metadataOutput) == true) {
                    customCameraController.captureSession?.addOutput(metadataOutput)
                }
            } catch (e: Exception) {
                platform.Foundation.NSLog("CameraK Error: metadata output - ${e.message}")
            }

            startSession()
        }

        try {
            customCameraController.setupSession(cameraDeviceType)
        } catch (e: Exception) {
            platform.Foundation.NSLog("CameraK Error: setupSession - ${e.message}")
            return
        }

        customCameraController.onPhotoCapture = { image ->
            image?.let {
                processImageCapture(it)
            }
        }

        customCameraController.onError = { error ->
            platform.Foundation.NSLog("CameraK Error: ${error}")
        }
    }

    private fun writeDataToFile(data: NSData, filePath: String): Boolean {
        return NSFileManager.defaultManager.createFileAtPath(filePath, data, null)
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
                    platform.Foundation.NSLog("CameraK: Error processing image data: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }

    fun setMetadataObjectsDelegate(delegate: AVCaptureMetadataOutputObjectsDelegateProtocol) {
        metadataObjectsDelegate = delegate
        metadataOutput.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
    }

    fun updateMetadataObjectTypes(newTypes: List<String>) {
        // Note: This is called from queueConfigurationChange, so session may be in config mode
        // Don't check isRunning() - just set the types
        if (metadataOutput.availableMetadataObjectTypes.isNotEmpty()) {
            // Filter to only supported types
            val supportedTypes = newTypes.filter { type -> 
                metadataOutput.availableMetadataObjectTypes.contains(type)
            }
            metadataOutput.metadataObjectTypes = supportedTypes
        } else {
            // Metadata output not fully ready yet, try setting all requested types
            metadataOutput.metadataObjectTypes = newTypes
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        customCameraController.cameraPreviewLayer?.setFrame(view.bounds)
    }

    @Deprecated(
        message = "Use takePictureToFile() instead for better performance",
        replaceWith = ReplaceWith("takePictureToFile()"),
        level = DeprecationLevel.WARNING
    )
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun takePicture(): ImageCaptureResult = suspendCancellableCoroutine { continuation ->
        // Atomic counter rejection pattern
        if (pendingCaptures.incrementAndGet() > maxConcurrentCaptures) {
            pendingCaptures.decrementAndGet()
            platform.Foundation.NSLog("CameraK: Burst queue full, dropping frame (${pendingCaptures.value} in progress)")
            continuation.resume(ImageCaptureResult.Error(Exception("Burst queue full, capture rejected")))
            return@suspendCancellableCoroutine
        }

        if (!isCapturing.compareAndSet(expect = false, update = true)) {
            pendingCaptures.decrementAndGet()
            continuation.resume(ImageCaptureResult.Error(Exception("Capture in progress")))
            return@suspendCancellableCoroutine
        }

        // Update memory status before capture
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
                                // Constant quality (0.95 for JPEG) - no dynamic adjustment
                                val quality = 0.95

                                val result = if (returnFilePath) {
                                    // File path mode - behavior depends on directory setting
                                    try {
                                        val filePath = createTempFile()
                                        val writeSuccess = if (imageFormat == ImageFormat.JPEG) {
                                            // Write JPEG directly
                                            writeDataToFile(image, filePath)
                                        } else {
                                            // Convert to PNG and write
                                            val uiImage = image.toUIImage()
                                            val orientedImage = uiImage.fixOrientation()
                                            val pngData = UIImagePNGRepresentation(orientedImage)
                                            if (pngData != null) writeDataToFile(pngData, filePath) else false
                                        }
                                        
                                        if (writeSuccess) {
                                            when (directory) {
                                                Directory.PICTURES, Directory.DCIM -> {
                                                    // Save to Photos library and return asset identifier
                                                    val photosPath = saveToPhotosLibrary(filePath, image)
                                                    
                                                    // Clean up temp file
                                                    platform.Foundation.NSFileManager.defaultManager.removeItemAtPath(filePath, null)
                                                    
                                                    if (photosPath != null) {
                                                        ImageCaptureResult.SuccessWithFile(photosPath)
                                                    } else {
                                                        ImageCaptureResult.Error(Exception("Failed to save to Photos library"))
                                                    }
                                                }
                                                Directory.DOCUMENTS -> {
                                                    // Return direct file path (already saved to Documents)
                                                    ImageCaptureResult.SuccessWithFile(filePath)
                                                }
                                            }
                                        } else {
                                            ImageCaptureResult.Error(Exception("Failed to write image to file"))
                                        }
                                    } catch (e: Exception) {
                                        platform.Foundation.NSLog("CameraK: Error in file path mode: ${e.message ?: "Unknown error"}")
                                        ImageCaptureResult.Error(e)
                                    }
                                } else {
                                    // ByteArray mode - existing logic
                                    when (imageFormat) {
                                        ImageFormat.JPEG -> {
                                            // Use original capture data directly - already has correct EXIF
                                            image.toByteArray().let {
                                                ImageCaptureResult.Success(it)
                                            }
                                        }
                                        ImageFormat.PNG -> {
                                            // Convert to PNG - fix orientation
                                            val uiImage = image.toUIImage()
                                            val orientedImage = uiImage.fixOrientation()
                                            UIImagePNGRepresentation(orientedImage)?.toByteArray()?.let {
                                                ImageCaptureResult.Success(it)
                                            }
                                        }
                                        else -> null
                                    }
                                }

                                dispatch_async(dispatch_get_main_queue()) {
                                    isCapturing.value = false
                                    pendingCaptures.decrementAndGet()
                                    continuation.resume(result ?: ImageCaptureResult.Error(Exception("Image processing failed")))
                                }
                            }
                        } catch (e: Exception) {
                            dispatch_async(dispatch_get_main_queue()) {
                                isCapturing.value = false
                                pendingCaptures.decrementAndGet()
                                continuation.resume(ImageCaptureResult.Error(e))
                            }
                        }
                    }
                } else {
                    isCapturing.value = false
                    pendingCaptures.decrementAndGet()
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
            isCapturing.value = false
            pendingCaptures.decrementAndGet()
            captureHandler.process(null, "Capture cancelled")
        }

        // Trigger capture with constant quality (95)
        customCameraController.captureImage(0.95)
    }

    /**
     * Fast file-based capture for iOS - saves directly without ByteArray processing.
     */
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun takePictureToFile(): ImageCaptureResult = suspendCancellableCoroutine { continuation ->
        if (pendingCaptures.incrementAndGet() > maxConcurrentCaptures) {
            pendingCaptures.decrementAndGet()
            platform.Foundation.NSLog("CameraK: Burst queue full, dropping frame")
            continuation.resume(ImageCaptureResult.Error(Exception("Burst queue full, capture rejected")))
            return@suspendCancellableCoroutine
        }

        if (!isCapturing.compareAndSet(expect = false, update = true)) {
            pendingCaptures.decrementAndGet()
            continuation.resume(ImageCaptureResult.Error(Exception("Capture in progress")))
            return@suspendCancellableCoroutine
        }

        val captureHandler = object {
            var completed = false

            fun process(image: NSData?, error: String?) {
                if (completed) return
                completed = true

                if (image != null) {
                    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0u)) {
                        try {
                            autoreleasepool {
                                val result = try {
                                    val filePath = createTempFile()
                                    val writeSuccess = if (imageFormat == ImageFormat.JPEG) {
                                        // Write JPEG directly - no processing
                                        writeDataToFile(image, filePath)
                                    } else {
                                        // PNG: minimal processing
                                        val uiImage = image.toUIImage()
                                        val orientedImage = uiImage.fixOrientation()
                                        val pngData = UIImagePNGRepresentation(orientedImage)
                                        if (pngData != null) writeDataToFile(pngData, filePath) else false
                                    }
                                    
                                    if (writeSuccess) {
                                        when (directory) {
                                            Directory.PICTURES, Directory.DCIM -> {
                                                val photosPath = saveToPhotosLibrary(filePath, image)
                                                platform.Foundation.NSFileManager.defaultManager.removeItemAtPath(filePath, null)
                                                
                                                if (photosPath != null) {
                                                    ImageCaptureResult.SuccessWithFile(photosPath)
                                                } else {
                                                    ImageCaptureResult.Error(Exception("Failed to save to Photos library"))
                                                }
                                            }
                                            Directory.DOCUMENTS -> {
                                                ImageCaptureResult.SuccessWithFile(filePath)
                                            }
                                        }
                                    } else {
                                        ImageCaptureResult.Error(Exception("Failed to write image to file"))
                                    }
                                } catch (e: Exception) {
                                    platform.Foundation.NSLog("CameraK: File capture error: ${e.message ?: "Unknown"}")
                                    ImageCaptureResult.Error(e)
                                }

                                dispatch_async(dispatch_get_main_queue()) {
                                    isCapturing.value = false
                                    pendingCaptures.decrementAndGet()
                                    continuation.resume(result)
                                }
                            }
                        } catch (e: Exception) {
                            dispatch_async(dispatch_get_main_queue()) {
                                isCapturing.value = false
                                pendingCaptures.decrementAndGet()
                                continuation.resume(ImageCaptureResult.Error(e))
                            }
                        }
                    }
                } else {
                    isCapturing.value = false
                    pendingCaptures.decrementAndGet()
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
            isCapturing.value = false
            pendingCaptures.decrementAndGet()
            captureHandler.process(null, "Capture cancelled")
        }

        customCameraController.captureImage(0.95)
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
    
    actual fun getTorchMode(): TorchMode? {
        return torchMode
    }

    actual fun setZoom(zoomRatio: Float) {
        customCameraController.setZoom(zoomRatio)
    }
    
    actual fun getZoom(): Float {
        return customCameraController.getZoom()
    }
    
    actual fun getMaxZoom(): Float {
        return customCameraController.getMaxZoom()
    }

    actual fun toggleCameraLens() {

        memoryManager.updateMemoryStatus()


        if (memoryManager.isUnderMemoryPressure()) {
            memoryManager.clearBufferPools()
        }

        cameraLens = if (cameraLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        customCameraController.switchCamera()
    }
    
    actual fun getCameraLens(): CameraLens? {
        return cameraLens
    }
    
    actual fun getImageFormat(): ImageFormat {
        return imageFormat
    }
    
    actual fun getQualityPrioritization(): QualityPrioritization {
        return qualityPriority
    }
    
    actual fun getPreferredCameraDeviceType(): CameraDeviceType {
        return cameraDeviceType
    }

    actual fun startSession() {
        // Note: The actual session start happens in onSessionReady callback (setupCamera).
        // This method is called from CameraKStateHolder.initialize() which may be before
        // viewDidLoad() triggers setupCamera(). The session will start automatically
        // when ready, so we only need to handle the case where session IS ready.
        if (customCameraController.captureSession != null) {
            memoryManager.clearBufferPools()
            customCameraController.startSession()
            initializeControllerPlugins()
        }
        // If captureSession is null, onSessionReady will call startSession() when ready
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
    
    actual fun cleanup() {
        customCameraController.cleanupSession()
        memoryManager.clearBufferPools()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun createTempFile(): String {
        val timestamp = Random.nextLong()
        val fileName = "IMG_${timestamp}.${imageFormat.extension}"
        
        return when (directory) {
            Directory.PICTURES, Directory.DCIM -> {
                // For Photos library, use temp directory first
                val tempDir = platform.Foundation.NSTemporaryDirectory()
                "$tempDir$fileName"
            }
            Directory.DOCUMENTS -> {
                // For Documents, create permanent location
                val documentsDir = platform.Foundation.NSSearchPathForDirectoriesInDomains(
                    platform.Foundation.NSDocumentDirectory,
                    platform.Foundation.NSUserDomainMask,
                    true
                ).firstOrNull() as? String ?: platform.Foundation.NSTemporaryDirectory()
                
                // Create CameraK subdirectory
                val cameraKDir = "$documentsDir/CameraK"
                val fileManager = platform.Foundation.NSFileManager.defaultManager
                if (!fileManager.fileExistsAtPath(cameraKDir)) {
                    fileManager.createDirectoryAtPath(
                        cameraKDir,
                        withIntermediateDirectories = true,
                        attributes = null,
                        error = null
                    )
                }
                
                "$cameraKDir/$fileName"
            }
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    private fun saveToPhotosLibrary(filePath: String, imageData: NSData): String? {
        var savedAssetId: String? = null
        var saveError: String? = null
        
        // Save to Photos library synchronously (blocking the capture flow)
        val semaphore = platform.darwin.dispatch_semaphore_create(0)
        
        PHPhotoLibrary.sharedPhotoLibrary().performChanges(
            changeBlock = {
                // Create asset from image data
                val creationRequest = PHAssetChangeRequest.creationRequestForAssetFromImageAtFileURL(
                    platform.Foundation.NSURL.fileURLWithPath(filePath)
                )
                savedAssetId = creationRequest?.placeholderForCreatedAsset?.localIdentifier
            },
            completionHandler = { success, error ->
                if (!success || error != null) {
                    saveError = error?.localizedDescription ?: "Failed to save to Photos"
                    platform.Foundation.NSLog("CameraK: Failed to save to Photos: ${saveError ?: "Unknown error"}")
                }
                platform.darwin.dispatch_semaphore_signal(semaphore)
            }
        )
        
        platform.darwin.dispatch_semaphore_wait(semaphore, platform.darwin.DISPATCH_TIME_FOREVER)
        
        // Return the asset identifier or error
        return if (saveError == null && savedAssetId != null) {
            "ph://$savedAssetId" // Use custom scheme to indicate Photos library asset
        } else {
            null
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