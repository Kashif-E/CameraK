package com.kashif.cameraK

import androidx.compose.runtime.Composable


expect fun checkCameraPermission(): Boolean
expect fun requestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit)


expect fun checkStoragePermission(): Boolean


expect fun requestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit)