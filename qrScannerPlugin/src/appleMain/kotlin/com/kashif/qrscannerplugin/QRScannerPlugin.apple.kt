package com.kashif.qrscannerplugin


import com.kashif.cameraK.controller.CameraController
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeFace
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.darwin.NSObject

actual fun startScanning(controller: CameraController, onQrScanner: (String) -> Unit) {
    val qrCodeAnalyzer = QRCodeAnalyzer(onQrScanner)

    controller.setMetadataObjectsDelegate(qrCodeAnalyzer)
    println("QR code scanning started")
    controller.updateMetadataObjectTypes(
        listOf(
            AVMetadataObjectTypeQRCode!!,
            AVMetadataObjectTypeEAN13Code!!,
            AVMetadataObjectTypeEAN8Code!!,
            AVMetadataObjectTypeCode128Code!!,
            AVMetadataObjectTypeFace!!,
            AVMetadataObjectTypePDF417Code!!
        )
    )
    controller.startSession()
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
                println("QR code detected: ${metadata.stringValue}")
                val qrCode = metadata.stringValue
                if (qrCode != null) {
                    println("QR code detected: $qrCode")
                    onQrScanner(qrCode)
                }
            }
        }
    }
}

