package com.kashif.cameraK.utils

import androidx.compose.runtime.Composable


expect fun checkCameraPermission(): Boolean
@Composable
expect fun RequestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit)


expect fun checkStoragePermission(): Boolean

@Composable
expect fun RequestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit)