package com.kashif.imageSaverPlugin

import coil3.PlatformContext
import com.kashif.cameraK.utils.toNSData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.UIKit.*
import kotlinx.cinterop.*
import platform.darwin.*

/**
 * iOS-specific implementation of [ImageSaverPlugin].
 *
 * @param config The configuration settings for the plugin.
 * @param onImageSaved Callback invoked when the image is successfully saved.
 * @param onImageSavedFailed Callback invoked when the image saving fails.
 */
class IOSImageSaverPlugin(
    config: ImageSaverConfig,
    private val onImageSaved: () -> Unit,
    private val onImageSavedFailed: (String) -> Unit
) : ImageSaverPlugin(config) {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun saveImage(byteArray: ByteArray, imageName: String?) {
        //    withContext(Dispatchers.Main) {
        val nsData = byteArray.toNSData()

        if (nsData == null) {
            println("Failed to convert ByteArray to NSData.")
            onImageSavedFailed("Failed to convert ByteArray to NSData.")
            return
        }


        val image = UIImage.imageWithData(nsData)


        if (image == null) {
            println("Failed to convert NSData to UIImage.")
            onImageSavedFailed("Failed to create UIImage from NSData.")
            return
        }

        UIImageWriteToSavedPhotosAlbum(
            image, null, null, null
        )

        println("Image successfully saved to Photos album.")
        println("Image saved successfully.")
    }
}

//    /**
//     * Helper class to handle UIImageWriteToSavedPhotosAlbum callbacks.
//     */
//    private class SaveCallback(
//        private val onSuccess: () -> Unit,
//        private val onError: (String) -> Unit
//    ) : NSObject() {
//
//        @OptIn(ExperimentalForeignApi::class)
//        @ObjCAction
//        fun image_didFinishSavingWithError_contextInfo(
//            image: UIImage,
//            error: NSError?,
//            contextInfo: COpaquePointer?
//        ) {
//            if (error == null) {
//                println("Image saved to Photo Library.")
//                onSuccess()
//            } else {
//                println("Failed to save image: ${error.localizedDescription}")
//                onError(error.localizedDescription ?: "Unknown error")
//            }
//        }
//    }


/**
 * Factory function to create an iOS-specific [ImageSaverPlugin].
 *
 * @param config Configuration settings for the plugin.
 * @return An instance of [IOSImageSaverPlugin].
 */

actual fun createPlatformImageSaverPlugin(
    context: PlatformContext, config: ImageSaverConfig
): ImageSaverPlugin {

    return IOSImageSaverPlugin(config = config, onImageSaved = {

        println("Image saved successfully!")
    }, onImageSavedFailed = { errorMessage ->

        println("Failed to save image: $errorMessage")
    })
}