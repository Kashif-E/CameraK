package com.kashif.cameraK.compose

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kashif.cameraK.controller.CameraController

/**
 * Android implementation of stateless camera preview.
 * Displays the camera feed using CameraX's PreviewView.
 */
@Composable
actual fun CameraPreviewView(controller: CameraController, modifier: Modifier) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(controller, previewView) {
        controller.bindCamera(previewView) {
            // Camera is already bound and started by the state holder
        }
        onDispose {
            // Cleanup is handled by the state holder
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}
