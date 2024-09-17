package com.kashif.cameraK

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream

class CameraK(
    private val context: Context,
    private val owner: LifecycleOwner
) {
    private val cameraProviderFuture by lazy { ProcessCameraProvider.getInstance(context) }

    fun startCameraPreviewView(rotation: Int, lens: Int): PreviewView {
        val previewView = PreviewView(context)
        val preview = createPreview(rotation, previewView)
        val camSelector = createCameraSelector(lens)

        bindToLifecycle(camSelector, preview)
        return previewView
    }

    fun unbind() {
        try {
            cameraProviderFuture.get().unbindAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun changeLens(value: Int) {
        val camSelector = createCameraSelector(value)
        bindToLifecycle(camSelector)
    }

    fun changeFlashMode(value: Int, lens: Int, cameraRotation: Int) {
        val camSelector = createCameraSelector(lens)
        val imageCapture = createImageCapture(cameraRotation, value)
        val previewView = PreviewView(context)
        val preview = createPreview(cameraRotation, previewView)

        bindToLifecycle(camSelector, imageCapture, preview)
    }

    fun takePicture(imageFormat: ImageFormat): ImageCaptureResult {
        val imageCapture = createImageCapture(context.resources.configuration.orientation)
        val outputFile = File(context.cacheDir, "temp_image.${imageFormat.extension}")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        return captureImage(imageCapture, outputOptions,outputFile.absolutePath )
    }

    fun savePicture(name: String, file: ByteArray, directory: Directory) {
        val dir = File(context.getExternalFilesDir(null), directory.value)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val pictureFile = File(dir, name)
        try {
            FileOutputStream(pictureFile).use { fos ->
                fos.write(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createPreview(rotation: Int, previewView: PreviewView): Preview {
        return Preview.Builder()
            .setTargetRotation(rotation)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
    }

    private fun createCameraSelector(lens: Int): CameraSelector {
        return CameraSelector.Builder()
            .requireLensFacing(lens)
            .build()
    }

    private fun createImageCapture(rotation: Int, flashMode: Int = ImageCapture.FLASH_MODE_OFF): ImageCapture {
        return ImageCapture.Builder()
            .setTargetRotation(rotation)
            .setFlashMode(flashMode)
            .build()
    }

    private fun bindToLifecycle(camSelector: CameraSelector, vararg useCases: androidx.camera.core.UseCase) {
        try {
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(owner, camSelector, *useCases)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun captureImage(
        imageCapture: ImageCapture,
        outputOptions: ImageCapture.OutputFileOptions,
        absolutePath: String
    ): ImageCaptureResult {
        return try {

            val result = CompletableDeferred<ImageCaptureResult>()
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val byteArray = outputFileResults.savedUri?.let { uri ->
                            context.contentResolver.openInputStream(uri)?.readBytes()
                        } ?: outputOptions.file?.readBytes() ?: byteArrayOf()
                        result.complete(ImageCaptureResult.Success(byteArray, absolutePath))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        result.complete(ImageCaptureResult.Error(exception))
                    }
                }
            )
            runBlocking { result.await() }
        } catch (e: Exception) {
            ImageCaptureResult.Error(e)
        }
    }

    fun setRotation(rotation: Int, lens: Int) {
        unbind()
        val camSelector = createCameraSelector(lens)
        val previewView = PreviewView(context)
        val preview = createPreview(rotation, previewView)
        bindToLifecycle(camSelector, preview)

    }
}

