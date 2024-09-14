package com.kashif.cameraK


import androidx.compose.runtime.Composable
import platform.AVFoundation.*
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication

@Composable
actual fun checkCameraPermission(): Boolean {
    val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
    return status == AVAuthorizationStatusAuthorized
}

@Composable
actual fun requestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit) {
    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
        if (granted) {
            onGranted()
        } else {
            onDenied()
        }
    }
}

@Composable
actual fun checkStoragePermission(): Boolean {
    val status = PHPhotoLibrary.authorizationStatus()
    return status == PHAuthorizationStatusAuthorized
}

@Composable
actual fun requestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
    PHPhotoLibrary.requestAuthorization { status ->
        if (status == PHAuthorizationStatusAuthorized) {
            onGranted()
        } else {
            onDenied()
        }
    }
}