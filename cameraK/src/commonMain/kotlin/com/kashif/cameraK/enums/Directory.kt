package com.kashif.cameraK.enums

/**
 * Enum representing the directory where captured images will be saved.
 *
 * - PICTURES: Android saves to public Pictures directory, iOS saves to Photos library
 * - DCIM: Android saves to public DCIM directory, iOS saves to Photos library
 * - DOCUMENTS: Saves to app-specific storage, private to the app. Captures are not added to Photos or
 *   announced to Android's media scanner, and on iOS the `Documents/CameraK` folder is excluded from
 *   iCloud backup. On Android the files live under `getExternalFilesDir()`, which Auto Backup includes
 *   by default; exclude the `CameraK` folder in your app's backup rules if captures must not be backed up.
 */
enum class Directory {
    PICTURES,
    DCIM,
    DOCUMENTS,
}
