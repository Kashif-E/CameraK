package com.kashif.cameraK.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.requestAccessForMediaType
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHPhotoLibrary

/**
 * iOS-specific implementation of [Permissions].
 */
@Composable
actual fun providePermissions(): Permissions {
    return rememberIOSPermissions()
}

/**
 * Helper function to create and remember the IOSPermissions instance.
 */
@Composable
fun rememberIOSPermissions(): Permissions {
    return remember {
        object : Permissions {
            @Composable
            override fun RequestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit) {
                AVCaptureDevice.requestAccessForMediaType(
                    AVMediaTypeVideo
                ) { granted ->
                    if (granted) {
                        onGranted()
                    } else {
                        onDenied()
                    }
                }
            }
            @Composable
            override fun RequestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
                // iOS doesn't have explicit storage permissions.
                // Photos access is needed if saving to the photo library.
                PHPhotoLibrary.requestAuthorization { status ->
                    when (status) {
                        PHAuthorizationStatusAuthorized-> onGranted()
                        else -> onDenied()
                    }
                }
            }
        }
    }
}
