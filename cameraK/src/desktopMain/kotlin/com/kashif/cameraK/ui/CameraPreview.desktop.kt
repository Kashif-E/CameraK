package com.kashif.cameraK.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.kashif.cameraK.builder.CameraControllerBuilder
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.controller.DesktopCameraControllerBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@Composable
actual fun expectCameraPreview(
    modifier: Modifier,
    cameraConfiguration: CameraControllerBuilder.() -> Unit,
    onCameraControllerReady: (CameraController) -> Unit
) {
    val cameraController = remember {
        DesktopCameraControllerBuilder()
            .apply(cameraConfiguration).build()
    }

    BoxWithConstraints(modifier = modifier) {
        val scope = rememberCoroutineScope()

        val frameChannel = cameraController.getFrameChannel()
        var img by remember { mutableStateOf<ImageBitmap?>(null) }

        DisposableEffect(Unit) {
            cameraController.startSession()
            cameraController.initializeControllerPlugins()
            onCameraControllerReady(cameraController)

            val frameJob = scope.launch(Dispatchers.Main) {
                frameChannel.consumeAsFlow().collect { image ->
                    img = image.toComposeImageBitmap()
                }
            }

            onDispose {
                frameJob.cancel()
                cameraController.stopSession()
            }
        }

        if (img != null) {
            Image(img!!, contentDescription = null, modifier.fillMaxSize())
        }
    }
}
