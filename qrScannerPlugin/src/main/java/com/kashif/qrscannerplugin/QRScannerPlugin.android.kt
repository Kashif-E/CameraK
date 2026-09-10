package com.kashif.qrscannerplugin

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.utils.CameraKLogger

/**
 * Enables QR code and barcode scanning on this camera controller.
 *
 * @param onQrScanner Callback invoked when a QR code is detected with the scanned text
 */
fun CameraController.enableQrCodeScanner(onQrScanner: (String) -> Unit): ImageAnalysis.Analyzer? {
    CameraKLogger.d("QRScanner", "Enabling QR code scanner")
    return try {
        val analyzer = QRCodeAnalyzer(onQrScanner)
        registerImageAnalyzer(analyzer)
        analyzer
    } catch (e: Exception) {
        CameraKLogger.e("QRScanner", "Failed to enable QR scanner: ${e.message}", e)
        // Camera might not be fully initialized yet - this is expected during startup
        null
    }
}

/**
 * Internal analyzer for QR codes and barcodes using Google ML Kit Barcode Scanning.
 *
 * Processes camera frames to detect and decode QR codes and common barcode formats
 * (EAN-13, EAN-8, CODE-128, CODE-39, UPC-A, UPC-E). Implements debouncing to prevent
 * duplicate detections within 1 second.
 *
 * @param onQrScanner Callback invoked when a QR code is successfully decoded
 */
private class QRCodeAnalyzer(private val onQrScanner: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val options =
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build()
    private val scanner = BarcodeScanning.getClient(options)
    private var lastScannedCode: String? = null
    private var lastScanTime: Long = 0
    private val scanDebounceMs = 1000L

    /**
     * Analyzes camera frames to detect QR codes and barcodes.
     *
     * @param imageProxy The camera frame to analyze
     */
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        try {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees,
            )

            scanner.process(image).addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue ?: continue
                    val currentTime = System.currentTimeMillis()
                    if (rawValue != lastScannedCode || (currentTime - lastScanTime) > scanDebounceMs) {
                        CameraKLogger.d("QRScanner", "QR Code detected: $rawValue")
                        lastScannedCode = rawValue
                        lastScanTime = currentTime
                        onQrScanner(rawValue)
                        break
                    }
                }
            }.addOnFailureListener { exception ->
                CameraKLogger.e(
                    tag = "QRScanner",
                    message = "QR scanning failed",
                    throwable = exception,
                )
            }.addOnCompleteListener {
                imageProxy.close()
            }
        } catch (e: Exception) {
            CameraKLogger.e(
                tag = "QRScanner",
                message = "Analyzer error",
                throwable = e,
            )
            imageProxy.close()
        }
    }

    fun shutdown() {
        try {
            scanner.close()
        } catch (e: Exception) {
            CameraKLogger.e(
                tag = "QRScanner",
                message = "Error closing scanner",
                throwable = e,
            )
        }
    }
}

actual fun startScanning(controller: CameraController, onQrScanner: (String) -> Unit): ScannerHandle {
    CameraKLogger.d("QRScanner", "Starting QR scanner")
    val analyzer = controller.enableQrCodeScanner(onQrScanner) as? QRCodeAnalyzer
    return ScannerHandle {
        analyzer?.let {
            controller.unregisterImageAnalyzer(it)
            it.shutdown()
        }
    }
}
