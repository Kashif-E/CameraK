package com.kashif.qrscannerplugin

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.awt.image.BufferedImage
import java.util.concurrent.locks.ReentrantLock


class QRScanner {
    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true
        )
        setHints(hints)
    }
    private val lock = ReentrantLock()
    private var lastProcessTime = 0L
    private val processInterval = 200L // Time between scans in milliseconds

    fun scanImage(image: BufferedImage): String? {
        if (!lock.tryLock()) return null

        return try {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastProcessTime < processInterval) {
                return null
            }

            val source = BufferedImageLuminanceSource(image)
            val bitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result = reader.decode(bitmap)
                lastProcessTime = currentTime
                result.text
            } catch (e: NotFoundException) {
                null
            }
        } catch (e: Exception) {
            println("QR scanning error: ${e.message}")
            null
        } finally {
            lock.unlock()
        }
    }
}