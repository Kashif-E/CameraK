package com.kashif.cameraK.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import com.kashif.cameraK.controller.CameraController
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIDeviceOrientationDidChangeNotification

/**
 * iOS implementation of stateless camera preview.
 * Displays the camera feed using AVFoundation preview layer.
 */
@Composable
actual fun CameraPreviewView(controller: CameraController, modifier: Modifier) {
    // Key on controller identity to force recreation when controller changes
    key(controller) {
        DisposableEffect(controller) {
            val notificationCenter = NSNotificationCenter.defaultCenter
            val observer = notificationCenter.addObserverForName(
                UIDeviceOrientationDidChangeNotification,
                null,
                null,
            ) { _ ->
                controller.getCameraPreviewLayer()?.connection?.videoOrientation =
                    controller.currentVideoOrientation()
            }

            onDispose {
                notificationCenter.removeObserver(observer)
            }
        }

        UIKitViewController(
            factory = { controller },
            modifier = modifier,
            update = { viewController ->
                // No update needed - controller manages itself
            },
        )
    }
}
