package com.kashif.cameraK

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import platform.Foundation.*
import platform.Photos.*

actual class CameraController actual constructor() {
    private val cameraView = CameraView()
    actual var cameraState: MutableStateFlow<CameraState> = MutableStateFlow(CameraState())

    @OptIn(ExperimentalForeignApi::class)
    actual fun takePicture(imageFormat: ImageFormat): ImageCaptureResult = runBlocking {
        try {
            val photo = cameraView.capturePhoto()
            if (photo != null) {
                val byteArray = photo.toByteArray()
                ImageCaptureResult.Success(byteArray, "")
            } else {
                ImageCaptureResult.Error(Exception("Failed to capture photo"))
            }
        } catch (e: Exception) {
            ImageCaptureResult.Error(e)
        }
    }


    actual fun savePicture(name: String, file: ByteArray, directory: Directory) {
        when (directory) {
            Directory.DCIM -> {
                val fileManager = NSFileManager.defaultManager
                val paths = fileManager.URLsForDirectory(NSPicturesDirectory, inDomains = NSUserDomainMask)
                val documentsDirectory = paths.first() as NSURL
                val fileURL = documentsDirectory.URLByAppendingPathComponent(name)

                fileURL?.let {
                    val nsData = file.toNSData()
                    nsData.writeToURL(it, atomically = true)
                }
            }

            Directory.PICTURES -> {
                val nsData = file.toNSData()
                PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                    val request = PHAssetCreationRequest.creationRequestForAsset()
                    request.addResourceWithType(PHAssetResourceTypePhoto, data = nsData, options = null)
                }, completionHandler = { success, error ->
                    if (!success) {
                        println("Error saving photo to library: ${error?.localizedDescription}")
                    }
                })
            }
        }
    }


    actual fun setFlashMode(flashMode: FlashMode) {
        cameraView.setFlashMode(flashMode)
    }

    actual fun setCameraLens(lens: CameraLens) {
        cameraView.setCameraLens(lens)
    }

    actual fun getFlashMode(): Int {
        return cameraState.value.flashMode.value
    }

    actual fun getCameraLens(): Int {
        return cameraState.value.cameraLens.value
    }

    actual fun getCameraRotation(): Int {
        return cameraState.value.rotation.value
    }

    @Composable
    fun CameraPreview(modifier: Modifier) {

        UIKitView(
            factory = { cameraView },
            modifier = modifier,
            properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true)
        )


    }

    actual fun setCameraRotation(rotation: Rotation) {
        cameraView.setRotation(rotation)
    }

}

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData() = usePinned {
    NSData.create(bytes = it.addressOf(0), this.size.convert())
}
