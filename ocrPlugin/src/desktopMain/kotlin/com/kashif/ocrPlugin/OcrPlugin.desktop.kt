package com.kashif.ocrPlugin

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import com.kashif.cameraK.controller.CameraController
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bytedeco.leptonica.PIX
import org.bytedeco.leptonica.global.leptonica.pixCreate
import org.bytedeco.leptonica.global.leptonica.pixSetPixel
import org.bytedeco.opencv.global.opencv_text.PSM_AUTO
import org.bytedeco.tesseract.TessBaseAPI
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte


class OCRProcessor {
    private val api = TessBaseAPI()
    private val lock = ReentrantLock()
    private var lastProcessTime = 0L
    private val processInterval = 200L

    init {

        if (api.Init(
                "/Users/vyro/IdeaProjects/CameraK/ocrPlugin/src/desktopMain/kotlin/com/kashif/ocrPlugin",
                "eng"
            ) != 0
        ) {
            throw IllegalStateException("Could not initialize Tesseract")
        }


        api.SetVariable(
            "tessedit_char_whitelist",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,!?"
        )
        api.SetPageSegMode(PSM_AUTO)
    }

    fun scanImage(image: BufferedImage): String? {
        if (!lock.tryLock()) return null

        return try {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastProcessTime < processInterval) {
                return null
            }


            val processedImage = preprocessImage(image)


            val pix = convertImageToPix(processedImage)

            api.SetImage(pix)
            val result = api.GetUTF8Text()?.getString()?.trim()
            lastProcessTime = currentTime

            result.takeIf { !it.isNullOrBlank() }
        } catch (e: Exception) {
            println("OCR scanning error: ${e.message}")
            null
        } finally {
            lock.unlock()
        }
    }

    private fun preprocessImage(image: BufferedImage): BufferedImage {

        val grayImage = BufferedImage(
            image.width,
            image.height,
            BufferedImage.TYPE_BYTE_GRAY
        )

        val g = grayImage.graphics
        g.drawImage(image, 0, 0, null)
        g.dispose()

        return grayImage
    }

    private fun convertImageToPix(image: BufferedImage): PIX {
        val width = image.width
        val height = image.height
        val pix = pixCreate(width, height, 8)

        val raster = image.raster
        val data = raster.dataBuffer as DataBufferByte
        val pixels = data.data

        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = pixels[y * width + x].toInt() and 0xFF
                pixSetPixel(pix, x, y, value)
            }
        }

        return pix
    }

    fun close() {
        api.End()
    }
}


actual suspend fun extractTextFromBitmapImpl(bitmap: ImageBitmap): String {
    val ocrProcessor = OCRProcessor()
    return ocrProcessor.scanImage(bitmap.toAwtImage()) ?: ""
}

actual fun startRecognition(
    cameraController: CameraController,
    onText: (text: String) -> Unit
) {
    val ocrProcessor = OCRProcessor()
    val scope = CoroutineScope(Dispatchers.Default)

    scope.launch {
        cameraController.getFrameChannel().consumeAsFlow().collect { image ->
            ocrProcessor.scanImage(image)?.let { text ->
                withContext(Dispatchers.Main) {
                    onText(text)
                }
            }
        }
    }
}

