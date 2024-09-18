package org.company.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.kashif.cameraK.*
import kotlinx.coroutines.launch
import org.company.app.theme.AppTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun App() = AppTheme {
    val controller = remember { CameraController() }
    val scope = rememberCoroutineScope()
    var imageBitmap: ImageBitmap? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        if (controller.isPermissionGranted()) {
            controller.bindCamera()
        } else {
            requestCameraPermission({}, {})
            requestStoragePermission({}, {})
        }
    }


    Box {

        CameraKPreview(
            modifier = Modifier.fillMaxSize(),
            cameraController = controller
        )
        Row(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart)) {
            Row {
                // Button to toggle flash
                Button(onClick = { controller.toggleFlashMode() }) {
                    Text("Toggle Flash")
                }
                // Button to toggle camera lens
                Button(onClick = { controller.toggleCameraLens() }) {
                    Text("Toggle Camera Lens")
                }
            }

        }
        Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
            Row {

                Button(onClick = {
                    scope.launch {
                        val imageResult = controller.takePicture(ImageFormat.JPEG)

                        when (imageResult) {
                            is ImageCaptureResult.Success -> {
                                imageBitmap = imageResult.image.decodeToImageBitmap()
                                controller.savePicture(imageResult.path, imageResult.image, Directory.PICTURES)
                            }

                            is ImageCaptureResult.Error -> {
                               println("Error: ${imageResult.exception.message}")
                            }
                        }
                    }
                }) {
                    Text("Toggle Flash")
                }
                // Button to toggle camera lens
                Button(onClick = { controller.toggleCameraLens() }) {
                    Text("Toggle Camera Lens")
                }
            }

            imageBitmap?.let {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = "Captured Image",
                    modifier = Modifier.size(50.dp)
                )
            }


        }
    }

}