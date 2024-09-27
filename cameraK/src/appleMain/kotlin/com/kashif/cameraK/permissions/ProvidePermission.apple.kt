package com.kashif.cameraK.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary


/**
 * Helper function to create and remember the IOSPermissions instance.
 */
@Composable
fun rememberIOSPermissions(): Permissions {
    return remember {
        object : Permissions {

            /**
             * Checks if the camera permission is granted.
             */
            override fun hasCameraPermission(): Boolean {
                val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
                return status == AVAuthorizationStatusAuthorized
            }

            /**
             * Checks if the Photo Library access is granted or limited.
             */
            override fun hasStoragePermission(): Boolean {
                val status = PHPhotoLibrary.authorizationStatus()
                return status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited
            }

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

/**
 * Factory function to provide platform-specific [Permissions] implementation.
 */
@Composable
actual fun providePermissions(): Permissions {
    return rememberIOSPermissions()
}