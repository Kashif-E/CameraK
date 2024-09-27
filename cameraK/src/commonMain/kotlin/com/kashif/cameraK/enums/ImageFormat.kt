package com.kashif.cameraK.enums

/**
 * Enum representing the image format for captured photos.
 */
enum class ImageFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/jpeg")
}