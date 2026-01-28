# Changelog

All notable changes to the CameraK project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- **Plugin Initialization Timing (#52)**: Fixed plugins attempting to initialize before camera is ready
  - Added null checks in `QRScannerPlugin.android.kt` to prevent crashes when `cameraProvider` is null
  - Wrapped `OcrPlugin.onAttach()` in try-catch to gracefully handle initialization failures
  - Made OCR plugin fail silently (with warning logs) if tessdata not found instead of crashing the app
  - Added validation to ensure camera is fully bound before updating image analyzer

- **Desktop Tesseract Path Resolution (#51)**: Fixed hardcoded tessdata path breaking on other machines
  - Implemented dynamic path resolution checking multiple locations for tessdata files
  - Added graceful degradation when tessdata is not found on desktop
  - Desktop app now runs without OCR errors even when tessdata is unavailable

- **Desktop QR Scanner Syntax Error (#50)**: Fixed duplicate `put()` statement in decode hints map
  - Corrected malformed EnumMap initialization in QRScannerPlugin

### Added
- **API Toggle Feature**: Added UI toggle button to switch between new and old camera APIs
  - New Compose-first `CameraKScreen` with reactive state management
  - Legacy callback-based `CameraPreview` API available via toggle
  - Both APIs work seamlessly with all plugins (ImageSaver, QRScanner, OCR)
  - Toggle button in camera controls bottom sheet for easy switching

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

