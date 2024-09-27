package com.kashif.cameraK.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
public fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toNSData),
        length = this@toNSData.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
public fun NSData.toByteArray(): ByteArray = ByteArray(this@toByteArray.length.toInt()).apply {
    usePinned {
        memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
    }
}
fun NSData.toUIImage() = UIImage(this)