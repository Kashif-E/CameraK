package com.kashif.cameraK

enum class FlashMode(val value: Int) {
    ON(1), OFF(0)
}


enum class CameraLens(val value: Int) {
    DEFAULT(2), FRONT(0), BACK(1)
}

fun Int.toCameraLens(): CameraLens {
    return when (this) {
        0 -> CameraLens.FRONT
        1 -> CameraLens.BACK
        else -> CameraLens.DEFAULT
    }
}

enum class ImageFormat(val extension: String) {
    JPEG(".jpeg"), PNG(".png"), RAW(".raw")
}

enum class Directory(val value: String) {
    PICTURES("Pictures"), DCIM("DCIM")
}
enum class Rotation(val value: Int) {
    ROTATION_0(0), ROTATION_90(1), ROTATION_180(2), ROTATION_270(3)
}

fun Int.toRotation(): Rotation {
    return when (this) {
        0 -> Rotation.ROTATION_0
        1 -> Rotation.ROTATION_90
        2 -> Rotation.ROTATION_180
        3 -> Rotation.ROTATION_270
        else -> Rotation.ROTATION_0
    }
}
