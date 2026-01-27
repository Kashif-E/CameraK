# Changelog

All notable changes to the CameraK project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- **iOS Image Orientation Issue (#44)**: Fixed images being saved in wrong orientation on iOS
  - Added device orientation detection at photo capture time by setting `videoOrientation` on the photo output connection
  - Photos captured in portrait mode now save as portrait, landscape photos save as landscape
  - Optimized image processing to use original `fileDataRepresentation()` when quality is high (>85%), preserving original EXIF orientation metadata
  - Added `UIImage.fixOrientation()` utility function that applies orientation metadata to pixel data when re-encoding is necessary (format conversion or quality reduction)
  - Updated `ImageSaverPlugin` to apply orientation correction when saving images to Photos library
  - Ensured orientation is correctly preserved across portrait, landscape, and upside-down device orientations

### Technical Details
The iOS camera implementation previously had an issue where:
1. Photos were captured with correct EXIF orientation metadata
2. But when converted from `NSData` → `UIImage` → `JPEG/PNG` data, the orientation was lost or incorrectly applied

The fix implements a multi-layered approach:
1. **Capture Level**: Set `videoOrientation` on `AVCapturePhotoOutput` connection before capture to ensure correct EXIF metadata
2. **Processing Level**: Use original capture data directly when possible (JPEG format with high quality)
3. **Re-encoding Level**: When format conversion or quality reduction is needed, apply `fixOrientation()` to bake orientation into pixel data
4. **Save Level**: Apply orientation correction in `ImageSaverPlugin` before saving to Photos library

This ensures images display correctly regardless of how they were captured (portrait, landscape, etc.) and regardless of the output format (JPEG, PNG).

