package com.kashif.cameraK.result


/**
 * Sealed class representing the result of an image capture operation.
 */
sealed class ImageCaptureResult {
    /**
     * Represents a successful image capture with ByteArray data.
     *
     * @param byteArray The captured image data as a [ByteArray].
     */
    data class Success(val byteArray: ByteArray) : ImageCaptureResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Success

            return byteArray.contentEquals(other.byteArray)
        }

        override fun hashCode(): Int {
            return byteArray.contentHashCode()
        }
    }

    /**
     * Represents a successful image capture with file path.
     * Use this for maximum performance when you need the file instead of ByteArray.
     * Skips all processing and file reading - fastest option.
     *
     * @param filePath The absolute path to the captured image file.
     */
    data class SuccessWithFile(val filePath: String) : ImageCaptureResult()

    /**
     * Represents a failed image capture.
     *
     * @param exception The exception that occurred during image capture.
     */
    data class Error(val exception: Exception) : ImageCaptureResult()
}