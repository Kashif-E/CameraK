package com.kashif.cameraK.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

fun Context.getActivityOrNull(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }

    return null
}

/**
 * Compress bitmap to byte array with memory-efficient approach
 * @param format Bitmap format (JPEG, PNG)
 * @param quality Compression quality (0-100)
 * @param recycleInput Whether to recycle the input bitmap after compression
 * @return ByteArray containing the compressed image data
 */
fun Bitmap.compressToByteArray(
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 95,
    recycleInput: Boolean = false,
): ByteArray {
    val initialSize =
        when (format) {
            Bitmap.CompressFormat.JPEG -> width * height / 8
            Bitmap.CompressFormat.PNG -> width * height / 4
            else -> width * height / 6
        }

    val outputStream = ByteArrayOutputStream(initialSize.coerceIn(16384, 1024 * 1024))

    compress(format, quality.coerceIn(0, 100), outputStream)

    if (recycleInput) {
        recycle()
    }

    return outputStream.toByteArray()
}
