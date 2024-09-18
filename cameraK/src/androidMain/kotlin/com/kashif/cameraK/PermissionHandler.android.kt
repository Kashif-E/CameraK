package com.kashif.cameraK

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
private const val STORAGE_PERMISSION_REQUEST_CODE = 1002


actual fun checkCameraPermission(): Boolean {
    val context = AppContext.get()
    val permission = android.Manifest.permission.CAMERA
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}




actual fun requestCameraPermission( onGranted: () -> Unit, onDenied: () -> Unit) {
    val context = AppContext.get()
    val permission = Manifest.permission.CAMERA

    val activity = context.findAndroidActivity() as ComponentActivity

    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
        onGranted()
    } else {

       val cameraPermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {

                onGranted()
            } else {

                onDenied()
            }
        }

        cameraPermissionLauncher.launch(permission)
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

actual fun checkStoragePermission(): Boolean {
    val context = AppContext.get()
    val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}


actual fun requestStoragePermission( onGranted: () -> Unit, onDenied: () -> Unit) {
    val activity = (AppContext.get()).findAndroidActivity() as ComponentActivity
    val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE


    // Check if permission is already granted
    if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
        onGranted()
    } else {

       val storagePermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onGranted()
            } else {
                onDenied()
            }
        }
        storagePermissionLauncher.launch(permission)
    }
}