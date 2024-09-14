package com.kashif.cameraK

import androidx.compose.runtime.Composable


@Composable
expect fun checkCameraPermission(): Boolean

@Composable
expect fun requestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit)

@Composable
expect fun checkStoragePermission(): Boolean

@Composable
expect fun requestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit)