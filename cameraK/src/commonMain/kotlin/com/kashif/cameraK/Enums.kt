package com.kashif.cameraK

enum class FlashMode(val value: Int) {
    ON(1), OFF(0)
}

enum class CameraLens(val value: Int) {
    DEFAULT(2), FRONT(0), BACK(1)
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
