# Lens Capabilities + Real Ultra-Wide/Telephoto Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Public `getCameraCapabilities()` on all platforms, and Android `setPreferredCameraDeviceType(ULTRA_WIDE/TELEPHOTO)` that actually binds those lenses — including physical sub-lenses of logical multi-cameras.

**Architecture:** Pure classification math (35mm-equivalent focal length) lives in commonMain (unit-testable via `desktopTest`). Android enumerates cameras via `CameraManager` including `physicalCameraIds` expansion, and binds physical sub-lenses via `CameraSelector.setPhysicalCameraId` + `Camera2Interop.Extender.setPhysicalCameraId` on every use-case builder (both are required together — verified on real hardware). iOS wraps its existing `AVCaptureDeviceDiscoverySession` pattern into `LensInfo`s. Spec: `docs/superpowers/specs/2026-07-16-lens-capabilities-design.md`.

**Tech Stack:** Kotlin Multiplatform, CameraX 1.5.1 (already the project version — do not bump), AVFoundation, kotlin.test.

## Global Constraints

- Package: new public API in `com.kashif.cameraK.capabilities`.
- Android minSdk 21: `physicalCameraIds` (API 28) and `CONTROL_ZOOM_RATIO_RANGE` (API 30) MUST be behind `Build.VERSION.SDK_INT` checks — `NoSuchMethodError`/`NoSuchFieldError` are Errors, not Exceptions; try/catch does not save you.
- No breaking API changes: `setPreferredCameraDeviceType` signature unchanged.
- Formatting: run `./gradlew spotlessApply` before every commit.
- Test loop: `./gradlew cameraK:desktopTest` (fast); full gate is `./gradlew check`.
- Logger: `com.kashif.cameraK.utils.CameraKLogger` (object; `CameraKLogger.w("CameraK", "msg")`).
- Every step's commit messages end with `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.

---

### Task 1: Common capabilities model

**Files:**
- Create: `cameraK/src/commonMain/kotlin/com/kashif/cameraK/capabilities/LensInfo.kt`
- Test: `cameraK/src/commonTest/kotlin/com/kashif/cameraK/capabilities/CameraCapabilitiesTest.kt`

**Interfaces:**
- Consumes: existing `CameraDeviceType`, `CameraLens` enums.
- Produces: `LensInfo(id: String, deviceType: CameraDeviceType, lens: CameraLens, minZoom: Float, maxZoom: Float, hasFlash: Boolean, isLogical: Boolean)`; `CameraCapabilities(allLenses: List<LensInfo>)` with `lenses(lens: CameraLens): List<LensInfo>`, `availableDeviceTypes(lens: CameraLens): Set<CameraDeviceType>`, and `CameraCapabilities.EMPTY`. All later tasks use these exact names.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.kashif.cameraK.capabilities

