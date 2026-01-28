package com.kashif.cameraK.compose

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kashif.cameraK.builder.createIOSCameraControllerBuilder
import com.kashif.cameraK.state.CameraConfiguration
import com.kashif.cameraK.state.CameraKState
import com.kashif.cameraK.state.CameraKStateHolder

/**
 * iOS implementation of [rememberCameraKState].
 */
@Composable
actual fun rememberCameraKState(
    config: CameraConfiguration,
    setupPlugins: suspend (CameraKStateHolder) -> Unit
): State<CameraKState> {
    val scope = rememberCoroutineScope()
    
    val stateHolder = remember(config) {
        CameraKStateHolder(
            cameraConfiguration = config,
            controllerFactory = {
                createIOSCameraControllerBuilder()
                    .apply {
                        setFlashMode(config.flashMode)
                        setTorchMode(config.torchMode)
                        setCameraLens(config.cameraLens)
                        setImageFormat(config.imageFormat)
                        setQualityPrioritization(config.qualityPrioritization)
                        setPreferredCameraDeviceType(config.cameraDeviceType)
                        setAspectRatio(config.aspectRatio)
                        setDirectory(config.directory)
                        config.targetResolution?.let { (width, height) ->
                            setResolution(width, height)
                        }
                    }
                    .build()
            },
            coroutineScope = scope
        )
    }
    
    // Initialize controller and plugins
    LaunchedEffect(stateHolder) {
        setupPlugins(stateHolder)
        stateHolder.initialize()
    }
    
    // Cleanup on disposal
    DisposableEffect(stateHolder) {
        onDispose {
            stateHolder.shutdown()
        }
    }
    
    return stateHolder.cameraState.collectAsStateWithLifecycle()
}
