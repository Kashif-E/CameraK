package com.kashif.cameraK.permissions

import androidx.compose.runtime.Composable

/**
 * Interface for requesting permissions in a platform-agnostic way.
 */
interface Permissions {

    /**
     * Checks if the camera permission is granted.
     *
     * @return True if granted, false otherwise.
     */
    fun hasCameraPermission(): Boolean

    /**
     * Checks if the storage permission is granted.
     *
     * @return True if granted or not required, false otherwise.
     */
    fun hasStoragePermission(): Boolean
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