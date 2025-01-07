package com.kashif.imagesaverplugin

import coil3.PlatformContext
import com.kashif.cameraK.utils.toByteArray
import com.kashif.cameraK.utils.toNSData
import io.ktor.utils.io.errors.IOException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSCondition
import platform.Foundation.NSData
import platform.Photos.PHAsset
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsVersionCurrent
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIImage

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

    override suspend fun saveImage(byteArray: ByteArray, imageName: String?): String? {
        return withContext(Dispatchers.Main) {
            try {
                val nsData = byteArray.toNSData()

                if (nsData == null) {
                    println("Failed to convert ByteArray to NSData.")
                    onImageSavedFailed("Failed to convert ByteArray to NSData.")
                    return@withContext null
                }

                val image = UIImage.imageWithData(nsData)

                if (image == null) {
                    println("Failed to convert NSData to UIImage.")
                    onImageSavedFailed("Failed to create UIImage from NSData.")
                    return@withContext null
                }

                var assetId: String? = null
                val semaphore = NSCondition()

                PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                    val request = PHAssetChangeRequest.creationRequestForAssetFromImage(image)
                    assetId = request.placeholderForCreatedAsset?.localIdentifier
                }) { success, error ->
                    if (success && assetId != null) {
                        println("Image successfully saved to Photos album with ID: $assetId")
                    } else {
                        println("Failed to save image: ${error?.localizedDescription}")
                        assetId = null
                    }
                    semaphore.signal()
                }

                semaphore.wait()
                assetId
            } catch (e: Exception) {
                println("Exception while saving image: ${e.message}")
                null
            }
        }
    }

    override fun getByteArrayFrom(path: String): ByteArray {
        val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(path), null)
        if (fetchResult.count.toInt() == 0) {
            throw IOException("No asset found with identifier: $path")
        }

        val asset = fetchResult.firstObject as PHAsset
        val options = PHImageRequestOptions().apply {
            synchronous = true
            deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
            networkAccessAllowed = false
            version = PHImageRequestOptionsVersionCurrent
        }

        var imageData: NSData? = null
        val semaphore = NSCondition()

        PHImageManager.defaultManager().requestImageDataAndOrientationForAsset(
            asset,
            options
        ) { data, _, _, _ ->
            imageData = data
            semaphore.signal()
        }

        semaphore.wait()

        if (imageData == null) {
            throw IOException("Failed to get image data from asset: $path")
        }


        return imageData?.toByteArray() ?: byteArrayOf()
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