package com.kashif.qrscannerplugin

import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import androidx.camera.core.ExperimentalGetImage
import com.kashif.cameraK.controller.CameraController
import kotlinx.atomicfu.AtomicBoolean

fun CameraController.enableQrCodeScanner(onQrScanner: (String) -> Unit) {
    Log.e("QRScanner", "Enabling QR code scanner")
    imageAnalyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build().apply {
            setAnalyzer(
                ContextCompat.getMainExecutor(context),
                QRCodeAnalyzer(onQrScanner)
            )
        }

    updateImageAnalyzer()
}

private class QRCodeAnalyzer(private val onQrScanner: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader()

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        Log.e("QRScanner", "QRCodeAnalyzer.analyze called")
        val image = imageProxy.image ?: return
        if (image.format != ImageFormat.YUV_420_888) {
            Log.e("QRScanner", "Unsupported image format: ${image.format}")
            imageProxy.close()
            return
        }

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val intArray = IntArray(bytes.size) { bytes[it].toInt() and 0xFF }
        val source = RGBLuminanceSource(image.width, image.height, intArray)
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decode(bitmap)
           onQrScanner(result.text)
        } catch (e: Exception) {
            Log.e("QRScanner", "No QR Code detected: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }
}

actual fun startScanning(
    controller: CameraController,
    onQrScanner: (String) -> Unit
) {
    Log.e("QRScanner", "startScanning called")
    controller.enableQrCodeScanner(onQrScanner)
}