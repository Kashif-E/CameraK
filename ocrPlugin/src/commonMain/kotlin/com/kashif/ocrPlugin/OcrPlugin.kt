package com.kashif.ocrPlugin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.plugins.CameraPlugin
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class OcrPlugin(val coroutineScope: CoroutineScope) : CameraPlugin {
    private var cameraController: CameraController? = null
    val ocrFlow = Channel<String>()
    private var isRecognising = atomic(false)

    override fun initialize(cameraController: CameraController) {
        this.cameraController = cameraController
    }

    fun extractTextFromBitmap(textureImage: ImageBitmap) = coroutineScope.launch {
        println("Starting text extraction from bitmap")
        val extractedText = extractTextFromBitmapImpl(textureImage)
        println("Extracted text: $extractedText")
    }

    fun startRecognition() {
        isRecognising.value = true
        startRecognition(cameraController!!) {
            if (isRecognising.value) {
                ocrFlow.trySend(it)
            }
        }

    }

    fun stopRecognition() {
        isRecognising.value = false
    }


}

expect suspend fun extractTextFromBitmapImpl(bitmap: ImageBitmap): String

expect fun startRecognition(cameraController: CameraController, onText: (text: String) -> Unit)

@Composable
fun rememberOcrPlugin(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): OcrPlugin {
    return remember {
        OcrPlugin(coroutineScope)
    }
}