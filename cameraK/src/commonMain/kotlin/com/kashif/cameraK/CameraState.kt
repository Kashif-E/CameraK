package com.kashif.cameraK

data class CameraState(
    val isActive: Boolean = false,
    val cameraLens: CameraLens = CameraLens.BACK,
    val flashMode: FlashMode = FlashMode.OFF,
    val isPreviewing: Boolean = false,
    val rotation: Rotation = Rotation.ROTATION_0
)