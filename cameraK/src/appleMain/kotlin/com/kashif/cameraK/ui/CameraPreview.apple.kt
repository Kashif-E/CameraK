package com.kashif.cameraK.ui


import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import com.kashif.cameraK.builder.CameraControllerBuilder
import com.kashif.cameraK.builder.createIOSCameraControllerBuilder
import com.kashif.cameraK.controller.CameraController
import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.Foundation.NSNotificationCenter
import platform.UIKit.*
import platform.darwin.*

/**
 * iOS-specific implementation of [CameraPreview].
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

    val cameraController = remember {
        createIOSCameraControllerBuilder()
            .apply(cameraConfiguration)
            .build()
    }

    LaunchedEffect(cameraController) {
        onCameraControllerReady(cameraController)
    }

    DisposableEffect(Unit) {
        val notificationCenter = NSNotificationCenter.defaultCenter
        val observer = notificationCenter.addObserverForName(
            UIDeviceOrientationDidChangeNotification,
            null,
            null
        ) { _ ->
            cameraController.getCameraPreviewLayer()?.connection?.videoOrientation =
                cameraController.currentVideoOrientation()
        }

        onDispose {
            notificationCenter.removeObserver(observer)
        }
    }

    UIKitViewController(
        factory = { cameraController },
        modifier = modifier,
    )
}
