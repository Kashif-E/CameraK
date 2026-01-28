import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.plugins.CameraPlugin
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.Channel

class AnalyzerPlugin : CameraPlugin {
    private var cameraController: CameraController? = null
    val analyzerFlow = Channel<ByteArray>()
    private var isAnalyzing = atomic(false)

    override fun initialize(cameraController: CameraController) {
        this.cameraController = cameraController
    }

    fun startAnalyzer() {
        isAnalyzing.value = true
        startAnalyzer(cameraController!!) {
            if (isAnalyzing.value)
                analyzerFlow.trySend(it)
        }
    }

    fun stopAnalyzer(){
        isAnalyzing.value = false
        stopAnalyzer(cameraController!!)
    }
}

expect fun startAnalyzer(cameraController: CameraController, onFrameAvailable: (ByteArray) -> Unit)
expect fun stopAnalyzer(cameraController: CameraController)