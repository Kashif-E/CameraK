package com.kashif.cameraK

import kotlinx.coroutines.flow.MutableStateFlow

expect class CameraController() {
    var cameraState : MutableStateFlow<CameraState>
        private set
    fun takePicture(imageFormat: ImageFormat) :ImageCaptureResult
    fun savePicture(name: String, file: ByteArray, directory: Directory)
    fun setFlashMode(flashMode: FlashMode)
    fun setCameraLens(lens: CameraLens)
    fun getFlashMode():Int
    fun getCameraLens(): Int
    fun getCameraRotation(): Int
    fun setCameraRotation(rotation: Rotation)
}


sealed class ImageCaptureResult {
    data class Success(val image: ByteArray, val path: String) : ImageCaptureResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Success

            return image.contentEquals(other.image)
        }

        override fun hashCode(): Int {
            return image.contentHashCode()
        }
    }

    data class Error(val exception: Throwable) : ImageCaptureResult()
}
