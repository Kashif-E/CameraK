package com.kashif.cameraK.ui


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import com.kashif.cameraK.builder.CameraControllerBuilder
import com.kashif.cameraK.builder.createIOSCameraControllerBuilder
import com.kashif.cameraK.controller.CameraController
import platform.UIKit.UIViewController

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

    // Invoke the callback to provide the CameraController to the parent composable
    LaunchedEffect(cameraController) {
        onCameraControllerReady(cameraController)
        cameraController.initializePlugins()
    }


    UIKitViewController(
        factory = { cameraController as UIViewController },
        modifier = modifier
    )
}
