package com.kashif.cameraK.video

import androidx.compose.runtime.Immutable

/**
 * Sealed class representing the result of a completed video recording.
 * Mirrors [com.kashif.cameraK.result.ImageCaptureResult] in structure.
 */
@Immutable
sealed class VideoCaptureResult {

    /**
     * Recording completed successfully.
     *
     * @property filePath Absolute path to the saved video file.
     * @property durationMs Actual recorded duration in milliseconds.
     */
    @Immutable
    data class Success(val filePath: String, val durationMs: Long) : VideoCaptureResult()

    /**
     * Recording failed or was cancelled.
     *
     * @property exception The underlying cause.
     */
    @Immutable
    data class Error(val exception: Exception) : VideoCaptureResult()
}
