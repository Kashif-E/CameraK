import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.utils.toByteArray

actual fun startAnalyzer(
    cameraController: CameraController,
    onFrameAvailable: (ByteArray) -> Unit
) {
    cameraController.enableAnalyzer(onFrameAvailable)
}
internal fun CameraController.enableAnalyzer(
    onFrameAvailable: (ByteArray) -> Unit,
): CameraAnalyzer {
    val analyzer = CameraAnalyzer(onFrameAvailable = onFrameAvailable)

    imageAnalyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply {
            setAnalyzer(
                ContextCompat.getMainExecutor(context),
                analyzer
            )
        }

    updateImageAnalyzer()
    return analyzer
}

internal class CameraAnalyzer(
    private val onFrameAvailable: (ByteArray) -> Unit,
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }
        onFrameAvailable(image.toByteArray())
    }
}

