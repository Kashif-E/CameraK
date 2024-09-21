package com.kashif.cameraK

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
private const val STORAGE_PERMISSION_REQUEST_CODE = 1002


actual fun checkCameraPermission(): Boolean {
    val context = AppContext.get()
    val permission = android.Manifest.permission.CAMERA
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}


@Composable
actual fun RequestCameraPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    val context = LocalContext.current
    val permission = Manifest.permission.CAMERA

    val activity = context.findAndroidActivity()
        ?: throw IllegalStateException("No activity found for context $context")
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onGranted()
        } else {
            onDenied()
        }
    }

    LaunchedEffect(key1 = true) {

        if (ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onGranted()
        } else {

            cameraPermissionLauncher.launch(permission)
        }
    }
}

private fun Context.findAndroidActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}

actual fun checkStoragePermission(): Boolean {
    val context = AppContext.get()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // handle using mediastore
        true
    } else {
        // For Android 12 and below, check WRITE_EXTERNAL_STORAGE
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}


@Composable
actual fun RequestStoragePermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    val context = LocalContext.current
    val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE

    val activity = context.findAndroidActivity()
        ?: throw IllegalStateException("No activity found for context $context")

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onGranted()
        } else {
            onDenied()
        }
    }

    LaunchedEffect(key1 = true) {

        if (ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onGranted()
        } else {

            storagePermissionLauncher.launch(permission)
        }
    }
}
