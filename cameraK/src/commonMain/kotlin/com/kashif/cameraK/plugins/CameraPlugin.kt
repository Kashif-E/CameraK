package com.kashif.cameraK.plugins

import com.kashif.cameraK.controller.CameraController

/**
 * Legacy interface that all camera plugins must implement.
 *
 * @deprecated Use the new [com.kashif.cameraK.state.CameraKPlugin] interface for better
 *             integration with Compose-first state management. This interface will be
 *             removed in v2.0.0 (12-month deprecation timeline).
 *
 * **Migration Guide:**
 * ```kotlin
 * // Old way
 * class MyPlugin : CameraPlugin {
 *     override fun initialize(cameraController: CameraController) {
 *         // Setup with controller
 *     }
 * }
 *
 * // New way
 * class MyPlugin : CameraKPlugin {
 *     private var stateHolder: CameraKStateHolder? = null
 *
 *     override fun onAttach(stateHolder: CameraKStateHolder) {
 *         this.stateHolder = stateHolder
 *         val controller = stateHolder.getController()
 *         // Setup with controller and access to state
 *     }
 *
 *     override fun onDetach() {
 *         // Cleanup
 *         stateHolder = null
 *     }
 * }
 * ```
 *
 * @see com.kashif.cameraK.state.CameraKPlugin
 */
@Deprecated(
    message = "Use CameraKPlugin from com.kashif.cameraK.state package for Compose-first state management",
    replaceWith = ReplaceWith(
        "CameraKPlugin",
        "com.kashif.cameraK.state.CameraKPlugin",
    ),
    level = DeprecationLevel.WARNING,
)
interface CameraPlugin {
    /**
     * Initializes the plugin with the provided [CameraController].
     *
     * @param cameraController The [CameraController] instance.
     */
    fun initialize(cameraController: CameraController)
}
