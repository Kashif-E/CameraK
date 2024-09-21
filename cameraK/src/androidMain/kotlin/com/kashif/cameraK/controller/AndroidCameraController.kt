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
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.utils.InvalidConfigurationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

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
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private var flashMode: FlashMode,
    private var cameraLens: CameraLens,
    private var rotation: Rotation,
    private var imageFormat: ImageFormat,
    private var directory: Directory
) : CameraController {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var camera: Camera? = null

    private val imageCaptureListeners = mutableListOf<(ByteArray) -> Unit>()

    override fun bindCamera(previewView: Any) {
        if (previewView !is PreviewView) {
            throw IllegalArgumentException("Invalid preview view type. Expected PreviewView.")
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Unbind all use cases before rebinding
            cameraProvider?.unbindAll()

            // Setup Preview Use Case
            preview = Preview.Builder()
                .setTargetRotation(rotation.toSurfaceRotation())
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Setup ImageCapture Use Case
            imageCapture = ImageCapture.Builder()
                .setFlashMode(flashMode.toCameraXFlashMode())
                .setTargetRotation(rotation.toSurfaceRotation())
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Select Camera Lens
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(cameraLens.toCameraXLensFacing())
                .build()

            try {
                // Bind Use Cases to Lifecycle
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                throw InvalidConfigurationException("Failed to bind camera use cases: ${exc.message}")
            }

        }, ContextCompat.getMainExecutor(context))
    }

    override suspend fun takePicture(): ImageCaptureResult = suspendCancellableCoroutine { cont ->
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
        ) ?: cont.resume(ImageCaptureResult.Error(Exception("ImageCapture use case is not initialized.")))
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

    // Helper Methods
    private fun createTempFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.externalCacheDir?.absolutePath ?: context.cacheDir.absolutePath
        return File.createTempFile("IMG_$timeStamp", ".${imageFormat.extension}", File(storageDir))
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

    private fun Rotation.toSurfaceRotation(): Int = when (this) {
        Rotation.ROTATION_0 -> android.view.Surface.ROTATION_0
        Rotation.ROTATION_90 -> android.view.Surface.ROTATION_90
        Rotation.ROTATION_180 -> android.view.Surface.ROTATION_180
        Rotation.ROTATION_270 -> android.view.Surface.ROTATION_270
    }
}