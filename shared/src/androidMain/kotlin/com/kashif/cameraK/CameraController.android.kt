package com.kashif.cameraK

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

actual class CameraController  {
    actual var cameraState: MutableStateFlow<CameraState> = MutableStateFlow(CameraState())
    private var cameraK: CameraK? = null

    actual fun takePicture(imageFormat: ImageFormat): ImageCaptureResult {
        return cameraK?.takePicture(imageFormat) ?: run {
            Log.e("CameraController", "CameraK is null. Cannot take picture.")
            ImageCaptureResult.Error(NullPointerException("CameraK is null"))
        }
    }

    actual fun savePicture(name: String, file: ByteArray, directory: Directory) {
        cameraK?.savePicture(name, file, directory) ?: Log.e("CameraController", "CameraK is null. Cannot save picture.")
    }

    actual fun setFlashMode(flashMode: FlashMode) {
        cameraState.update {
            it.copy(flashMode = flashMode)
        }
        cameraK?.changeFlashMode(value = flashMode.value, lens = getCameraLens(), cameraRotation = getCameraRotation())
            ?: Log.e("CameraController", "CameraK is null. Cannot change flash mode.")
    }

    actual fun setCameraLens(lens: CameraLens) {
        cameraState.update {
            it.copy(cameraLens = lens)
        }
        cameraK?.changeLens(lens.value) ?: Log.e("CameraController", "CameraK is null. Cannot change lens.")
    }

    actual fun getFlashMode(): Int {
        return cameraState.value.flashMode.value
    }

    actual fun getCameraLens(): Int {
        return cameraState.value.cameraLens.value
    }


    @Composable
    fun CameraCompose(modifier: Modifier) {
        val context = LocalContext.current
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        cameraK = CameraK(context = context, owner = lifecycleOwner)
        AndroidView(modifier = modifier, factory = {
            cameraK!!.startCameraPreviewView(
                rotation = getCameraRotation(),
                lens = getCameraLens()
            )
        })
    }

    actual fun getCameraRotation(): Int {
        return cameraState.value.rotation.value
    }

    actual fun setCameraRotation(rotation: Rotation) {
        cameraState.update {
            it.copy(rotation = rotation)
        }
        cameraK?.setRotation(rotation.value, getCameraLens())
    }
}