package com.kashif.cameraK.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.kashif.cameraK.builder.CameraControllerBuilder
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.controller.DesktopCameraControllerBuilder
import com.kashif.cameraK.controller.ImagePanel
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
        var panel by remember { mutableStateOf<ImagePanel?>(null) }

        DisposableEffect(Unit) {
            cameraController.startSession()
            cameraController.initializeControllerPlugins()

            val frameJob = scope.launch(Dispatchers.Main) {
                frameChannel.consumeAsFlow().collect { image ->
                    panel?.updateImage(image)

                }
            }

            onDispose {
                frameJob.cancel()
                cameraController.stopSession()
            }
        }


        SwingPanel(
            modifier = modifier.fillMaxSize(),
            factory = {
                onCameraControllerReady(cameraController)
                ImagePanel().also { panel = it }
            },
            update = { }
        )
    }
}