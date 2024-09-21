package com.kashif.cameraK


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.AVFoundation.*
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHPhotoLibrary


actual fun checkCameraPermission(): Boolean {
    val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
    return status == AVAuthorizationStatusAuthorized
}

@Composable
actual fun RequestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit) {
    LaunchedEffect(Unit){
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            if (granted) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }

}


actual fun checkStoragePermission(): Boolean {
    val status = PHPhotoLibrary.authorizationStatus()
    return status == PHAuthorizationStatusAuthorized
}


@Composable
actual fun RequestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
    LaunchedEffect(Unit){
        PHPhotoLibrary.requestAuthorization { status ->
            if (status == PHAuthorizationStatusAuthorized) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }
}

