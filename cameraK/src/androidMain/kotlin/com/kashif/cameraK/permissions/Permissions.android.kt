package com.kashif.cameraK.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
            override fun hasCameraPermission(): Boolean {
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }

            override fun hasStoragePermission(): Boolean {
                return if (Build.VERSION.SDK_INT >= 32) {
                    // Storage permission not required from API level 32 onwards
                    true
                } else {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }


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
                    PackageManager.PERMISSION_DENIED -> {
                        // Ensure that launcher.launch is called within a LaunchedEffect or similar
                        LaunchedEffect(Unit) {
                            launcher.launch(Manifest.permission.CAMERA)
                        }
                    }
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
                    PackageManager.PERMISSION_DENIED -> {
                        LaunchedEffect(Unit) {
                            launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                }
            }
        }
    }
}