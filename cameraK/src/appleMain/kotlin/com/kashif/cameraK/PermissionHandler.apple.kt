package com.kashif.cameraK


import platform.AVFoundation.*
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHPhotoLibrary


actual fun checkCameraPermission(): Boolean {
    val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
    return status == AVAuthorizationStatusAuthorized
}


actual fun requestCameraPermission(onGranted: () -> Unit, onDenied: () -> Unit) {
    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
        if (granted) {
            onGranted()
        } else {
            onDenied()
        }
    }
}


actual fun checkStoragePermission(): Boolean {
    val status = PHPhotoLibrary.authorizationStatus()
    return status == PHAuthorizationStatusAuthorized
}


actual fun requestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
    PHPhotoLibrary.requestAuthorization { status ->
        if (status == PHAuthorizationStatusAuthorized) {
            onGranted()
        } else {
            onDenied()
        }
    }
}

