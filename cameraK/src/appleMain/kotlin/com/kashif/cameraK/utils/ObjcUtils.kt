package com.kashif.cameraK.utils

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray()
        : ByteArray {
    val bytes = this.bytes?.reinterpret<ByteVar>()
    return ByteArray(this.length.toInt()) { i -> bytes!![i] }
}

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData? = memScoped {
    val string = NSString.create(string = this@toNSData.decodeToString())
    return string.dataUsingEncoding(NSUTF8StringEncoding)
}