import com.kashif.cameraK.enums.CameraDeviceType
import com.kashif.cameraK.enums.CameraLens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraCapabilitiesTest {

    private fun lens(
        id: String,
        type: CameraDeviceType,
        facing: CameraLens,
    ) = LensInfo(
        id = id,
        deviceType = type,
        lens = facing,
        minZoom = 1f,
        maxZoom = 4f,
        hasFlash = false,
        isLogical = false,
    )

    private val caps = CameraCapabilities(
        listOf(
            lens("0", CameraDeviceType.WIDE_ANGLE, CameraLens.BACK),
            lens("2", CameraDeviceType.ULTRA_WIDE, CameraLens.BACK),
            lens("1", CameraDeviceType.WIDE_ANGLE, CameraLens.FRONT),
        ),
    )

    @Test
    fun lenses_filtersByFacing() {
        assertEquals(listOf("0", "2"), caps.lenses(CameraLens.BACK).map { it.id })
        assertEquals(listOf("1"), caps.lenses(CameraLens.FRONT).map { it.id })
    }

    @Test
    fun availableDeviceTypes_isSetPerFacing() {
        assertEquals(
            setOf(CameraDeviceType.WIDE_ANGLE, CameraDeviceType.ULTRA_WIDE),
            caps.availableDeviceTypes(CameraLens.BACK),
        )
        assertEquals(setOf(CameraDeviceType.WIDE_ANGLE), caps.availableDeviceTypes(CameraLens.FRONT))
    }

    @Test
    fun empty_hasNoLenses() {
        assertTrue(CameraCapabilities.EMPTY.allLenses.isEmpty())
        assertTrue(CameraCapabilities.EMPTY.lenses(CameraLens.BACK).isEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew cameraK:desktopTest --tests "com.kashif.cameraK.capabilities.CameraCapabilitiesTest"`
Expected: compilation FAILURE — `LensInfo`/`CameraCapabilities` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.kashif.cameraK.capabilities

import androidx.compose.runtime.Immutable
import com.kashif.cameraK.enums.CameraDeviceType
import com.kashif.cameraK.enums.CameraLens

/**
 * Describes one physical (or logical) camera lens on the device.
 *
 * @property id Platform camera id (Camera2 id on Android, AVCaptureDevice.uniqueID on iOS, "" on Desktop).
 * @property deviceType Classified lens type. Unclassifiable lenses report [CameraDeviceType.DEFAULT].
 * @property lens Which side of the device the lens faces.
 * @property minZoom Minimum zoom ratio (can be < 1.0 on logical multi-cameras that reach ultra-wide via zoom-out).
 * @property maxZoom Maximum zoom ratio.
 * @property hasFlash Whether this lens has a flash unit.
 * @property isLogical Android logical multi-camera (merges several physical lenses behind one id); false elsewhere.
 */
@Immutable
data class LensInfo(
    val id: String,
    val deviceType: CameraDeviceType,
    val lens: CameraLens,
    val minZoom: Float,
    val maxZoom: Float,
    val hasFlash: Boolean,
    val isLogical: Boolean,
)

/**
 * Snapshot of the device's camera hardware. Obtain via `CameraController.getCameraCapabilities()`
 * or `CameraKStateHolder.getCameraCapabilities()`.
 */
@Immutable
class CameraCapabilities(val allLenses: List<LensInfo>) {

    /** Lenses facing [lens], in enumeration order. */
    fun lenses(lens: CameraLens): List<LensInfo> = allLenses.filter { it.lens == lens }

    /** Device types selectable via `setPreferredCameraDeviceType` for [lens]. */
    fun availableDeviceTypes(lens: CameraLens): Set<CameraDeviceType> =
        lenses(lens).mapTo(mutableSetOf()) { it.deviceType }

    companion object {
        val EMPTY = CameraCapabilities(emptyList())
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew cameraK:desktopTest --tests "com.kashif.cameraK.capabilities.CameraCapabilitiesTest"`
Expected: BUILD SUCCESSFUL, 3 tests pass.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add cameraK/src/commonMain/kotlin/com/kashif/cameraK/capabilities/LensInfo.kt cameraK/src/commonTest/kotlin/com/kashif/cameraK/capabilities/CameraCapabilitiesTest.kt
git commit -m "feat: add LensInfo + CameraCapabilities model

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: Lens classification math (commonMain, internal)

**Files:**
- Create: `cameraK/src/commonMain/kotlin/com/kashif/cameraK/capabilities/LensClassification.kt`
- Test: `cameraK/src/commonTest/kotlin/com/kashif/cameraK/capabilities/LensClassificationTest.kt`

**Interfaces:**
- Consumes: `CameraDeviceType` (Task 1's file only for package co-location).
- Produces (internal, used by Task 4's Android enumerator):
  - `internal class RawLensFocalInfo(val id: String, val focalLengthsMm: List<Float>, val physicalSizeWidthMm: Float?, val isLogical: Boolean)`
  - `internal fun equivalentFocalLengthMm(focalLengthMm: Float, physicalSizeWidthMm: Float?): Float`
  - `internal fun deriveLensDeviceTypes(lenses: List<RawLensFocalInfo>): Map<String, CameraDeviceType>`
  - `internal fun dedupeOpticallyDistinctLenses(physicals: List<RawLensFocalInfo>): List<RawLensFocalInfo>`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.kashif.cameraK.capabilities

import com.kashif.cameraK.enums.CameraDeviceType
import kotlin.test.Test
import kotlin.test.assertEquals

class LensClassificationTest {

    private fun raw(
        id: String,
        focals: List<Float>,
        sensorWidth: Float? = 6.4f,
        isLogical: Boolean = false,
    ) = RawLensFocalInfo(id, focals, sensorWidth, isLogical)

    // ── equivalentFocalLengthMm ──────────────────────────────────────

    @Test
    fun equivalent_normalizesTo35mm() {
        // 4.38mm lens on a 6.4mm-wide sensor → 4.38 * 36 / 6.4 = 24.64mm equiv
        assertEquals(24.6375f, equivalentFocalLengthMm(4.38f, 6.4f), 0.001f)
    }

    @Test
    fun equivalent_fallsBackToRawWhenSensorUnknown() {
        assertEquals(4.38f, equivalentFocalLengthMm(4.38f, null), 0.001f)
        assertEquals(4.38f, equivalentFocalLengthMm(4.38f, 0f), 0.001f)
    }

    // ── deriveLensDeviceTypes ────────────────────────────────────────

    @Test
    fun pixel7Case_noFabricatedTelephoto() {
        // Pixel 7 back physicals: 6.81mm on 9.79mm sensor (≈25mm equiv, wide)
        // + 2.35mm on 5.04mm sensor (≈16.8mm equiv, ultra-wide). No telephoto glass.
        val kinds = deriveLensDeviceTypes(
            listOf(
                raw("2", listOf(6.81f), 9.79f),
                raw("3", listOf(2.35f), 5.04f),
            ),
        )
        assertEquals(CameraDeviceType.WIDE_ANGLE, kinds["2"])
        assertEquals(CameraDeviceType.ULTRA_WIDE, kinds["3"])
    }

    @Test
    fun telephotoThreshold() {
        // 13.0mm on 6.4mm sensor → 73.1mm equiv → TELEPHOTO
        val kinds = deriveLensDeviceTypes(
            listOf(
                raw("0", listOf(4.38f)),          // 24.6mm equiv → WIDE
                raw("1", listOf(13.0f)),          // 73.1mm equiv → TELE
            ),
        )
        assertEquals(CameraDeviceType.WIDE_ANGLE, kinds["0"])
        assertEquals(CameraDeviceType.TELEPHOTO, kinds["1"])
    }

    @Test
    fun logicalMultiFocal_isWideAngle() {
        // Logical camera reporting merged focal lengths must never be tagged ULTRA_WIDE
        // just because one merged focal length is short — it opens at 1.0x (wide).
        val kinds = deriveLensDeviceTypes(
            listOf(raw("0", listOf(2.35f, 6.81f), isLogical = true)),
        )
        assertEquals(CameraDeviceType.WIDE_ANGLE, kinds["0"])
    }

    @Test
    fun singleLens_isWideAngleUnconditionally() {
        // A 2.0mm focal on tiny front sensor would classify ULTRA_WIDE by threshold,
        // but a facing's only camera is in practice the primary lens.
        val kinds = deriveLensDeviceTypes(listOf(raw("1", listOf(2.0f), 3.6f)))
        assertEquals(CameraDeviceType.WIDE_ANGLE, kinds["1"])
    }

    @Test
    fun unreadableFocal_isDefaultAndExcludedFromOrdering() {
        val kinds = deriveLensDeviceTypes(
            listOf(
                raw("0", emptyList()),
                raw("1", listOf(4.38f)),
            ),
        )
        assertEquals(CameraDeviceType.DEFAULT, kinds["0"])
        // "1" is the only classifiable lens → single-lens rule applies → WIDE_ANGLE
        assertEquals(CameraDeviceType.WIDE_ANGLE, kinds["1"])
    }

    // ── dedupeOpticallyDistinctLenses ────────────────────────────────

    @Test
    fun pixel7CropPseudoLens_isDropped() {
        // id4 shares id2's exact 6.81mm optics on a smaller (4.90mm) readout — a digital
        // crop, not real telephoto glass. Keep the largest-sensor entry per focal group.
        val survivors = dedupeOpticallyDistinctLenses(
            listOf(
                raw("2", listOf(6.81f), 9.79f),
                raw("3", listOf(2.35f), 5.04f),
                raw("4", listOf(6.81f), 4.90f),
            ),
        )
        assertEquals(listOf("2", "3"), survivors.map { it.id })
    }

    @Test
    fun unreadableFocal_survivesAsSingleton() {
        val survivors = dedupeOpticallyDistinctLenses(
            listOf(
                raw("2", listOf(6.81f), 9.79f),
                raw("9", emptyList(), null),
            ),
        )
        assertEquals(listOf("2", "9"), survivors.map { it.id })
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew cameraK:desktopTest --tests "com.kashif.cameraK.capabilities.LensClassificationTest"`
Expected: compilation FAILURE — `RawLensFocalInfo` etc. unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.kashif.cameraK.capabilities

import com.kashif.cameraK.enums.CameraDeviceType
import kotlin.math.roundToInt

/**
 * Raw per-camera focal data used to classify a lens — platform-independent so the
 * classification math is unit-testable in desktopTest without Camera2/AVFoundation.
 * [focalLengthsMm] is LENS_INFO_AVAILABLE_FOCAL_LENGTHS verbatim (a logical multi-camera
 * reports the union of its physical cameras' focal lengths). [physicalSizeWidthMm] is
 * SENSOR_INFO_PHYSICAL_SIZE width, used to normalize to 35mm-equivalent — comparing raw mm
 * across different sensor sizes misclassifies (the bug this replaces). Null when unreadable.
 */
internal class RawLensFocalInfo(
    val id: String,
    val focalLengthsMm: List<Float>,
    val physicalSizeWidthMm: Float?,
    val isLogical: Boolean,
)

// Absolute-ish 35mm-equivalent boundaries. Phone ultra-wides sit ~13-18mm equiv and phone
// telephotos start ~48mm equiv and up, so these leave a safety margin on both sides.
private const val ULTRA_WIDE_MAX_EQUIV_MM = 20f
private const val TELEPHOTO_MIN_EQUIV_MM = 70f

// Two physical sub-lenses whose raw focal lengths match within this tolerance share the
// same optics (one is a crop readout of the other).
private const val FOCAL_LENGTH_GROUPING_TOLERANCE_MM = 0.05f

/**
 * Standard 35mm ("full-frame") equivalent focal length: actual focal length scaled by
 * 36mm (full-frame sensor width) over this sensor's own width. Falls back to the raw
 * focal length when the sensor size is unreadable.
 */
internal fun equivalentFocalLengthMm(
    focalLengthMm: Float,
    physicalSizeWidthMm: Float?,
): Float =
    if (physicalSizeWidthMm != null && physicalSizeWidthMm > 0f) {
        focalLengthMm * (36f / physicalSizeWidthMm)
    } else {
        focalLengthMm
    }

/**
 * Classifies each camera of ONE facing by 35mm-equivalent focal length against absolute
 * thresholds — never by ordinal rank within the set (rank fabricates a TELEPHOTO on
 * 2-lens {ultra-wide, wide} devices like the Pixel 7). Rules:
 * - unreadable focal data → [CameraDeviceType.DEFAULT], excluded from siblings' comparison
 * - logical camera reporting multiple focal lengths → [CameraDeviceType.WIDE_ANGLE]
 *   (it opens at 1.0x, which resolves to the wide lens on current hardware)
 * - a facing's single classifiable camera → [CameraDeviceType.WIDE_ANGLE] unconditionally
 * - otherwise: equiv ≤ 20mm → ULTRA_WIDE, equiv ≥ 70mm → TELEPHOTO, else WIDE_ANGLE
 */
internal fun deriveLensDeviceTypes(lenses: List<RawLensFocalInfo>): Map<String, CameraDeviceType> {
    val types = mutableMapOf<String, CameraDeviceType>()
    val toClassify = mutableListOf<Pair<String, Float>>()

    for (lens in lenses) {
        when {
            lens.focalLengthsMm.isEmpty() -> types[lens.id] = CameraDeviceType.DEFAULT
            lens.isLogical && lens.focalLengthsMm.size > 1 -> types[lens.id] = CameraDeviceType.WIDE_ANGLE
            else -> toClassify.add(
                lens.id to equivalentFocalLengthMm(lens.focalLengthsMm.first(), lens.physicalSizeWidthMm),
            )
        }
    }

    when (toClassify.size) {
        0 -> Unit
        1 -> types[toClassify.single().first] = CameraDeviceType.WIDE_ANGLE
        else -> for ((id, equivMm) in toClassify) {
            types[id] = when {
                equivMm <= ULTRA_WIDE_MAX_EQUIV_MM -> CameraDeviceType.ULTRA_WIDE
                equivMm >= TELEPHOTO_MIN_EQUIV_MM -> CameraDeviceType.TELEPHOTO
                else -> CameraDeviceType.WIDE_ANGLE
            }
        }
    }
    return types
}

/**
 * Dedupes a logical camera's physical sub-lenses down to genuinely-distinct optics.
 * The HAL over-enumerates: some physical ids are crop pseudo-lenses sharing another
 * id's exact focal length on a smaller readout (Pixel 7's "telephoto" is a 2x crop of
 * the wide). Groups by raw focal length (±[FOCAL_LENGTH_GROUPING_TOLERANCE_MM]) and keeps
 * the largest-sensor entry per group. Unreadable-focal lenses survive as singletons.
 */
internal fun dedupeOpticallyDistinctLenses(physicals: List<RawLensFocalInfo>): List<RawLensFocalInfo> {
    val groups = LinkedHashMap<Any, MutableList<RawLensFocalInfo>>()
    for (lens in physicals) {
        val focal = lens.focalLengthsMm.firstOrNull()
        val key: Any = if (focal != null) roundToGroupingStep(focal) else lens.id
        groups.getOrPut(key) { mutableListOf() }.add(lens)
    }
    return groups.values.map { group -> group.maxByOrNull { it.physicalSizeWidthMm ?: -1f } ?: group.first() }
}

private fun roundToGroupingStep(focalLengthMm: Float): Float =
    (focalLengthMm / FOCAL_LENGTH_GROUPING_TOLERANCE_MM).roundToInt() * FOCAL_LENGTH_GROUPING_TOLERANCE_MM
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew cameraK:desktopTest --tests "com.kashif.cameraK.capabilities.LensClassificationTest"`
Expected: BUILD SUCCESSFUL, 8 tests pass.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add cameraK/src/commonMain/kotlin/com/kashif/cameraK/capabilities/LensClassification.kt cameraK/src/commonTest/kotlin/com/kashif/cameraK/capabilities/LensClassificationTest.kt
git commit -m "feat: 35mm-equivalent lens classification + crop-lens dedup

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: expect declaration, StateHolder delegation, desktop + placeholder actuals

**Files:**
- Modify: `cameraK/src/commonMain/kotlin/com/kashif/cameraK/controller/CameraController.kt` (after `setPreferredCameraDeviceType`, ~line 125)
- Modify: `cameraK/src/commonMain/kotlin/com/kashif/cameraK/state/CameraKStateHolder.kt` (near the other controller delegations, ~line 378)
- Modify: `cameraK/src/desktopMain/kotlin/com/kashif/cameraK/controller/CameraController.desktop.kt` (after `setPreferredCameraDeviceType`, ~line 180)
- Modify: `cameraK/src/androidMain/kotlin/com/kashif/cameraK/controller/CameraController.android.kt` (near `getPreferredCameraDeviceType`, ~line 596)
- Modify: `cameraK/src/appleMain/kotlin/com/kashif/cameraK/controller/CameraController.apple.kt` (near `getMaxZoom`, ~line 433)

**Interfaces:**
- Consumes: `CameraCapabilities`, `LensInfo`, `CameraCapabilities.EMPTY` (Task 1); `CameraDeviceType`, `CameraLens`.
- Produces: `CameraController.getCameraCapabilities(): CameraCapabilities` (expect + all actuals compile on every target); `CameraKStateHolder.getCameraCapabilities(): CameraCapabilities`. Android/iOS actuals are placeholders returning `EMPTY`, replaced in Tasks 4/6 — the desktop actual is final here.

- [ ] **Step 1: Add the expect declaration**

In `CameraController.kt`, after `setPreferredCameraDeviceType` (import `com.kashif.cameraK.capabilities.CameraCapabilities`):

```kotlin
    /**
     * Returns a snapshot of the device's camera hardware: every lens with its
     * classified type (ultra-wide / wide / telephoto / macro), facing, zoom range
     * and flash availability.
     *
     * Use [CameraCapabilities.availableDeviceTypes] to know which values of
     * [setPreferredCameraDeviceType] will actually switch lenses on this device.
     *
     * Platform notes:
     * - Android: enumerated via Camera2, including physical sub-lenses of logical
     *   multi-cameras. Cached after the first call.
     * - iOS: enumerated via AVCaptureDeviceDiscoverySession.
     * - Desktop: reports a single DEFAULT lens.
     */
    fun getCameraCapabilities(): CameraCapabilities
```

- [ ] **Step 2: Add all platform actuals**

Desktop (`CameraController.desktop.kt`, final implementation — import `com.kashif.cameraK.capabilities.CameraCapabilities` and `com.kashif.cameraK.capabilities.LensInfo`):

```kotlin
    actual fun getCameraCapabilities(): CameraCapabilities = CameraCapabilities(
        listOf(
            LensInfo(
                id = "",
                deviceType = CameraDeviceType.DEFAULT,
                lens = CameraLens.BACK,
                minZoom = 1f,
                maxZoom = 1f,
                hasFlash = false,
                isLogical = false,
            ),
        ),
    )
```

Android (`CameraController.android.kt`, placeholder — replaced in Task 4):

```kotlin
    actual fun getCameraCapabilities(): CameraCapabilities = CameraCapabilities.EMPTY
```

iOS (`CameraController.apple.kt`, placeholder — replaced in Task 6):

```kotlin
    actual fun getCameraCapabilities(): CameraCapabilities = CameraCapabilities.EMPTY
```

- [ ] **Step 3: Add StateHolder delegation**

In `CameraKStateHolder.kt`, next to the other controller-delegating functions (import `com.kashif.cameraK.capabilities.CameraCapabilities`):

```kotlin
    /**
     * Returns the device's camera hardware capabilities, or [CameraCapabilities.EMPTY]
     * while the camera is not yet initialized.
     */
    fun getCameraCapabilities(): CameraCapabilities =
        controller?.getCameraCapabilities() ?: CameraCapabilities.EMPTY
```

- [ ] **Step 4: Verify all targets compile**

Run: `./gradlew cameraK:compileKotlinDesktop cameraK:compileDebugKotlinAndroid cameraK:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL for all three.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add -u
git commit -m "feat: getCameraCapabilities() expect/actual + StateHolder delegation

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: Android lens enumerator + real capabilities actual

**Files:**
- Create: `cameraK/src/androidMain/kotlin/com/kashif/cameraK/capabilities/LensEnumerator.kt`
- Modify: `cameraK/src/androidMain/kotlin/com/kashif/cameraK/controller/CameraController.android.kt` (replace Task 3's placeholder actual)

**Interfaces:**
- Consumes: `RawLensFocalInfo`, `deriveLensDeviceTypes`, `dedupeOpticallyDistinctLenses` (Task 2); `LensInfo`, `CameraCapabilities` (Task 1).
- Produces: `internal class AndroidLensDescriptor(val info: LensInfo, val isPhysicalChild: Boolean, val parentLogicalId: String?)`; `internal object LensEnumerator { fun snapshot(context: Context): List<AndroidLensDescriptor> }`; `CameraController.lensSnapshot(): List<AndroidLensDescriptor>` (internal, cached) — Task 5 selects lenses from it.

- [ ] **Step 1: Write the enumerator**

```kotlin
package com.kashif.cameraK.capabilities

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.kashif.cameraK.enums.CameraDeviceType
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.utils.CameraKLogger

/** [LensInfo] plus the Android-only binding facts Task 5's selector needs. */
internal class AndroidLensDescriptor(
    val info: LensInfo,
    val isPhysicalChild: Boolean,
    val parentLogicalId: String?,
)

/**
 * Camera2 snapshot of every lens on the device. Top-level ids come from
 * [CameraManager.cameraIdList]; a logical multi-camera's physical sub-lenses (which are
 * NOT in that list) are expanded via physicalCameraIds and read directly with
 * getCameraCharacteristics(physicalId) — Camera2 supports that since API 28. Crop
 * pseudo-lenses the HAL over-enumerates are dropped via [dedupeOpticallyDistinctLenses].
 */
internal object LensEnumerator {

    fun snapshot(context: Context): List<AndroidLensDescriptor> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val cameraIds = try {
            manager?.cameraIdList.orEmpty()
        } catch (e: Exception) {
            CameraKLogger.w("CameraK", "Lens enumeration failed to list cameras: ${e.message}")
            emptyArray()
        }

        val rawByFacing = mutableMapOf<CameraLens, MutableList<RawCamera>>()
        for (id in cameraIds) {
            val raw = describe(manager, id, isPhysicalChild = false, parentLogicalId = null) ?: continue
            rawByFacing.getOrPut(raw.facing) { mutableListOf() }.add(raw)
            if (raw.isLogical) {
                for (physicalId in raw.physicalCameraIds) {
                    val child = describe(manager, physicalId, isPhysicalChild = true, parentLogicalId = id) ?: continue
                    rawByFacing.getOrPut(child.facing) { mutableListOf() }.add(child)
                }
            }
        }

        val result = mutableListOf<AndroidLensDescriptor>()
        for ((facing, allRaw) in rawByFacing) {
            val rawCameras = dedupeCropSiblings(allRaw)
            val types = deriveLensDeviceTypes(
                rawCameras.map { RawLensFocalInfo(it.id, it.focalLengthsMm, it.physicalSizeWidthMm, it.isLogical) },
            )
            for (raw in rawCameras) {
                // Focal length can't detect macro; keep the pre-existing min-focus-distance rule
                // for top-level ids (matches the old createCameraSelector MACRO filter).
                val deviceType = if (!raw.isPhysicalChild && raw.minFocusDistance in MACRO_FOCUS_RANGE) {
                    CameraDeviceType.MACRO
                } else {
                    types[raw.id] ?: CameraDeviceType.DEFAULT
                }
                result.add(
                    AndroidLensDescriptor(
                        info = LensInfo(
                            id = raw.id,
                            deviceType = deviceType,
                            lens = facing,
                            minZoom = raw.minZoomRatio,
                            maxZoom = raw.maxZoomRatio,
                            hasFlash = raw.hasFlash,
                            isLogical = raw.isLogical,
                        ),
                        isPhysicalChild = raw.isPhysicalChild,
                        parentLogicalId = raw.parentLogicalId,
                    ),
                )
            }
        }
        return result
    }

    private val MACRO_FOCUS_RANGE = 0.001f..0.2f

    private fun dedupeCropSiblings(rawCameras: List<RawCamera>): List<RawCamera> {
        val (children, topLevel) = rawCameras.partition { it.isPhysicalChild }
        val surviving = dedupeOpticallyDistinctLenses(
            children.map { RawLensFocalInfo(it.id, it.focalLengthsMm, it.physicalSizeWidthMm, it.isLogical) },
        ).mapTo(mutableSetOf()) { it.id }
        return topLevel + children.filter { it.id in surviving }
    }

    private class RawCamera(
        val id: String,
        val facing: CameraLens,
        val minZoomRatio: Float,
        val maxZoomRatio: Float,
        val hasFlash: Boolean,
        val isLogical: Boolean,
        val isPhysicalChild: Boolean,
        val parentLogicalId: String?,
        val focalLengthsMm: List<Float>,
        val physicalSizeWidthMm: Float?,
        val physicalCameraIds: Set<String>,
        val minFocusDistance: Float,
    )

    private fun describe(
        manager: CameraManager?,
        id: String,
        isPhysicalChild: Boolean,
        parentLogicalId: String?,
    ): RawCamera? {
        val characteristics = try {
            manager?.getCameraCharacteristics(id)
        } catch (e: Exception) {
            CameraKLogger.w("CameraK", "Lens enumeration failed on camera $id: ${e.message}")
            null
        } ?: return null

        val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_FRONT -> CameraLens.FRONT
            else -> CameraLens.BACK
        }

        // CONTROL_ZOOM_RATIO_RANGE is an API 30+ Key FIELD — referencing it below 30 throws
        // NoSuchFieldError (an Error, uncatchable by catch(Exception)). Guard by SDK level.
        val (minZoom, maxZoom) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val range = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            (range?.lower ?: 1.0f) to (range?.upper ?: 1.0f)
        } else {
            val maxDigital = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
            1.0f to maxDigital
        }

        // physicalCameraIds is API 28+ — same NoSuchMethodError trap, same guard.
        val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                characteristics.physicalCameraIds
            } catch (e: Exception) {
                emptySet()
            }
        } else {
            emptySet()
        }

        return RawCamera(
            id = id,
            facing = facing,
            minZoomRatio = minZoom,
            maxZoomRatio = maxZoom,
            hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true,
            isLogical = physicalIds.isNotEmpty(),
            isPhysicalChild = isPhysicalChild,
            parentLogicalId = parentLogicalId,
            focalLengthsMm = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList().orEmpty(),
            physicalSizeWidthMm = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.width,
            physicalCameraIds = physicalIds,
            minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f,
        )
    }
}
```

- [ ] **Step 2: Wire the cached snapshot into the controller**

In `CameraController.android.kt` — add imports `com.kashif.cameraK.capabilities.AndroidLensDescriptor`, `com.kashif.cameraK.capabilities.CameraCapabilities`, `com.kashif.cameraK.capabilities.LensEnumerator`; add a field next to the other private fields (~line 111):

```kotlin
    // Hardware doesn't change at runtime; enumerate once.
    private var cachedLensSnapshot: List<AndroidLensDescriptor>? = null

    internal fun lensSnapshot(): List<AndroidLensDescriptor> =
        cachedLensSnapshot ?: LensEnumerator.snapshot(context).also { cachedLensSnapshot = it }
```

Replace the Task 3 placeholder actual:

```kotlin
    actual fun getCameraCapabilities(): CameraCapabilities = CameraCapabilities(lensSnapshot().map { it.info })
```

- [ ] **Step 3: Verify compile + existing tests still green**

Run: `./gradlew cameraK:compileDebugKotlinAndroid cameraK:desktopTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Format and commit**

```bash
./gradlew spotlessApply
git add -u cameraK/src/androidMain
git commit -m "feat(android): Camera2 lens enumeration with physical sub-lens expansion

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: Android selector rewrite — id-based selection + physical pinning

**Files:**
- Modify: `cameraK/src/androidMain/kotlin/com/kashif/cameraK/controller/CameraController.android.kt`:
  - `createCameraSelector()` (~lines 291-357, full replacement)
  - `bindCamera()` (~lines 142-151: selector must be created BEFORE the Preview builder)
  - `configureCaptureUseCase()` (~line 364)
  - `rebuildMultiplexedAnalyzer()` (~line 428)
  - `setPreferredCameraDeviceType` / `toggleCameraLens`: no changes (they already rebind)

**Interfaces:**
- Consumes: `lensSnapshot(): List<AndroidLensDescriptor>` (Task 4).
- Produces: private `pinnedPhysicalId: String?` field consulted by all use-case builders. No public API change.

- [ ] **Step 1: Replace createCameraSelector**

Add the field next to `cachedLensSnapshot`:

```kotlin
    // Set by createCameraSelector when the requested device type is a physical sub-lens of a
    // logical multi-camera. CameraSelector.setPhysicalCameraId ALONE silently no-ops on real
    // hardware — it must be paired with Camera2Interop.Extender.setPhysicalCameraId on EVERY
    // use-case builder (verified on real hardware). Consulted by bindCamera,
    // configureCaptureUseCase and rebuildMultiplexedAnalyzer.
    private var pinnedPhysicalId: String? = null
```

Replace the whole `createCameraSelector()` (delete the old focal-length filters):

```kotlin
    /**
     * Creates a camera selector for the current facing + requested device type, resolved
     * against the real lens snapshot (35mm-equivalent classification) instead of raw
     * focal-length thresholds.
     *
     * - WIDE_ANGLE/DEFAULT: no filter (the default logical camera opens wide).
     * - Requested type maps to a top-level camera id: exact-id filter.
     * - Requested type maps to a physical sub-lens: physical-camera pin (selector half;
     *   the use-case builders apply the Camera2Interop half via [pinnedPhysicalId]).
     * - Type unavailable on this facing: warn and fall back to the default camera.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun createCameraSelector(): CameraSelector {
        pinnedPhysicalId = null
        val builder = CameraSelector.Builder()
            .requireLensFacing(cameraLens.toCameraXLensFacing())

        if (cameraDeviceType == CameraDeviceType.DEFAULT || cameraDeviceType == CameraDeviceType.WIDE_ANGLE) {
            return builder.build()
        }

        val target = lensSnapshot().firstOrNull {
            it.info.lens == cameraLens && it.info.deviceType == cameraDeviceType
        }
        if (target == null) {
            CameraKLogger.w("CameraK", "$cameraDeviceType not available for $cameraLens, using default camera")
            return builder.build()
        }

        return if (target.isPhysicalChild) {
            pinnedPhysicalId = target.info.id
            builder.setPhysicalCameraId(target.info.id).build()
        } else {
            builder.addCameraFilter { cameraInfos ->
                cameraInfos.filter { Camera2CameraInfo.from(it).cameraId == target.info.id }
                    .ifEmpty {
                        CameraKLogger.w("CameraK", "Camera ${target.info.id} not offered by CameraX, using default")
                        cameraInfos
                    }
            }.build()
        }
    }
```

- [ ] **Step 2: Reorder bindCamera and pin the Preview builder**

In `bindCamera()`, the selector is currently created AFTER the Preview builder — `pinnedPhysicalId` must be set first. Change lines ~142-151 from:

```kotlin
                val resolutionSelector = createResolutionSelector()

                preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val cameraSelector = createCameraSelector()
```

to:

```kotlin
                val resolutionSelector = createResolutionSelector()
                // Must run before any use-case builder: it decides pinnedPhysicalId.
                val cameraSelector = createCameraSelector()

                preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .apply { pinnedPhysicalId?.let { Camera2Interop.Extender(this).setPhysicalCameraId(it) } }
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
```

Add import `androidx.camera.camera2.interop.Camera2Interop` and, if not already present on `bindCamera`'s containing declarations, keep the `@OptIn(ExperimentalCamera2Interop::class)` annotation on the modified functions (bindCamera, configureCaptureUseCase, rebuildMultiplexedAnalyzer).

- [ ] **Step 3: Pin ImageCapture and ImageAnalysis builders**

`configureCaptureUseCase()` (~line 364) — add the same apply to the builder chain:

```kotlin
        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode.toCameraXFlashMode())
            .setCaptureMode(
                /* existing when-block unchanged */
            )
            .setResolutionSelector(resolutionSelector)
            .apply { pinnedPhysicalId?.let { Camera2Interop.Extender(this).setPhysicalCameraId(it) } }
            .build()
```

`rebuildMultiplexedAnalyzer()` (~line 428) — same on `ImageAnalysis.Builder()`:

```kotlin
        imageAnalyzer = ImageAnalysis.Builder()
            /* existing configuration unchanged */
            .apply { pinnedPhysicalId?.let { Camera2Interop.Extender(this).setPhysicalCameraId(it) } }
            .build()
```

Note: `VideoCapture.Builder` has no Camera2Interop extender — video recording on a pinned physical lens records whatever the logical camera resolves to. Known ceiling; acceptable (photo/preview/analysis are the pinned paths).

- [ ] **Step 4: Verify compile + tests**

Run: `./gradlew cameraK:compileDebugKotlinAndroid cameraK:desktopTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Build the Sample APK as an integration smoke check**

Run: `./gradlew Sample:assembleDebug`
Expected: BUILD SUCCESSFUL. (On-device verification: switch to ULTRA_WIDE in the Sample and confirm a visibly wider field of view — manual step, note it in the PR description.)

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add -u cameraK/src/androidMain
git commit -m "feat(android): bind ultra-wide/tele lenses via snapshot ids + physical-camera pinning

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: iOS — extract discovery helper + real capabilities

**Files:**
- Modify: `cameraK/src/appleMain/kotlin/com/kashif/cameraK/controller/CustomCameraController.kt`:
  - Add `discoverDevices(...)` helper; use it in `setupInputs` (~line 176) and `switchToDeviceType` (~lines 531-545)
  - Add `getCameraCapabilities(): List<LensInfo>`
- Modify: `cameraK/src/appleMain/kotlin/com/kashif/cameraK/controller/CameraController.apple.kt` (replace Task 3's placeholder actual)

**Interfaces:**
- Consumes: `LensInfo`, `CameraCapabilities` (Task 1).
- Produces: `CustomCameraController.getCameraCapabilities(): List<LensInfo>`; `CameraController.getCameraCapabilities(): CameraCapabilities` actual.

- [ ] **Step 1: Add the discovery helper and refactor both call sites**

In `CustomCameraController.kt`:

```kotlin
    /**
     * Single discovery entry point — setupInputs, switchToDeviceType and
     * getCameraCapabilities all enumerate through here.
     */
    private fun discoverDevices(
        deviceTypes: List<String>,
        position: AVCaptureDevicePosition,
    ): List<AVCaptureDevice> =
        AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            deviceTypes,
            AVMediaTypeVideo,
            position,
        ).devices.map { it as AVCaptureDevice }

    private fun allLensDeviceTypes(): List<String> = listOfNotNull(
        AVCaptureDeviceTypeBuiltInWideAngleCamera,
        AVCaptureDeviceTypeBuiltInTelephotoCamera,
        AVCaptureDeviceTypeBuiltInUltraWideCamera,
    )
```

In `setupInputs` (~line 176), replace the inline `AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(...)` block with:

```kotlin
        val discovered = discoverDevices(
            deviceTypeString?.let { listOf(it) } ?: allLensDeviceTypes(),
            AVCaptureDevicePositionUnspecified,
        )
        val devices = discovered.ifEmpty {
            listOfNotNull(AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) as? AVCaptureDevice)
        }
```

(The `devices.forEach { ... }` and `findByTypeAndPosition` logic below stays, but the `as AVCaptureDevice` casts can be dropped since the list is now typed.)

In `switchToDeviceType` (~lines 531-545), replace both discovery blocks with:

```kotlin
        val newDevice = discoverDevices(listOf(targetType), position).firstOrNull()
            ?: discoverDevices(listOf(targetType), AVCaptureDevicePositionUnspecified).firstOrNull()
            ?: return
```

- [ ] **Step 2: Add the capabilities mapping**

```kotlin
    /**
     * Enumerates every built-in lens (wide, telephoto, ultra-wide) on both positions.
     */
    fun getCameraCapabilities(): List<LensInfo> =
        discoverDevices(allLensDeviceTypes(), AVCaptureDevicePositionUnspecified).map { device ->
            LensInfo(
                id = device.uniqueID,
                deviceType = when (device.deviceType) {
                    AVCaptureDeviceTypeBuiltInUltraWideCamera -> CameraDeviceType.ULTRA_WIDE
                    AVCaptureDeviceTypeBuiltInTelephotoCamera -> CameraDeviceType.TELEPHOTO
                    AVCaptureDeviceTypeBuiltInWideAngleCamera -> CameraDeviceType.WIDE_ANGLE
                    else -> CameraDeviceType.DEFAULT
                },
                lens = if (device.position == AVCaptureDevicePositionFront) CameraLens.FRONT else CameraLens.BACK,
                minZoom = device.minAvailableVideoZoomFactor.toFloat(),
                maxZoom = device.maxAvailableVideoZoomFactor.toFloat(),
                hasFlash = device.hasFlash,
                isLogical = false,
            )
        }
```

Imports needed in `CustomCameraController.kt`: `com.kashif.cameraK.capabilities.LensInfo`, `com.kashif.cameraK.enums.CameraDeviceType` (already imported: check), `com.kashif.cameraK.enums.CameraLens` (already imported).

- [ ] **Step 3: Replace the apple actual**

In `CameraController.apple.kt` (import `com.kashif.cameraK.capabilities.CameraCapabilities`):

```kotlin
    actual fun getCameraCapabilities(): CameraCapabilities =
        CameraCapabilities(customCameraController.getCameraCapabilities())
```

- [ ] **Step 4: Verify iOS compiles**

Run: `./gradlew cameraK:compileKotlinIosSimulatorArm64 cameraK:compileKotlinIosArm64`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add -u cameraK/src/appleMain
git commit -m "feat(ios): getCameraCapabilities via unified device discovery helper

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 7: Docs + full gate

**Files:**
- Modify: `README.MD` (the `CameraKStateHolder` API listing ~line 720 and the `CameraController` listing ~line 750; feature bullet ~line 22)

**Interfaces:** none — documentation only.

- [ ] **Step 1: Document the API in README**

In the feature bullet (~line 22), change `Aspect ratios, zoom, focus, flash control` → `Aspect ratios, zoom, focus, flash, lens selection (ultra-wide/tele) with hardware capability query`. In the `CameraKStateHolder` API listing add `fun getCameraCapabilities(): CameraCapabilities` next to the other functions, and in the `CameraController` listing add:

```kotlin
    // Hardware capabilities
    fun getCameraCapabilities(): CameraCapabilities
```

Plus a short usage snippet in the appropriate examples area:

```kotlin
val capabilities = stateHolder.getCameraCapabilities()
if (CameraDeviceType.ULTRA_WIDE in capabilities.availableDeviceTypes(CameraLens.BACK)) {
    stateHolder.setPreferredCameraDeviceType(CameraDeviceType.ULTRA_WIDE)
}
```

- [ ] **Step 2: Run the full gate**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL (formatting + all target tests).

- [ ] **Step 3: Commit**

```bash
git add README.MD
git commit -m "docs: document getCameraCapabilities + lens selection

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```
