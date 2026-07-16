# Lens Capabilities + Real Ultra-Wide/Telephoto Selection

**Date:** 2026-07-16
**Status:** Approved (design discussed and accepted in session)
**Reference:** VisionSDK Android camera core (`vision-sdk-android/.../camera/core`) — ported, not depended on.

## Problem

1. **Android lens selection is broken on modern phones.** `CameraController.android.kt#createCameraSelector()` classifies lenses by raw focal length in mm (`> 4.0` → telephoto, `< 2.5` → ultra-wide). Raw mm is not comparable across sensor sizes, so classification is wrong on many devices. Worse, it only filters CameraX's top-level cameras: flagships (Pixel 7, most Samsungs) expose one logical back camera whose ultra-wide/tele exist only as physical sub-lenses, so `ULTRA_WIDE`/`TELEPHOTO` silently fall back to the main lens.
2. **No public capabilities API.** There is no way to ask "what lenses does this device have?" on any platform. Existing zoom/flash queries (`getMaxZoom()`, `hasTorch`) only describe the currently bound camera.

## Goals

- `setPreferredCameraDeviceType(ULTRA_WIDE / TELEPHOTO / ...)` actually binds those lenses on Android, including physical sub-lenses of logical multi-cameras.
- Public `getCameraCapabilities()` on `CameraController` (expect/actual) and `CameraKStateHolder`, returning per-lens info on Android and iOS.
- No breaking API changes. The enum-based switching API stays.

## Non-Goals

- No `Lens`-object/`LensSelection.Pin` API (VisionSDK style) — the existing enum API is kept.
- No capabilities in `CameraUIState`/StateFlow — static hardware info, a query function suffices.
- No zoom switch-points (Android exposes no API for it).
- No changes to the iOS switching path (already correct via `AVCaptureDeviceType`).

## Public API (commonMain)

```kotlin
// com.kashif.cameraK.capabilities
@Immutable
data class LensInfo(
    val id: String,                    // Camera2 id / AVCaptureDevice uniqueID; "" on desktop
    val deviceType: CameraDeviceType,  // ULTRA_WIDE / WIDE_ANGLE / TELEPHOTO / MACRO / DEFAULT
    val lens: CameraLens,              // FRONT / BACK
    val minZoom: Float,
    val maxZoom: Float,
    val hasFlash: Boolean,
    val isLogical: Boolean,            // Android logical multi-camera; false elsewhere
)

@Immutable
class CameraCapabilities(val allLenses: List<LensInfo>) {
    fun lenses(lens: CameraLens): List<LensInfo>
    fun availableDeviceTypes(lens: CameraLens): Set<CameraDeviceType>
}

// CameraController (expect):
fun getCameraCapabilities(): CameraCapabilities

// CameraKStateHolder (delegates, returns empty capabilities when controller is null):
fun getCameraCapabilities(): CameraCapabilities
```

Classification note: a lens whose focal data is unreadable maps to `CameraDeviceType.DEFAULT` (the existing enum has no `UNKNOWN`; adding one is unnecessary API surface).

## Architecture

### Shared classification math (commonMain, internal)

Pure functions ported from VisionSDK's `LensKindClassifier`, placed in commonMain so they run in `desktopTest` (fastest loop, repo convention):

- `equivalentFocalLengthMm(focalMm, sensorWidthMm?)` — 35mm-equivalent: `focal × 36 / sensorWidth`; falls back to raw mm when sensor size unreadable.
- `deriveLensDeviceTypes(List<RawLensFocalInfo>): Map<String, CameraDeviceType>` — rules:
  - unreadable focal data → `DEFAULT`, excluded from siblings' comparison
  - logical camera reporting multiple focal lengths → `WIDE_ANGLE` (it opens at 1.0x wide)
  - single classifiable camera in a facing → `WIDE_ANGLE` unconditionally
  - otherwise absolute thresholds on 35mm-equivalent: ≤ 20mm → `ULTRA_WIDE`, ≥ 70mm → `TELEPHOTO`, else `WIDE_ANGLE`
