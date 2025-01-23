package com.kashif.cameraK.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Factory function to provide platform-specific [Permissions] implementation.
 */
@Composable
actual fun providePermissions(): Permissions {
    //not needed
    return remember {
        object : Permissions {
            override fun hasCameraPermission(): Boolean {
                return true
            }

            override fun hasStoragePermission(): Boolean {
                return true
            }

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