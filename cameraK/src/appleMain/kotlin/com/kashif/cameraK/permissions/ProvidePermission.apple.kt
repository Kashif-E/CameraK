package com.kashif.cameraK.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary

private class IOSPermissionsImpl : Permissions {
    override fun hasCameraPermission(): Boolean =
        AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized

    override fun hasStoragePermission(): Boolean {
        val status = PHPhotoLibrary.authorizationStatus()
        return status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited
    }

    @Composable
    override fun RequestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit) {
        if (hasCameraPermission()) {
            LaunchedEffect(Unit) { onGranted() }
        } else {
            LaunchedEffect(Unit) {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    if (granted) onGranted() else onDenied()
                }
            }
        }
    }

    @Composable
    override fun RequestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
        if (hasStoragePermission()) {
            LaunchedEffect(Unit) { onGranted() }
        } else {
            LaunchedEffect(Unit) {
                PHPhotoLibrary.requestAuthorization { status ->
                    if (status == PHAuthorizationStatusAuthorized) onGranted() else onDenied()
                }
            }
        }
    }
}

/**
 * Helper function to create and remember the IOSPermissions instance.
 * @deprecated Use providePermissions() instead for consistent cross-platform API
 */
@Deprecated(
    message = "Use providePermissions() instead",
    replaceWith = ReplaceWith("providePermissions()"),
    level = DeprecationLevel.WARNING
)
@Composable
fun rememberIOSPermissions(): Permissions = remember { IOSPermissionsImpl() }

/**
 * Factory function to provide platform-specific [Permissions] implementation.
 */
@Composable
actual fun providePermissions(): Permissions = remember { IOSPermissionsImpl() }