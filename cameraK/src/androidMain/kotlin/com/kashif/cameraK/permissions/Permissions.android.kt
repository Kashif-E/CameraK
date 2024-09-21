package com.kashif.cameraK.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Android-specific implementation of [Permissions] using Activity Result Contracts.
 */
@Composable
actual fun providePermissions(): Permissions {
    val context = LocalContext.current

    return remember {
        object : Permissions {
            @Composable
            override fun RequestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit) {
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (isGranted) {
                            onGranted()
                        } else {
                            onDenied()
                        }
                    }
                )

                val permissionStatus = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                )

                when (permissionStatus) {
                    PackageManager.PERMISSION_GRANTED -> onGranted()
                    PackageManager.PERMISSION_DENIED -> launcher.launch(Manifest.permission.CAMERA)
                }
            }
            @Composable
            override fun RequestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (isGranted) {
                            onGranted()
                        } else {
                            onDenied()
                        }
                    }
                )

                val permissionStatus = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )

                when (permissionStatus) {
                    PackageManager.PERMISSION_GRANTED -> onGranted()
                    PackageManager.PERMISSION_DENIED -> launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }
}