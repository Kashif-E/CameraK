package com.kashif.cameraK

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController

@Composable
actual fun CameraKPreview(modifier: Modifier, cameraController: CameraController) {

    if (checkCameraPermission() && checkStoragePermission()) {
        UIKitViewController(factory = {
            cameraController
        }, modifier = modifier)
    }
}


