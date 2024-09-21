package com.kashif.cameraK.permissions

import androidx.compose.runtime.Composable

/**
 * Interface for requesting permissions in a platform-agnostic way.
 */
interface Permissions {
    /**
     * Requests camera permission.
     *
     * @param onGranted Callback invoked when permission is granted.
     * @param onDenied Callback invoked when permission is denied.
     */
    @Composable
    fun RequestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit)

    /**
     * Requests storage permission.
     *
     * @param onGranted Callback invoked when permission is granted.
     * @param onDenied Callback invoked when permission is denied.
     */

    @Composable
    fun RequestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit)
}