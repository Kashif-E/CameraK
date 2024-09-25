package com.kashif.cameraK.plugins


import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.controller.IosCameraController
import com.kashif.cameraK.utils.InvalidConfigurationException
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.darwin.NSObject

actual fun startScanning(controller: CameraController, onQrScanner: (String) -> Unit) {
    if (controller is IosCameraController) {

        val qrCodeAnalyzer = QRCodeAnalyzer(onQrScanner)

        controller.setMetadataObjectsDelegate(qrCodeAnalyzer)

        if (!controller.getSession().isRunning()) {
            controller.startSession()
        }
    } else {
        throw InvalidConfigurationException("Invalid camera controller type")
    }
}

private class QRCodeAnalyzer(private val onQrScanner: (String) -> Unit) : NSObject(),
    AVCaptureMetadataOutputObjectsDelegateProtocol {

    override fun captureOutput(
        captureOutput: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        for (metadata in didOutputMetadataObjects) {
            if (metadata is AVMetadataMachineReadableCodeObject && metadata.type == AVMetadataObjectTypeQRCode) {
                val qrCode = metadata.stringValue
                if (qrCode != null) {
                    onQrScanner(qrCode)
                }
            }
        }
    }
}

