package com.kashif.cameraK.ui

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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


    val cameraController = remember {
        createAndroidCameraControllerBuilder(context, lifecycleOwner)
            .apply(cameraConfiguration)
            .build()
    }

    // Invoke the callback to provide the CameraController to the parent composable
    LaunchedEffect(cameraController) {
        onCameraControllerReady(cameraController)
    }

    val previewView = remember { PreviewView(context) }

    DisposableEffect(previewView) {
        cameraController.bindCamera(previewView)
        onDispose {
            cameraController.stopSession()
        }
    }


    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}