package org.company.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role.Companion.Switch
import androidx.compose.ui.unit.dp
import com.kashif.cameraK.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.company.app.theme.AppTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun App() = AppTheme {
    val controller = remember { CameraController() }
    val scope = rememberCoroutineScope()

    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var storagePermissionGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {

        if (controller.allPermissionsGranted()) {
            cameraPermissionGranted = true
            storagePermissionGranted = true
            controller.bindCamera()

        }
    }

    if (!cameraPermissionGranted) {
        RequestCameraPermission(
            onGranted = {
                cameraPermissionGranted = true
                if (storagePermissionGranted) {
                    controller.bindCamera()
                }
            },
            onDenied = {
                // Handle the case where camera permission is denied
            }
        )
    }

    if (!storagePermissionGranted) {
        RequestStoragePermission(
            onGranted = {
                storagePermissionGranted = true
                if (cameraPermissionGranted) {
                    controller.bindCamera()
                }
            },
            onDenied = {
                // Handle the case where storage permission is denied
            }
        )
    }


    CameraK(controller, scope)

}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun CameraK(
    controller: CameraController,
    scope: CoroutineScope,
) {
    var imageBitmap: ImageBitmap? by remember { mutableStateOf(null) }

    val flashMode = remember(controller.getFlashMode()) {
        controller.getFlashMode() == FlashMode.ON
    }
    Box(modifier = Modifier.fillMaxSize()) {

        CameraKPreview(
            modifier = Modifier.fillMaxSize(),
            cameraController = controller
        )
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Switch(
                checked = flashMode,
                onCheckedChange = { controller.toggleFlashMode() }
            )

            Button(onClick = { controller.toggleCameraLens() }) {
                Text("Toggle Camera Lens")
            }
        }

        Button(onClick = {
            scope.launch {
                when (val result = controller.takePicture(ImageFormat.PNG)) {
                    is ImageCaptureResult.Success -> {
                        imageBitmap = result.image.decodeToImageBitmap()

                        controller.savePicture("image.png", result.image, Directory.PICTURES)
                    }

                    is ImageCaptureResult.Error -> {
                        println(result.exception.message ?: "Error")
                    }
                }
            }

        }, modifier = Modifier.clip(CircleShape).align(Alignment.BottomCenter).padding(16.dp)) {
            Text("Capture")
        }
    }
}