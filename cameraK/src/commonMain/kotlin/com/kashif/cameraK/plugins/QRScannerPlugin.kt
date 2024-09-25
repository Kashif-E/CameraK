package com.kashif.cameraK.plugins

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kashif.cameraK.controller.CameraController
/**
 * A plugin for scanning QR codes using the camera.
 *
 * @property onQrScanner A callback function that is invoked when a QR code is scanned.
 */
class QRScannerPlugin(
    private val onQrScanner: (String) -> Unit
) : CameraPlugin {
    private var cameraController: CameraController? = null

    private var isScanning = false

    /**
     * Initializes the QRScannerPlugin with the given CameraController.
     *
     * @param cameraController The CameraController to be used for scanning.
     */
    override fun initialize(cameraController: CameraController) {
        this.cameraController = cameraController

    }

    /**
     * Starts the QR code scanning process.
     *
     * @throws IllegalStateException If the CameraController is not initialized.
     */
    private fun startScanning() {
        if (isScanning) return
        isScanning = true
        cameraController?.let {
            startScanning(cameraController!!, onQrScanner = onQrScanner)
        } ?: throw IllegalStateException("CameraController is not initialized")
    }

    /**
     * Pauses the QR code scanning process.
     */
    fun pauseScanning() {
        isScanning = false
    }

    /**
     * Resumes the QR code scanning process.
     */
    fun resumeScanning() {
        startScanning()
    }
}

/**
 * Platform-specific function to start scanning for QR codes.
 *
 * @param controller The CameraController to be used for scanning.
 * @param onQrScanner A callback function that is invoked when a QR code is scanned.
 */
expect fun startScanning(controller: CameraController, onQrScanner: (String) -> Unit)

/**
 * Creates and remembers a QRScannerPlugin composable.
 *
 * @param onQrScanner A callback function that is invoked when a QR code is scanned.
 * @return A remembered instance of QRScannerPlugin.
 */
@Composable
fun createQRScannerPlugin(
    onQrScanner: (String) -> Unit,
): QRScannerPlugin {
    return remember {
        QRScannerPlugin(onQrScanner)
    }
}