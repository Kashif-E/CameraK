package com.kashif.ocrPlugin

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kashif.cameraK.controller.CameraController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

actual suspend fun extractTextFromBitmapImpl(bitmap: ImageBitmap): String =
    withContext(Dispatchers.Default) {
        try {
            suspendCancellableCoroutine { continuation ->
                Log.d("TextRecognition", "Starting text extraction from bitmap")

                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                if (bitmap.width <= 0 || bitmap.height <= 0) {
                    continuation.resumeWithException(IllegalArgumentException("Invalid bitmap dimensions"))
                    return@suspendCancellableCoroutine
                }

                val androidBitmap = bitmap.asAndroidBitmap()
                val image = InputImage.fromBitmap(androidBitmap, 0)

                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        val extractedText = result.text
                        Log.d(
                            "TextRecognition",
                            "Text extracted successfully: ${extractedText.take(100)}..."
                        )
                        continuation.resume(extractedText) { cause, _, _ ->
                            recognizer.close()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("TextRecognition", "Text extraction failed", exception)
                        continuation.resumeWithException(exception)
                    }

                continuation.invokeOnCancellation {
                    recognizer.close()
                }
            }
        } catch (e: Exception) {
            Log.e("TextRecognition", "Error during text extraction", e)
            throw e
        }
    }


internal fun CameraController.enableTextRecognition(
    onTextRecognized: (String) -> Unit,
    onError: (Exception) -> Unit = { Log.e("TextRecognition", "Recognition error", it) }
): TextRecognitionAnalyzer {
    Log.d("TextRecognition", "Configuring text recognition analyzer")

    val analyzer = TextRecognitionAnalyzer(
        onTextRecognized = onTextRecognized,
        onError = onError
    )

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

internal class TextRecognitionAnalyzer(
    private val onTextRecognized: (String) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val processedTexts = mutableSetOf<String>()

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
                imageProxy.imageInfo.rotationDegrees
            )

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    // Avoid duplicate emissions for same text
                    if (text.isNotEmpty() && !processedTexts.contains(text)) {
                        processedTexts.add(text)
                        Log.d("TextRecognition", "Text detected: ${text.take(100)}...")
                        onTextRecognized(text)
                        
                        // Clear cache periodically to avoid memory buildup
                        if (processedTexts.size > 50) {
                            processedTexts.clear()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("TextRecognition", "Recognition failed", exception)
                    onError(exception)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (e: Exception) {
            Log.e("TextRecognition", "Analyzer error", e)
            onError(e)
            imageProxy.close()
        }
    }
    
    fun shutdown() {
        try {
            recognizer.close()
            processedTexts.clear()
        } catch (e: Exception) {
            Log.e("TextRecognition", "Error closing recognizer", e)
        }
    }
}

actual fun startRecognition(
    cameraController: CameraController,
    onText: (text: String) -> Unit
) {
    // Store analyzer reference for cleanup (would need to be stored in a property)
    cameraController.enableTextRecognition(onText)
}