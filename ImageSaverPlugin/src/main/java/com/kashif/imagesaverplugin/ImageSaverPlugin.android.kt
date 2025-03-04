package com.kashif.imagesaverplugin


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
import java.util.Date
import java.util.Locale

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

    override suspend fun saveImage(byteArray: ByteArray, imageName: String?): String? {
        return withContext(Dispatchers.IO) {
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


            try {
                val imageUri = resolver.insert(collection, contentValues)
                    ?: throw IOException("Failed to create new MediaStore record.")

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
                imageUri.toString()
            } catch (e: IOException) {
                e.printStackTrace()
                println("Failed to save image: ${e.message}")
                null
            }
        }
    }

    override fun getByteArrayFrom(path: String): ByteArray {
        try {
            val uri = Uri.parse(path)
            return context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: throw IOException("Failed to open input stream")
        } catch (e: Exception) {
            throw IOException("Failed to read image from URI: $path", e)
        }
    }

    /**
     * only for android may use this later on
     */
     fun getActualPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor.moveToFirst()) {
                    cursor.getString(columnIndex)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
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