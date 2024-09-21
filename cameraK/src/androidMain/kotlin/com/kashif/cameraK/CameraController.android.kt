package com.kashif.cameraK


import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Surface
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

actual class CameraController {

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewView: PreviewView? = null

    private val context: Context = AppContext.get()
    private var lifecycleOwner: LifecycleOwner? = null

    // Variables to store the current camera settings
    private var currentFlashMode: FlashMode by mutableStateOf(FlashMode.OFF)
    private var currentCameraLens: CameraLens by mutableStateOf(CameraLens.BACK)
    private var currentRotation: Rotation by mutableStateOf(Rotation.ROTATION_0)

    // Store the Preview use case to update rotation
    private var previewUseCase: Preview? = null

    fun init(lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
    }

    fun startCamera(previewView: PreviewView) {
        this.previewView = previewView
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            cameraProvider = cameraProviderFuture.get()


            previewUseCase = Preview.Builder()
                .setTargetRotation(currentRotation.toSurfaceRotation())
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }


            imageCapture = ImageCapture.Builder()
                .setFlashMode(currentFlashMode.toCameraXFlashMode())
                .setTargetRotation(currentRotation.toSurfaceRotation())
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(currentCameraLens.toCameraXLensFacing())
                .build()

            cameraProvider?.unbindAll()

            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner!!,
                cameraSelector,
                previewUseCase,
                imageCapture
            )

        }, ContextCompat.getMainExecutor(context))
    }

    actual suspend fun takePicture(imageFormat: ImageFormat): ImageCaptureResult = withContext(Dispatchers.IO) {
        val imageCapture =
            imageCapture ?: return@withContext ImageCaptureResult.Error(Exception("ImageCapture not initialized"))

        val outputOptions = ImageCapture.OutputFileOptions.Builder(createFile(imageFormat)).build()

        suspendCancellableCoroutine<ImageCaptureResult> { continuation ->
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        continuation.resume(ImageCaptureResult.Error(exception))
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = outputFileResults.savedUri ?: Uri.fromFile(createFile(imageFormat))
                        val byteArray = readBytesFromUri(savedUri)
                        continuation.resume(
                            ImageCaptureResult.Success(
                                byteArray,
                                savedUri.lastPathSegment
                                    ?: "captured_image${System.currentTimeMillis()}${imageFormat.extension}"
                            )
                        )
                    }
                }
            )
        }
    }

    actual fun savePicture(fileName: String, fileData: ByteArray, directory: Directory) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                when (directory) {
                    Directory.PICTURES -> Environment.DIRECTORY_PICTURES
                    Directory.DCIM -> Environment.DIRECTORY_DCIM
                }
            )
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(
                when (directory) {
                    Directory.PICTURES -> Environment.DIRECTORY_PICTURES
                    Directory.DCIM -> Environment.DIRECTORY_DCIM
                }
            ).absolutePath
            val file = File(picturesDir, fileName)
            contentValues.put(MediaStore.Images.Media.DATA, file.absolutePath)
        }

        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(fileData)
                outputStream.flush()
            }
        } ?: run {
            Toast.makeText(context, "Failed to create new MediaStore record.", Toast.LENGTH_SHORT).show()
        }
    }

    actual fun toggleFlashMode() {
        // Toggle between ON and OFF
        currentFlashMode = if (currentFlashMode == FlashMode.OFF) FlashMode.ON else FlashMode.OFF
        imageCapture?.flashMode = currentFlashMode.toCameraXFlashMode()
    }

    actual fun toggleCameraLens() {
        currentCameraLens = if (currentCameraLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        previewView?.let { startCamera(it) }
    }

    actual fun getFlashMode(): FlashMode = currentFlashMode

    actual fun getCameraLens(): CameraLens = currentCameraLens

    actual fun getCameraRotation(): Int = currentRotation.value

    actual fun setCameraRotation(rotation: Rotation) {
        currentRotation = rotation

        val surfaceRotation = rotation.toSurfaceRotation()

        imageCapture?.targetRotation = surfaceRotation
        previewUseCase?.targetRotation = surfaceRotation

        // Re-bind the camera to apply the changes immediately
        cameraProvider?.let { provider ->
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(currentCameraLens.toCameraXLensFacing())
                .build()

            provider.unbindAll()

            camera = provider.bindToLifecycle(
                lifecycleOwner!!,
                cameraSelector,
                previewUseCase,
                imageCapture
            )
        }
    }

    actual fun allPermissionsGranted(): Boolean {
        return checkStoragePermission() && checkCameraPermission()
    }

    actual fun bindCamera() {
        previewView?.let { startCamera(it) }
    }

    private fun createFile(imageFormat: ImageFormat): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, "CameraX-Images").apply { mkdirs() }
        } ?: context.filesDir

        return File(
            mediaDir,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + imageFormat.extension
        )
    }

    private fun readBytesFromUri(uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
        return inputStream?.readBytes() ?: ByteArray(0)
    }

    private fun FlashMode.toCameraXFlashMode(): Int {
        return when (this) {
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }

    private fun CameraLens.toCameraXLensFacing(): Int {
        return when (this) {
            CameraLens.FRONT -> CameraSelector.LENS_FACING_FRONT
            CameraLens.BACK -> CameraSelector.LENS_FACING_BACK
            else -> CameraSelector.LENS_FACING_BACK // Default to back camera
        }
    }

    private fun Rotation.toSurfaceRotation(): Int {
        return when (this) {
            Rotation.ROTATION_0 -> Surface.ROTATION_0
            Rotation.ROTATION_90 -> Surface.ROTATION_90
            Rotation.ROTATION_180 -> Surface.ROTATION_180
            Rotation.ROTATION_270 -> Surface.ROTATION_270
        }
    }

}