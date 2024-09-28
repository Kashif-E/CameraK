package com.kashif.qrscannerplugin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.plugins.CameraPlugin
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic

/**
 * A plugin for scanning QR codes using the camera.
 *
 * @property onQrScanner A callback function that is invoked when a QR code is scanned.
 */
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class QRScannerPlugin(
    private val coroutineScope: CoroutineScope
) : CameraPlugin {
    private var cameraController: CameraController? = null
    private val qrCodeFlow = MutableSharedFlow<String>()
    private var isScanning = atomic(false)

    /**
     * Initializes the QRScannerPlugin with the given CameraController.
     *
     * @param cameraController The CameraController to be used for scanning.
     */
    override fun initialize(cameraController: CameraController) {
        println("QRScannerPlugin initialized")
        this.cameraController = cameraController
    }

    /**
     * Starts the QR code scanning process.
     *
     * @throws IllegalStateException If the CameraController is not initialized.
     */
    fun startScanning() {
        cameraController?.let { controller ->
            isScanning.value = true
            startScanning(controller = controller) { qrCode ->
                if (isScanning.value) {
                    coroutineScope.launch {
                        qrCodeFlow.emit(qrCode)
                    }
                }
            }
        } ?: throw IllegalStateException("CameraController is not initialized")
    }

    /**
     * Pauses the QR code scanning process.
     */
    fun pauseScanning() {
        isScanning.value = false
    }

    /**
     * Resumes the QR code scanning process.
     */
    fun resumeScanning() {
        isScanning.value = true
        startScanning()
    }

    /**
     * Returns a flow that emits QR codes.
     *
     * @return SharedFlow<String>
     */
    fun getQrCodeFlow(debounce: Long) = qrCodeFlow.asSharedFlow().debounce(debounce)
}

/**
 * Platform-specific function to start scanning for QR codes.
 *
 * @param controller The CameraController to be used for scanning.
 * @param onQrScanner A callback function that is invoked when a QR code is scanned.
 */
expect fun startScanning(
    controller: CameraController,
    onQrScanner: (String) -> Unit
)

/**
 * Creates and remembers a QRScannerPlugin composable.
 *
 * @param onQrScanner A callback function that is invoked when a QR code is scanned.
 * @return A remembered instance of QRScannerPlugin.
 */
@Composable
fun createQRScannerPlugin(
    coroutineScope: CoroutineScope
): QRScannerPlugin {
    return remember {
        QRScannerPlugin(coroutineScope)
    }
}