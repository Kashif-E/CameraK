package com.kashif.cameraK.ui

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kashif.cameraK.builder.CameraControllerBuilder
import com.kashif.cameraK.builder.createAndroidCameraControllerBuilder
import com.kashif.cameraK.controller.CameraController

/**
 * Android-specific implementation of [CameraPreview].
 *
 * @param modifier Modifier to be applied to the camera preview.
 * @param cameraConfiguration Lambda to configure the [CameraControllerBuilder].
 * @param onCameraControllerReady Callback invoked with the initialized [CameraController].
 */
@Composable
actual fun expectCameraPreview(
    modifier: Modifier,
    cameraConfiguration: CameraControllerBuilder.() -> Unit,
    onCameraControllerReady: (CameraController) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val isCameraReady = remember { mutableStateOf(false) }
    val cameraController = remember {
        createAndroidCameraControllerBuilder(context, lifecycleOwner)
            .apply(cameraConfiguration)
            .build()
    }


    val previewView = remember { PreviewView(context) }

    DisposableEffect(previewView) {
        cameraController.bindCamera(previewView) {
            onCameraControllerReady(cameraController)
        }
        onDispose {
            cameraController.stopSession()
        }
    }



    AndroidView(
        factory = { previewView },
        modifier = modifier,

        )

}