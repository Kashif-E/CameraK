package com.kashif.cameraK.plugins.imageSaverPlugin

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import coil3.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Android-specific implementation of [ImageSaverPlugin] using Scoped Storage.
 *
 * @param context The Android [Context] used to access ContentResolver.
 * @param config The configuration settings for the plugin.
 */
class AndroidImageSaverPlugin(
    private val context: Context,
    config: ImageSaverConfig
) : ImageSaverPlugin(config) {

    override suspend fun saveImage(byteArray: ByteArray, imageName: String?) {
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = imageName ?: "IMG_$timeStamp"
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    "$fileName.${config.imageFormat.extension}"
                )
                put(MediaStore.MediaColumns.MIME_TYPE, config.imageFormat.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${config.directory.name}/${config.customFolderName ?: "CameraK"}"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            var imageUri: Uri? = null
            try {
                imageUri = resolver.insert(collection, contentValues)
                if (imageUri == null) throw IOException("Failed to create new MediaStore record.")

                resolver.openOutputStream(imageUri).use { outputStream ->
                    if (outputStream == null) throw IOException("Failed to get output stream.")
                    outputStream.write(byteArray)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }

                println("Image saved successfully at URI: $imageUri")

            } catch (e: IOException) {
                e.printStackTrace()
                println("Failed to save image: ${e.message}")
                // Optionally, handle the error (e.g., notify the user)
            }
        }
    }
}

/**
 * Factory function to create an Android-specific [ImageSaverPlugin].
 *
 * @param config Configuration settings for the plugin.
 * @return An instance of [AndroidImageSaverPlugin].
 */

actual fun createPlatformImageSaverPlugin(
    context: PlatformContext,
    config: ImageSaverConfig
): ImageSaverPlugin {
    return AndroidImageSaverPlugin(context, config)
}