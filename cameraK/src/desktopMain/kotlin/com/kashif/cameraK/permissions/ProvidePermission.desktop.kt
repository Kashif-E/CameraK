package com.kashif.cameraK.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Desktop-specific implementation of [Permissions].
 * On desktop platforms, permissions are automatically granted (no permission system).
 */
@Composable
actual fun providePermissions(): Permissions {
    return remember {
        /**
         * Desktop permissions provider - all permissions are automatically allowed.
         */
        object : Permissions {
            override fun hasCameraPermission(): Boolean = true

            override fun hasStoragePermission(): Boolean = true

            @Composable
            override fun RequestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit) {
                onGranted()
            }

            @Composable
            override fun RequestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
                onGranted()
            }
        }
    }
}