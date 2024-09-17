package com.kashif.cameraK

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileOutputStream

actual class CameraController {
    actual var cameraState: MutableStateFlow<CameraState> = MutableStateFlow(CameraState())
    internal var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    internal var context: Context? = null
    internal var owner: LifecycleOwner? = null


    fun startCameraPreviewView(context: Context): PreviewView {

        val previewView = PreviewView(context)
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        bindToLifecycle(createCameraSelector(cameraState.value.cameraLens.value), preview!!)
        return previewView
    }

    actual suspend fun takePicture(imageFormat: ImageFormat): ImageCaptureResult {
        return if (imageCapture == null) {
            ImageCaptureResult.Error(NullPointerException("ImageCapture is null"))
        } else {
            val outputFile = File(context?.cacheDir, "temp_image.${imageFormat.extension}")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
            captureImage(imageCapture!!, outputOptions, outputFile.absolutePath)
        }
    }

    actual fun savePicture(name: String, file: ByteArray, directory: Directory) {
        val dir = File(context?.getExternalFilesDir(null), directory.value)
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

    actual fun changeFlashMode(flashMode: FlashMode) {
        val mode = when (flashMode) {
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = mode
        cameraState.update { it.copy(flashMode = flashMode) }
    }

    actual fun changeCameraLens(lens: CameraLens) {
        cameraState.update { it.copy(cameraLens = lens) }
        bindToLifecycle(createCameraSelector(lens.value), preview!!, imageCapture!!)
    }

    actual fun getFlashMode(): Int {
        return cameraState.value.flashMode.value
    }

    actual fun getCameraLens(): Int {
        return cameraState.value.cameraLens.value
    }

    actual fun getCameraRotation(): Int {
        return cameraState.value.rotation.value
    }

    actual fun setCameraRotation(rotation: Rotation) {
        cameraState.update { it.copy(rotation = rotation) }
        preview?.targetRotation = rotation.value
        imageCapture?.targetRotation = rotation.value
    }

    private fun createCameraSelector(lens: Int): CameraSelector {
        return CameraSelector.Builder()
            .requireLensFacing(lens)
            .build()
    }

    private fun bindToLifecycle(camSelector: CameraSelector, vararg useCases: UseCase) {
        try {
            owner?.let { nonNullOwner ->
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(nonNullOwner, camSelector, *useCases)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun captureImage(
        imageCapture: ImageCapture,
        outputOptions: ImageCapture.OutputFileOptions,
        absolutePath: String
    ): ImageCaptureResult {
        return try {
            context?.let {
                val result = CompletableDeferred<ImageCaptureResult>()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(it),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val byteArray = File(absolutePath).readBytes()
                            result.complete(ImageCaptureResult.Success(byteArray, absolutePath))
                        }

                        override fun onError(exception: ImageCaptureException) {
                            result.complete(ImageCaptureResult.Error(exception))
                        }
                    }
                )
                result.await()
            } ?: ImageCaptureResult.Error(NullPointerException("Context is null"))

        } catch (e: Exception) {
            ImageCaptureResult.Error(e)
        }
    }


}