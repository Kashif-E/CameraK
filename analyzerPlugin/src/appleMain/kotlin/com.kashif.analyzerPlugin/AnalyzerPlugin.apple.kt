import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.utils.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.CoreImage.CIContext
import platform.CoreImage.CIImage
import platform.CoreImage.createCGImage
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.UIKit.UIImage
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

actual fun startAnalyzer(
    cameraController: CameraController,
    onFrameAvailable: (ByteArray) -> Unit
) {
    cameraController.enableAnalyzer(onFrameAvailable)
}

@OptIn(ExperimentalForeignApi::class)
internal fun CameraController.enableAnalyzer(
    onFrameAvailable: (ByteArray) -> Unit,
): CameraAnalyzer {
    val analyzer = CameraAnalyzer(onFrameAvailable = onFrameAvailable)

    val output = AVCaptureVideoDataOutput().apply {
        setSampleBufferDelegate(analyzer, dispatch_get_main_queue())
        videoSettings = mapOf(
            kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_32BGRA
        )
    }

    safeAddOutput(output)

    return analyzer
}

internal class CameraAnalyzer(
    private val onFrameAvailable: (ByteArray) -> Unit
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection
    ) {
        val buffer = didOutputSampleBuffer ?: return
        val uiImage = bufferToUIImage(buffer)

        uiImage?.let {
            onFrameAvailable(it.toByteArray())
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun bufferToUIImage(sampleBuffer: CMSampleBufferRef): UIImage? {
        val imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) ?: return null

        val ciImage = CIImage.imageWithCVPixelBuffer(imageBuffer)
        val context = CIContext.contextWithOptions(null)
        val cgImage = context.createCGImage(ciImage, ciImage.extent) ?: return null

        return UIImage.imageWithCGImage(cgImage)
    }
}