package org.company.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.permissions.Permissions
import com.kashif.cameraK.permissions.providePermissions
import com.kashif.cameraK.plugins.QRScannerPlugin
import com.kashif.cameraK.plugins.createQRScannerPlugin
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.ui.CameraPreview
import com.kashif.imageSaverPlugin.ImageSaverConfig
import com.kashif.imageSaverPlugin.ImageSaverPlugin
import com.kashif.imageSaverPlugin.createImageSaverPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.company.app.theme.AppTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun App() = AppTheme {
    val permissions: Permissions = providePermissions()


    // Initialize Camera Permission State based on current permission status
    val cameraPermissionState = remember {
        mutableStateOf(
            permissions.hasCameraPermission()
        )
    }

    // Initialize Storage Permission State
    val storagePermissionState = remember {
        mutableStateOf(
            permissions.hasStoragePermission()
        )
    }
    val qrScannerPlugin = createQRScannerPlugin {
        println("QR Code Scanned: $it")
    }
    val cameraController = remember { mutableStateOf<CameraController?>(null) }
    val imageSaverPlugin = createImageSaverPlugin(
        config = ImageSaverConfig(
            isAutoSave = false, // Set to true to enable automatic saving
            prefix = "MyApp", // Prefix for image names when auto-saving
            directory = Directory.PICTURES, // Directory to save images
            customFolderName = "CustomFolder" // Custom folder name within the directory, only works on android for now
        )
    )


    if (!cameraPermissionState.value) {
        permissions.RequestStoragePermission(onGranted = { cameraPermissionState.value = true },
            onDenied = {
                println("Camera Permission Denied")
            })
    }


    if (!storagePermissionState.value) {
        permissions.RequestStoragePermission(onGranted = { storagePermissionState.value = true },
            onDenied = {
                println("Storage Permission Denied")
            })
    }

    // Initialize CameraController only when permissions are granted
    if (cameraPermissionState.value && storagePermissionState.value) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.captionBar)
        ) { paddingValues ->
            Box {
                CameraPreview(modifier = Modifier.fillMaxSize(), cameraConfiguration = {
                    setCameraLens(CameraLens.BACK)
                    setFlashMode(FlashMode.OFF)
                    setImageFormat(ImageFormat.JPEG)
                    setDirectory(Directory.PICTURES)
                    addPlugin(imageSaverPlugin)
                    addPlugin(qrScannerPlugin)
                }, onCameraControllerReady = {
                    cameraController.value = it
                })
                cameraController.value?.let { controller ->
                    CameraScreen(cameraController = controller, imageSaverPlugin)
                }

            }

        }
    }
}

@OptIn(ExperimentalResourceApi::class, ExperimentalUuidApi::class)
@Composable
fun CameraScreen(cameraController: CameraController, imageSaverPlugin: ImageSaverPlugin) {
    val scope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Flash Mode Switch
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Flash")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isFlashOn,
                    onCheckedChange = {
                        isFlashOn = it
                        cameraController.toggleFlashMode()
                    }
                )
            }

            // Camera Lens Toggle Button
            Button(onClick = { cameraController.toggleCameraLens() }) {
                Text(text = "Toggle Lens")
            }
        }

        // Capture Button at the Bottom Center
        Button(
            onClick = {
                scope.launch {
                    when (val result = cameraController.takePicture()) {
                        is ImageCaptureResult.Success -> {

                            imageBitmap = result.byteArray.decodeToImageBitmap()
                            // If auto-save is disabled, manually save the image
                            if (!imageSaverPlugin.config.isAutoSave) {
                                // Generate a custom name or use default
                                val customName = "Manual_${Uuid.random().toHexString()}"
                                imageSaverPlugin.saveImage(
                                    byteArray = result.byteArray,
                                    imageName = customName
                                )
                            }
                        }

                        is ImageCaptureResult.Error -> {
                            println("Image Capture Error: ${result.exception.message}")
                        }
                    }
                }
            },
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .align(Alignment.BottomCenter)

        ) {
            Text(text = "Capture")
        }

        // Display the captured image
        imageBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Captured Image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )

            LaunchedEffect(bitmap) {
                delay(3000)
                imageBitmap = null
            }
        }
    }
}