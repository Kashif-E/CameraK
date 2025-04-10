package com.kashif.cameraK.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.posix.memcpy

fun ImageBitmap.toByteArray(): ByteArray? {
    val skiaBitmap = this.asSkiaBitmap()
    val skiaImage: Image = Image.makeFromBitmap(skiaBitmap)

    val encodedData: Data? = skiaImage.encodeToData(quality = 100)
    return encodedData?.bytes
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(
        bytes = allocArrayOf(this@toNSData),
        length = this@toNSData.size.toULong()
    )
}

/**
 * Converts NSData to ByteArray with optional buffer reuse for better memory efficiency
 * 
 * @param reuseBuffer Optional pre-allocated buffer to use if large enough
 * @return ByteArray containing the data
 */
@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(reuseBuffer: ByteArray? = null): ByteArray {
    val length = this.length.toInt()
    

    val buffer = if (reuseBuffer != null && reuseBuffer.size >= length) {
        reuseBuffer
    } else {
        ByteArray(length)
    }
    
    buffer.usePinned {
        memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
    }
    
    return buffer
}


@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray = toByteArray(null)

fun NSData.toUIImage() = UIImage(this)