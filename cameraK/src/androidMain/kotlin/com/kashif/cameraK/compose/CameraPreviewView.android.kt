package com.kashif.cameraK.compose

import android.content.res.Configuration
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.DeviceOrientation
import com.kashif.cameraK.enums.PreviewScaleType
import com.kashif.cameraK.enums.previewAspectRatio

@Composable
actual fun CameraPreviewView(
    controller: CameraController,
    modifier: Modifier,
    deviceOrientation: DeviceOrientation,
    overlay: @Composable (CameraPreviewScope.() -> Unit)?,
) {
    val context = LocalContext.current
    val scaleType = controller.getPreviewScaleType()
    val viewScaleType = when (scaleType) {
        PreviewScaleType.FIT_CENTER -> PreviewView.ScaleType.FIT_CENTER
        PreviewScaleType.FILL_CENTER -> PreviewView.ScaleType.FILL_CENTER
    }
    // Created without a scale type: it is applied in the AndroidView update block below so a change
    // takes effect on the existing view. Setting it here (in a keyless remember) would freeze the
    // value at first composition, since a config change reuses this same PreviewView.
    val previewView = remember { PreviewView(context) }

    DisposableEffect(controller, previewView) {
        controller.bindCamera(previewView) {}
        onDispose {}
    }

    // Aspect ratio the preview is sized to under FIT_CENTER, so it matches the capture. Drive
    // portrait vs landscape from the actual screen orientation (recomposes on rotation) rather than
    // the caller's deviceOrientation, which defaults to PORTRAIT and would otherwise mis-size a
    // landscape preview.
    val screenOrientation = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        DeviceOrientation.LANDSCAPE_LEFT
    } else {
        DeviceOrientation.PORTRAIT
    }
    val ratio = controller.getAspectRatio().previewAspectRatio(screenOrientation)

    // The camera output is already cropped to the configured aspect ratio by the UseCaseGroup's
    // ViewPort, so PreviewView.ScaleType only does something when the view it fills has a different
    // shape than that output:
    //  - FIT_CENTER sizes the view to the configured ratio, so preview FOV == captured FOV (#136,
    //    #119). Any letterboxing happens between this box and the caller's bounds.
    //  - FILL_CENTER lets the view fill the caller's bounds and crops the output to cover it, so
    //    there are no black bars (#163) at the cost of the preview showing less than it captures.
    val previewModifier = when (scaleType) {
        PreviewScaleType.FIT_CENTER -> Modifier.aspectRatio(ratio)
        PreviewScaleType.FILL_CENTER -> Modifier.fillMaxSize()
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Preview and overlay share one box so overlay coordinates (focus reticles, bounding boxes)
        // align with the preview frame that is actually on screen, not the outer bounds.
        Box(modifier = previewModifier) {
            AndroidView(
                factory = { previewView },
                update = { it.scaleType = viewScaleType },
                modifier = Modifier.matchParentSize(),
            )
            if (overlay != null) {
                val scope = CameraPreviewScopeImpl(this, deviceOrientation)
                scope.overlay()
            }
        }
    }
}
