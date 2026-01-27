package com.kashif.qrscannerplugin

import com.kashif.cameraK.controller.CameraController
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Mod43Code
import platform.AVFoundation.AVMetadataObjectTypeCode93Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeITF14Code
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.darwin.NSObject


sealed class ScannedCode {
    abstract val value: String
    abstract val type: String

    data class QR(override val value: String) : ScannedCode() {
        override val type: String = "QR_CODE"
    }

    data class Barcode(
        override val value: String,
        override val type: String
    ) : ScannedCode()

    companion object {
        fun fromAVMetadata(metadata: AVMetadataMachineReadableCodeObject): ScannedCode? {
            val value = metadata.stringValue ?: return null
            return when (metadata.type) {
                AVMetadataObjectTypeQRCode -> QR(value)
                AVMetadataObjectTypeEAN13Code -> Barcode(value, "EAN_13")
                AVMetadataObjectTypeEAN8Code -> Barcode(value, "EAN_8")
                AVMetadataObjectTypeCode128Code -> Barcode(value, "CODE_128")
                AVMetadataObjectTypeCode39Code -> Barcode(value, "CODE_39")
                AVMetadataObjectTypeCode93Code -> Barcode(value, "CODE_93")
                AVMetadataObjectTypeCode39Mod43Code -> Barcode(value, "CODE_39_MOD_43")
                AVMetadataObjectTypeEAN13Code -> Barcode(value, "EAN_13")
                AVMetadataObjectTypeEAN8Code -> Barcode(value, "EAN_8")
                AVMetadataObjectTypeITF14Code -> Barcode(value, "ITF_14")
                AVMetadataObjectTypePDF417Code -> Barcode(value, "PDF_417")
                AVMetadataObjectTypeAztecCode -> Barcode(value, "AZTEC")
                AVMetadataObjectTypeDataMatrixCode -> Barcode(value, "DATA_MATRIX")
                AVMetadataObjectTypeUPCECode -> Barcode(value, "UPC_E")
                else -> null
            }
        }
    }
}

actual fun startScanning(
    controller: CameraController,
    onQrScanner: (String) -> Unit
) {
    val codeAnalyzer = CodeAnalyzer(onCodeScanned = {
        onQrScanner(it.value)
    })
    controller.setMetadataObjectsDelegate(codeAnalyzer)


    controller.updateMetadataObjectTypes(
        listOf(
            AVMetadataObjectTypeQRCode!!,
            AVMetadataObjectTypeEAN13Code!!,
            AVMetadataObjectTypeEAN8Code!!,
            AVMetadataObjectTypeCode128Code!!,
            AVMetadataObjectTypeCode39Code!!,
            AVMetadataObjectTypeCode93Code!!,
            AVMetadataObjectTypeCode39Mod43Code!!,
            AVMetadataObjectTypeITF14Code!!,
            AVMetadataObjectTypePDF417Code!!,
            AVMetadataObjectTypeAztecCode!!,
            AVMetadataObjectTypeDataMatrixCode!!,
            AVMetadataObjectTypeUPCECode!!
        )
    )
    controller.startSession()
}

private class CodeAnalyzer(
    private val onCodeScanned: (ScannedCode) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    private val isProcessing = atomic(false)
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun captureOutput(
        captureOutput: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        if (isProcessing.value) return

        for (metadata in didOutputMetadataObjects) {
            if (metadata !is AVMetadataMachineReadableCodeObject) continue
            val scannedCode = ScannedCode.fromAVMetadata(metadata) ?: continue
            processCode(scannedCode)
        }
    }

    private fun processCode(code: ScannedCode) {
        scope.launch {
            if (isProcessing.compareAndSet(expect = false, update = true)) {
                try {
                    onCodeScanned(code) // emit continuously without distinct (#47)
                } finally {
                    isProcessing.value = false
                }
            }
        }
    }
}