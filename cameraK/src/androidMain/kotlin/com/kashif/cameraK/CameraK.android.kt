package com.kashif.cameraK

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat


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
actual fun CameraKPreview(modifier: Modifier, cameraController: CameraController) {
    if (allPermissionsGranted(context = LocalContext.current)) {
       cameraController.CameraCompose(modifier = modifier)
    } else {
        Log.e("CameraKPreview", "Permissions not granted")
    }
}

