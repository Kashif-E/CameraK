package com.kashif.cameraK

expect class CameraController() {
    suspend fun takePicture(imageFormat: ImageFormat) :ImageCaptureResult
    fun savePicture(fileName: String, fileData: ByteArray, directory: Directory)
    fun toggleFlashMode()
    fun toggleCameraLens()
    fun getFlashMode(): FlashMode
    fun getCameraLens(): CameraLens
    fun getCameraRotation(): Int
    fun setCameraRotation(rotation: Rotation)
    fun allPermissionsGranted(): Boolean
    fun bindCamera()

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
