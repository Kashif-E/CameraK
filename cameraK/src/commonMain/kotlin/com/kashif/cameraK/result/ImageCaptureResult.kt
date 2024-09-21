package com.kashif.cameraK.result


/**
 * Sealed class representing the result of an image capture operation.
 */
sealed class ImageCaptureResult {
    /**
     * Represents a successful image capture.
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
     * Represents a failed image capture.
     *
     * @param exception The exception that occurred during image capture.
     */
    data class Error(val exception: Exception) : ImageCaptureResult()
}