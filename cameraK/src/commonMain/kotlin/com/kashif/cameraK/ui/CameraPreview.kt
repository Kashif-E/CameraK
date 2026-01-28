package com.kashif.cameraK.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kashif.cameraK.builder.CameraControllerBuilder
import com.kashif.cameraK.controller.CameraController


/**
 * Cross-platform composable function to display the camera preview.
 *
 * @param modifier Modifier to be applied to the camera preview.
 * @param cameraConfiguration Lambda to configure the [CameraControllerBuilder].
 * @param onCameraControllerReady Callback invoked with the initialized [CameraController].
 * 
 * @deprecated Use the new Compose-first API with [rememberCameraKState] and [CameraKScreen] 
 *             for better state management and automatic lifecycle handling. This callback-based 
 *             API will be removed in v2.0.0 (12-month deprecation timeline from v0.2.0).
 * 
 * **Migration Guide:**
 * ```kotlin
 * // Old callback-based API (deprecated)
 * val cameraController = remember { mutableStateOf<CameraController?>(null) }
 * 
 * CameraPreview(
 *     cameraConfiguration = {
 *         setCameraLens(CameraLens.BACK)
 *         setFlashMode(FlashMode.AUTO)
 *     },
 *     onCameraControllerReady = { controller ->
 *         cameraController.value = controller
 *     }
 * )
 * 
 * // New Compose-first API (recommended)
 * val cameraState by rememberCameraKState(
 *     config = CameraConfiguration(
 *         cameraLens = CameraLens.BACK,
 *         flashMode = FlashMode.AUTO
 *     )
 * )
 * 
 * CameraKScreen(cameraState = cameraState) { state ->
 *     // Use state.controller and state.uiState
 *     // Automatic lifecycle, reactive state updates
 * }
 * ```
 * 
 * **Benefits of new API:**
 * - ✅ Automatic lifecycle management (no manual init/cleanup)
 * - ✅ Reactive state updates via StateFlow
 * - ✅ Type-safe state handling (Loading/Ready/Error)
 * - ✅ Built-in error handling
 * - ✅ 50% less boilerplate code
 * - ✅ Fully testable in pure Kotlin
 * 
 * @see com.kashif.cameraK.compose.rememberCameraKState
 * @see com.kashif.cameraK.compose.CameraKScreen
 * @see com.kashif.cameraK.state.CameraKState
 */
@Deprecated(
    message = "Use rememberCameraKState() and CameraKScreen() for Compose-first state management. " +
            "See COMPOSE_API_GUIDE.md for migration guide.",
    replaceWith = ReplaceWith(
        "rememberCameraKState(config = CameraConfiguration(...))",
        "com.kashif.cameraK.compose.rememberCameraKState",
        "com.kashif.cameraK.state.CameraConfiguration"
    ),
    level = DeprecationLevel.WARNING
)
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraConfiguration: CameraControllerBuilder.() -> Unit,
    onCameraControllerReady: (CameraController) -> Unit,
) {
    expectCameraPreview(modifier, cameraConfiguration, onCameraControllerReady)
}

/**
 * Expects platform-specific implementation of [CameraPreview].
 * 
 * @deprecated This is an internal function for the deprecated [CameraPreview] API.
 */
@Deprecated("Internal implementation for deprecated CameraPreview API")
@Composable
expect fun expectCameraPreview(
    modifier: Modifier,
    cameraConfiguration: CameraControllerBuilder.() -> Unit,
    onCameraControllerReady: (CameraController) -> Unit
)