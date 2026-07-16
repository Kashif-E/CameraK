package com.kashif.cameraK.capabilities

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.kashif.cameraK.enums.CameraDeviceType
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.utils.CameraKLogger

/** [LensInfo] plus the Android-only binding facts the selector needs. */
internal class AndroidLensDescriptor(val info: LensInfo, val isPhysicalChild: Boolean)

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
            val raw = describe(manager, id, isPhysicalChild = false) ?: continue
            rawByFacing.getOrPut(raw.facing) { mutableListOf() }.add(raw)
            if (raw.isLogical) {
                for (physicalId in raw.physicalCameraIds) {
                    val child = describe(manager, physicalId, isPhysicalChild = true) ?: continue
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
        val focalLengthsMm: List<Float>,
        val physicalSizeWidthMm: Float?,
        val physicalCameraIds: Set<String>,
        val minFocusDistance: Float,
    )

    private fun describe(manager: CameraManager?, id: String, isPhysicalChild: Boolean): RawCamera? {
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
            focalLengthsMm = characteristics.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS,
            )?.toList().orEmpty(),
            physicalSizeWidthMm = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.width,
            physicalCameraIds = physicalIds,
            minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f,
        )
    }
}
