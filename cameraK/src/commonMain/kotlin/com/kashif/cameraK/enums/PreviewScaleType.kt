package com.kashif.cameraK.enums

/**
 * How the camera preview is scaled within its container view.
 *
 * - [FIT_CENTER]: scale the whole frame to fit, letterboxing if the aspect ratios differ.
 *   Maps to `PreviewView.ScaleType.FIT_CENTER` on Android and `AVLayerVideoGravityResizeAspect` on iOS.
 * - [FILL_CENTER]: scale the frame to fill the container, cropping overflow if the aspect ratios differ.
 *   Maps to `PreviewView.ScaleType.FILL_CENTER` on Android and `AVLayerVideoGravityResizeAspectFill` on iOS.
 */
enum class PreviewScaleType {
    FIT_CENTER,
    FILL_CENTER,
}