- `dedupeOpticallyDistinctLenses(...)` — groups physical sub-lenses by raw focal length (±0.05mm); keeps the largest-sensor entry per group (drops HAL crop pseudo-lenses, e.g. Pixel 7's fake 2x "telephoto").
- `RawLensFocalInfo(id, focalLengthsMm, physicalSizeWidthMm, isLogical)` — internal carrier, platform-independent.

### Android (androidMain)

**New: `LensEnumerator`** (internal) — snapshot via `CameraManager`:

- For each id in `cameraIdList`: read facing, `CONTROL_ZOOM_RATIO_RANGE`, `FLASH_INFO_AVAILABLE`, focal lengths, sensor physical size, `physicalCameraIds` (API 28+ only, guarded by `Build.VERSION.SDK_INT` — `NoSuchMethodError` is not an `Exception`; Kamera minSdk is 21).
- Logical cameras (non-empty `physicalCameraIds`) are kept AND their physical children are enumerated as separate entries via `getCameraCharacteristics(physicalId)`.
- Physical children are deduped with `dedupeOpticallyDistinctLenses` before classification.
- Every characteristics read is wrapped: a throwing camera id is skipped, never fatal.
- Output: `List<LensInfo>` plus an internal descriptor keeping `isPhysicalChild` + parent logical id (needed for binding).
- MACRO classification: `LENS_INFO_MINIMUM_FOCUS_DISTANCE > 0 && < 0.2f` overrides the focal-length type for top-level ids (preserves current behavior; focal length can't detect macro).

**Rewrite: `createCameraSelector()`** — resolve requested `CameraDeviceType` against the snapshot for the current facing:

- Requested type maps to a **top-level** id → `addCameraFilter` matching `Camera2CameraInfo.getCameraId()` (exact id, not focal-length re-filtering).
- Requested type maps to a **physical child** → `CameraSelector.Builder().setPhysicalCameraId(id)` filtered to the parent logical camera, plus `Camera2Interop.Extender.setPhysicalCameraId` on Preview/ImageCapture/ImageAnalysis builders (CameraX 1.5.1, already the project version; proven by VisionSDK spike PXA-2178).
- Type unavailable for facing → log warning, bind default camera (current fallback behavior, unchanged).
- `WIDE_ANGLE`/`DEFAULT` → no filter (current behavior).

`getCameraCapabilities()` = `LensEnumerator.snapshot(context)`, cached after first call (hardware doesn't change).

### iOS (appleMain)

- Extract one internal `discoverDevices(): List<AVCaptureDevice>` in `CustomCameraController` from the two existing copy-pasted `AVCaptureDeviceDiscoverySession` blocks (setup, line ~176; runtime switch, line ~531); the capabilities function is its third consumer.
- Discovery covers wide, telephoto, ultra-wide (+ macro type where the SDK exposes it), positions front+back.
- Map each device to `LensInfo`: `uniqueID`, device type → `CameraDeviceType`, position → `CameraLens`, zoom from `minAvailableVideoZoomFactor`/`maxAvailableVideoZoomFactor`, `hasFlash`, `isLogical = false`.
- Switching path untouched.

### Desktop (desktopMain)

`getCameraCapabilities()` returns one `LensInfo(id = "", deviceType = DEFAULT, lens = BACK, minZoom = 1f, maxZoom = 1f, hasFlash = false, isLogical = false)` when a webcam is present, else empty list.

## Error Handling

- Android: every `CameraCharacteristics` read is try/caught per camera id; a bad id is skipped. An empty snapshot yields empty `CameraCapabilities` and default lens binding — never a crash.
- iOS: discovery returning no devices yields empty capabilities (existing setup fallback path unchanged).
- StateHolder with null controller returns `CameraCapabilities(emptyList())`.

## Testing (commonTest, runs via desktopTest)

- `deriveLensDeviceTypes`: threshold cases; Pixel 7 regression (25.0mm-equiv wide + 16.8mm-equiv ultra-wide → no fabricated TELEPHOTO); logical multi-focal → WIDE_ANGLE; single lens → WIDE_ANGLE; unreadable → DEFAULT and excluded from ordering.
- `dedupeOpticallyDistinctLenses`: Pixel 7 crop case (id4 same 6.81mm focal as id2, smaller sensor → dropped); unreadable-focal lens survives as singleton.
- `equivalentFocalLengthMm`: normalization + missing-sensor-size fallback.
- `CameraCapabilities`: `lenses()` facing filter, `availableDeviceTypes()` set.
- Platform enumeration/binding: not unit-testable; verified by building the Sample and on-device smoke test (ultra-wide visibly wider FOV).

## File Plan

| File | Change |
|---|---|
| `cameraK/src/commonMain/.../capabilities/LensInfo.kt` | new: `LensInfo`, `CameraCapabilities` |
| `cameraK/src/commonMain/.../capabilities/LensClassification.kt` | new: internal classification math |
| `cameraK/src/commonMain/.../controller/CameraController.kt` | add `getCameraCapabilities()` to expect |
| `cameraK/src/commonMain/.../state/CameraKStateHolder.kt` | add delegating `getCameraCapabilities()` |
| `cameraK/src/androidMain/.../capabilities/LensEnumerator.kt` | new: Camera2 snapshot |
| `cameraK/src/androidMain/.../controller/CameraController.android.kt` | rewrite `createCameraSelector()`, add actual |
| `cameraK/src/appleMain/.../controller/CustomCameraController.kt` | extract `discoverDevices()`, add capabilities mapping |
| `cameraK/src/appleMain/.../controller/CameraController.apple.kt` | add actual |
| `cameraK/src/desktopMain/.../controller/CameraController.desktop.kt` | add actual |
| `cameraK/src/commonTest/.../capabilities/LensClassificationTest.kt` | new tests |
| `cameraK/src/commonTest/.../capabilities/CameraCapabilitiesTest.kt` | new tests |
| `README.MD` | document `getCameraCapabilities()` |
