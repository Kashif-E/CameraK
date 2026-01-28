package com.kashif.cameraK.enums

/**
 * Enum representing the directory where captured images will be saved.
 *
 * - PICTURES: Android saves to public Pictures directory, iOS saves to Photos library
 * - DCIM: Android saves to public DCIM directory, iOS saves to Photos library
 * - DOCUMENTS: Saves to app's Documents directory (private storage, not synced with Photos)
 */
enum class Directory {
    PICTURES,
    DCIM,
    DOCUMENTS,
}
