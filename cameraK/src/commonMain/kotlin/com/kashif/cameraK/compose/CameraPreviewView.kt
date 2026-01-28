package com.kashif.cameraK.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kashif.cameraK.controller.CameraController

/**
 * Stateless camera preview composable for the new Compose-first API.
 * This is a pure rendering component that displays the camera feed from a [CameraController].
 * 
 * **Usage with new API:**
 * ```kotlin
 * val cameraState by rememberCameraKState()
 * 
 * when (val state = cameraState) {
 *     is CameraKState.Ready -> {
 *         CameraPreviewView(
 *             controller = state.controller,
 *             modifier = Modifier.fillMaxSize()
 *         )
 *     }
 *     else -> {}
 * }
 * ```
 * 
 * @param controller The initialized camera controller.
 * @param modifier Modifier to be applied to the preview.
 */
@Composable
expect fun CameraPreviewView(
    controller: CameraController,
    modifier: Modifier = Modifier
)
