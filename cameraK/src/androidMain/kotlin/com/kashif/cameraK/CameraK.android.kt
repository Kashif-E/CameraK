package com.kashif.cameraK

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner


val REQUIRED_PERMISSIONS = mutableListOf(
    Manifest.permission.CAMERA,
).apply {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}.toTypedArray()

fun allPermissionsGranted(context: Context) = REQUIRED_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun CameraKPreview(modifier: Modifier, cameraController: CameraController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    if (allPermissionsGranted(context)) {
        LaunchedEffect(context) {
            cameraController.context = context
            cameraController.owner = lifecycleOwner
            cameraController.cameraProvider = ProcessCameraProvider.getInstance(context).get()
        }

        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { ctx ->
                cameraController.startCameraPreviewView(ctx)
            }
        )
    } else {
        Log.e("CameraKPreview", "Permissions not granted")
    }
}
