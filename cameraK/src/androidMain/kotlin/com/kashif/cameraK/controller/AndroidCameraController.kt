package com.kashif.cameraK.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.Rotation
import com.kashif.cameraK.plugins.CameraPlugin
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.utils.InvalidConfigurationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume


interface CameraSetupListener {
    fun onCameraSetupComplete()
    fun onCameraSetupError(exception: Exception)
}
/**
 * Android-specific implementation of [CameraController] using CameraX.
 *
 * @param context The Android [Context].
 * @param lifecycleOwner The [LifecycleOwner] to bind the camera lifecycle.
 * @param flashMode The desired [FlashMode].
 * @param cameraLens The desired [CameraLens].
 * @param rotation The desired [Rotation].
 * @param imageFormat The desired [ImageFormat].
 * @param directory The desired [Directory] to save images.
 */
class AndroidCameraController(
    internal val context: Context,
    val lifecycleOwner: LifecycleOwner,
    internal var flashMode: FlashMode,
    internal var cameraLens: CameraLens,
    internal var rotation: Rotation,
    internal var imageFormat: ImageFormat,
    internal var directory: Directory,
    internal var plugins: MutableList<CameraPlugin>
) : CameraController {

    var cameraProvider: ProcessCameraProvider? = null
    var imageCapture: ImageCapture? = null
    var preview: Preview? = null
    var camera: Camera? = null
    var imageAnalyzer: ImageAnalysis? = null


    private val imageCaptureListeners = mutableListOf<(ByteArray) -> Unit>()

    fun bindCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                cameraProvider?.unbindAll() // Unbind previous use cases

                preview = Preview.Builder()
                    .setTargetRotation(rotation.toSurfaceRotation())
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraLens.toCameraXLensFacing())
                    .build()

                // Setup ImageCapture Use Case
                imageCapture = ImageCapture.Builder()
                    .setFlashMode(flashMode.toCameraXFlashMode())
                    .setTargetRotation(rotation.toSurfaceRotation())
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Setup ImageAnalysis Use Case only if needed
                val useCases = mutableListOf<UseCase>(preview!!, imageCapture!!)
                imageAnalyzer?.let { useCases.add(it) }

                // Bind the selected use cases to lifecycle
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    *useCases.toTypedArray()
                )

                // Notify the listener that the camera is set up
                initializePlugins()

            } catch (exc: Exception) {
                println("Use case binding failed: ${exc.message}")
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun updateImageAnalyzer() {
        camera?.let {
            cameraProvider?.unbind(imageAnalyzer)
            imageAnalyzer?.let { analyzer ->
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(cameraLens.toCameraXLensFacing()).build(),
                    analyzer // Only bind the analyzer without touching the rest of the setup
                )
            }
        } ?: throw InvalidConfigurationException("Camera not initialized.")
    }

    /**
     * Set or update the ImageAnalyzer. This will automatically restart the camera
     * to apply the new configuration.
     */
    fun setImageAnalyzer() {
        // Restart the camera to bind the new analyzer
        bindCamera(preview?.let { PreviewView(context) }
            ?: throw InvalidConfigurationException("PreviewView not initialized."))
    }


    override suspend fun takePicture(): ImageCaptureResult =
        suspendCancellableCoroutine { cont ->
            val outputOptions = ImageCapture.OutputFileOptions.Builder(createTempFile()).build()

            imageCapture?.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        // Read the image data into a ByteArray
                        val byteArray = try {
                            BitmapFactory.decodeFile(output.savedUri?.path)
                                ?.let { bitmap ->
                                    val stream = ByteArrayOutputStream()
                                    bitmap.compress(
                                        when (imageFormat) {
                                            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                                            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                                        },
                                        100,
                                        stream
                                    )
                                    stream.toByteArray()
                                }
                        } catch (e: Exception) {
                            null
                        }

                        if (byteArray != null) {
                            imageCaptureListeners.forEach { it(byteArray) }
                            cont.resume(ImageCaptureResult.Success(byteArray))
                        } else {
                            cont.resume(ImageCaptureResult.Error(Exception("Failed to convert image to ByteArray.")))
                        }
                    }

                    override fun onError(exc: ImageCaptureException) {
                        cont.resume(ImageCaptureResult.Error(exc))
                    }
                }
            )
                ?: cont.resume(ImageCaptureResult.Error(Exception("ImageCapture use case is not initialized.")))
        }

    override fun toggleFlashMode() {
        flashMode = when (flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
        imageCapture?.flashMode = flashMode.toCameraXFlashMode()
    }

    override fun toggleCameraLens() {
        cameraLens = if (cameraLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        bindCamera(preview?.let { PreviewView(context) }
            ?: throw InvalidConfigurationException("PreviewView not initialized."))
    }

    override fun setCameraRotation(rotation: Rotation) {
        this.rotation = rotation
        imageCapture?.targetRotation = rotation.toSurfaceRotation()
        preview?.targetRotation = rotation.toSurfaceRotation()
    }

    override fun startSession() {
        // CameraX handles session start based on lifecycle
    }

    override fun stopSession() {
        cameraProvider?.unbindAll()
    }

    override fun addImageCaptureListener(listener: (ByteArray) -> Unit) {
        imageCaptureListeners.add(listener)
    }

    override fun initializePlugins() {
        plugins.forEach { it.initialize(this) }
    }

    // Helper Methods
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

    fun Rotation.toSurfaceRotation(): Int = when (this) {
        Rotation.ROTATION_0 -> android.view.Surface.ROTATION_0
        Rotation.ROTATION_90 -> android.view.Surface.ROTATION_90
        Rotation.ROTATION_180 -> android.view.Surface.ROTATION_180
        Rotation.ROTATION_270 -> android.view.Surface.ROTATION_270
    }
}