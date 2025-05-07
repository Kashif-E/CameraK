package com.kashif.cameraK.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.kashif.cameraK.capture.BurstCaptureManager
import com.kashif.cameraK.enums.*
import com.kashif.cameraK.plugins.CameraPlugin
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.utils.InvalidConfigurationException
import com.kashif.cameraK.utils.MemoryManager
import com.kashif.cameraK.utils.compressToByteArray
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Android-specific implementation of [CameraController] using CameraX.
 */
actual class CameraController(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    internal var flashMode: FlashMode,
    internal var torchMode: TorchMode,
    internal var cameraLens: CameraLens,
    internal var imageFormat: ImageFormat,
    internal var qualityPriority: QualityPrioritization,
    internal var directory: Directory,
    internal var plugins: MutableList<CameraPlugin>
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    var imageAnalyzer: ImageAnalysis? = null
    private var previewView: PreviewView? = null

    private val imageCaptureListeners = mutableListOf<(ByteArray) -> Unit>()


    private val memoryManager = MemoryManager
    private val burstCaptureManager = BurstCaptureManager()


    private val imageProcessingExecutor = Executors.newFixedThreadPool(2)


    private var isCapturing = false

    fun bindCamera(previewView: PreviewView, onCameraReady: () -> Unit = {}) {
        this.previewView = previewView


        memoryManager.initialize(context)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                cameraProvider?.unbindAll()


                preview = Preview.Builder()
                    .setResolutionSelector(createResolutionSelector())
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraLens.toCameraXLensFacing())
                    .build()


                configureCaptureUseCase()


                val useCases = mutableListOf(preview!!, imageCapture!!)
                imageAnalyzer?.let { useCases.add(it) }


                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    *useCases.toTypedArray()
                )

                onCameraReady()

            } catch (exc: Exception) {
                println("Use case binding failed: ${exc.message}")
                exc.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Create a resolution selector based on memory conditions
     */
    private fun createResolutionSelector(): ResolutionSelector {

        memoryManager.updateMemoryStatus()

        return ResolutionSelector.Builder()
            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()
    }

    /**
     * Configure the image capture use case with settings adapted to current memory conditions
     */
    @OptIn(ExperimentalZeroShutterLag::class)
    private fun configureCaptureUseCase() {

        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode.toCameraXFlashMode())
            .setCaptureMode(
                when (qualityPriority) {
                    QualityPrioritization.QUALITY -> ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                    QualityPrioritization.SPEED -> ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
                    else -> {
                        if (memoryManager.isUnderMemoryPressure()) {
                            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                        } else {
                            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                        }
                    }
                }
            )
            .setResolutionSelector(createResolutionSelector())
            .build()
    }

    fun updateImageAnalyzer() {
        camera?.let {
            cameraProvider?.unbind(imageAnalyzer)
            imageAnalyzer?.let { analyzer ->
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(cameraLens.toCameraXLensFacing())
                        .build(),
                    analyzer
                )
            }
        } ?: throw InvalidConfigurationException("Camera not initialized.")
    }

    actual suspend fun takePicture(): ImageCaptureResult =
        suspendCancellableCoroutine { cont ->

            if (isCapturing) {
                cont.resume(ImageCaptureResult.Error(Exception("Capture already in progress")))
                return@suspendCancellableCoroutine
            }

            isCapturing = true


            memoryManager.updateMemoryStatus()


            val captureRequested = burstCaptureManager.requestCapture(
                captureFunction = {

                    val quality = burstCaptureManager.getOptimalQuality()
                    performCapture(cont, quality)
                },
                onComplete = {

                    isCapturing = false
                }
            )

            if (!captureRequested) {
                isCapturing = false
                cont.resume(ImageCaptureResult.Error(Exception("Too many captures in progress")))
            }
        }

    /**
     * Perform the actual image capture with the specified quality
     */
    private fun performCapture(
        continuation: CancellableContinuation<ImageCaptureResult>,
        quality: Int
    ) {
        val outputOptions = ImageCapture.OutputFileOptions.Builder(createTempFile()).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    imageProcessingExecutor.execute {
                        val byteArray = processImageOutput(output, quality)

                        if (byteArray != null) {
                            imageCaptureListeners.forEach { it(byteArray) }
                            continuation.resume(ImageCaptureResult.Success(byteArray))
                        } else {
                            continuation.resume(ImageCaptureResult.Error(Exception("Failed to convert image to ByteArray.")))
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    continuation.resume(ImageCaptureResult.Error(exc))
                }
            }
        ) ?: continuation.resume(ImageCaptureResult.Error(Exception("ImageCapture use case is not initialized.")))
    }

    /**
     * Process the saved image output with memory-efficient approach
     */
    private fun processImageOutput(
        output: ImageCapture.OutputFileResults,
        quality: Int
    ): ByteArray? {
        return try {
            output.savedUri?.let { uri ->

                val tempFile = createTempFile("temp_image", ".jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }


                val exif = ExifInterface(tempFile.absolutePath)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )


                val options = BitmapFactory.Options().apply {

                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(tempFile.absolutePath, options)


                options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight)
                options.inJustDecodeBounds = false


                val originalBitmap = BitmapFactory.decodeFile(tempFile.absolutePath, options)


                tempFile.delete()


                val rotationAngle = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                } + (imageCapture?.targetRotation?.toFloat() ?: 0f)


                val rotatedBitmap = if (originalBitmap != null && rotationAngle != 0f) {
                    Matrix().apply {
                        postRotate(rotationAngle)
                    }.let { matrix ->
                        Bitmap.createBitmap(
                            originalBitmap,
                            0,
                            0,
                            originalBitmap.width,
                            originalBitmap.height,
                            matrix,
                            true
                        )
                    }
                } else {
                    originalBitmap
                }


                rotatedBitmap?.compressToByteArray(
                    format = when (imageFormat) {
                        ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                        ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                    },
                    quality = quality,
                    recycleInput = true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Calculate appropriate sample size for bitmap decoding based on memory conditions
     */
    private fun calculateSampleSize(width: Int, height: Int): Int {
        return when {
            memoryManager.isUnderMemoryPressure() -> {

                var sampleSize = 1
                val totalPixels = width * height
                val targetPixels = 2_000_000

                while ((totalPixels / (sampleSize * sampleSize)) > targetPixels) {
                    sampleSize *= 2
                }
                sampleSize
            }

            burstCaptureManager.isBurstModeActive() -> {

                2
            }

            else -> {

                1
            }
        }
    }

    actual fun toggleFlashMode() {
        flashMode = when (flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
        imageCapture?.flashMode = flashMode.toCameraXFlashMode()
    }

    actual fun setFlashMode(mode: FlashMode) {
        flashMode = mode
        imageCapture?.flashMode = mode.toCameraXFlashMode()
    }

    actual fun getFlashMode(): FlashMode? {
        fun Int.toCameraKFlashMode(): FlashMode? {
            return when (this) {
                ImageCapture.FLASH_MODE_ON -> FlashMode.ON
                ImageCapture.FLASH_MODE_OFF -> FlashMode.OFF
                ImageCapture.FLASH_MODE_AUTO -> FlashMode.AUTO
                else -> null
            }
        }

        return imageCapture?.flashMode?.toCameraKFlashMode()
    }

    actual fun toggleTorchMode() {
        torchMode = when (torchMode) {
            TorchMode.OFF -> TorchMode.ON
            TorchMode.ON -> TorchMode.OFF
            else -> TorchMode.OFF
        }
        camera?.cameraControl?.enableTorch(torchMode == TorchMode.ON)
    }

    actual fun setTorchMode(mode: TorchMode) {
        torchMode = mode
        camera?.cameraControl?.enableTorch(mode == TorchMode.ON)
    }

    actual fun toggleCameraLens() {

        memoryManager.updateMemoryStatus()


        if (memoryManager.isUnderMemoryPressure()) {
            memoryManager.clearBufferPools()
            System.gc()
        }

        cameraLens = if (cameraLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        previewView?.let { bindCamera(it) }
    }

    actual fun startSession() {


        memoryManager.updateMemoryStatus()
        memoryManager.clearBufferPools()
        initializeControllerPlugins()
    }

    actual fun stopSession() {
        cameraProvider?.unbindAll()


        burstCaptureManager.reset()
        memoryManager.clearBufferPools()
    }

    actual fun addImageCaptureListener(listener: (ByteArray) -> Unit) {
        imageCaptureListeners.add(listener)
    }

    actual fun initializeControllerPlugins() {
        plugins.forEach { it.initialize(this) }
    }


    private fun createTempFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.externalCacheDir?.absolutePath ?: context.cacheDir.absolutePath
        return File.createTempFile(
            "IMG_$timeStamp",
            ".${imageFormat.extension}",
            File(storageDir)
        )
    }

    private fun FlashMode.toCameraXFlashMode(): Int = when (this) {
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }

    private fun CameraLens.toCameraXLensFacing(): Int = when (this) {
        CameraLens.FRONT -> CameraSelector.LENS_FACING_FRONT
        CameraLens.BACK -> CameraSelector.LENS_FACING_BACK
    }

    /**
     * Clean up resources when no longer needed
     * Should be called when the controller is being destroyed
     */
    fun cleanup() {
        imageProcessingExecutor.shutdown()
        burstCaptureManager.shutdown()
        memoryManager.clearBufferPools()
    }
}