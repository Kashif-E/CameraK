package com.kashif.cameraK

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
private const val STORAGE_PERMISSION_REQUEST_CODE = 1002

@Composable
actual fun checkCameraPermission(): Boolean {
    val permission = android.Manifest.permission.CAMERA
    return ContextCompat.checkSelfPermission(LocalContext.current, permission) == PackageManager.PERMISSION_GRANTED
}

@Composable
actual fun requestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit) {
    val context = LocalContext.current
    val permission = Manifest.permission.CAMERA

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onGranted()
        } else {
            onDenied()
        }
    }

    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
        onGranted()
    } else {
        launcher.launch(permission)
    }
}


private fun Context.findAndroidActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
actual fun checkStoragePermission(): Boolean {
    val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    return ContextCompat.checkSelfPermission(LocalContext.current, permission) == PackageManager.PERMISSION_GRANTED
}

@Composable
actual fun requestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
    val context = LocalContext.current
    val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onGranted()
        } else {
            onDenied()
        }
    }

    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
        onGranted()
    } else {
        launcher.launch(permission)
    }
}