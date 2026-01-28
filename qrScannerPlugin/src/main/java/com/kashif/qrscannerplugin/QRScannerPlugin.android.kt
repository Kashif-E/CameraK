package com.kashif.qrscannerplugin

import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.kashif.cameraK.controller.CameraController
import java.util.EnumMap

/**
 * Enables QR code and barcode scanning on this camera controller.
 *
 * @param onQrScanner Callback invoked when a QR code is detected with the scanned text
 */
fun CameraController.enableQrCodeScanner(onQrScanner: (String) -> Unit) {
    Log.d("QRScanner", "Enabling QR code scanner")
    try {
        imageAnalyzer =
            ImageAnalysis
                .Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        QRCodeAnalyzer(onQrScanner),
                    )
                }

        updateImageAnalyzer()
    } catch (e: Exception) {
        Log.e("QRScanner", "Failed to enable QR scanner: ${e.message}", e)
        // Camera might not be fully initialized yet - this is expected during startup
    }
}

/**
 * Internal analyzer for QR codes and barcodes using ZXing library.
 *
 * Processes camera frames to detect and decode QR codes and common barcode formats
 * (EAN-13, EAN-8, CODE-128, CODE-39, UPC-A, UPC-E). Implements debouncing to prevent
 * duplicate detections within 1 second.
 *
 * @param onQrScanner Callback invoked when a QR code is successfully decoded
 */
private class QRCodeAnalyzer(private val onQrScanner: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val decodeHints =
        EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.CHARACTER_SET, "UTF-8")
            put(
                DecodeHintType.POSSIBLE_FORMATS,
                listOf(
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    com.google.zxing.BarcodeFormat.EAN_13,
                    com.google.zxing.BarcodeFormat.EAN_8,
                    com.google.zxing.BarcodeFormat.CODE_128,
                    com.google.zxing.BarcodeFormat.CODE_39,
                    com.google.zxing.BarcodeFormat.UPC_A,
                    com.google.zxing.BarcodeFormat.UPC_E,
                ),
            )
        }
    private val reader = MultiFormatReader().apply { setHints(decodeHints) }
    private var lastScannedCode: String? = null
    private var lastScanTime: Long = 0
    private val scanDebounceMs = 1000L

    /**
     * Analyzes camera frames to detect QR codes and barcodes.
     *
     * Converts YUV_420_888 frame data to RGB for ZXing processing and applies debouncing
     * to prevent repeated detections of the same code.
     *
     * @param imageProxy The camera frame to analyze
     */
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image == null) {
            imageProxy.close()
            return
        }

        if (image.format != ImageFormat.YUV_420_888) {
            Log.e("QRScanner", "Unsupported image format: ${image.format}")
            imageProxy.close()
            return
        }

        try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val intArray = IntArray(bytes.size) { bytes[it].toInt() and 0xFF }
            val source = RGBLuminanceSource(image.width, image.height, intArray)
            val bitmap = BinaryBitmap(HybridBinarizer(source))

            val result = reader.decode(bitmap)
            val currentTime = System.currentTimeMillis()

            if (result.text != lastScannedCode || (currentTime - lastScanTime) > scanDebounceMs) {
                Log.d("QRScanner", "QR Code detected: ${result.text}")
                lastScannedCode = result.text
                lastScanTime = currentTime
                onQrScanner(result.text)
            }
        } catch (e: Exception) {
            // QR code detection failed - no code found in frame (expected during normal scanning)
        } finally {
            imageProxy.close()
        }
    }
}

actual fun startScanning(controller: CameraController, onQrScanner: (String) -> Unit) {
    Log.d("QRScanner", "Starting QR scanner")
    controller.enableQrCodeScanner(onQrScanner)
}
