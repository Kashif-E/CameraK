package org.company.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Crop
import com.composables.icons.lucide.Flashlight
import com.composables.icons.lucide.FlashlightOff
import com.composables.icons.lucide.Frame
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SwitchCamera
import com.composables.icons.lucide.X
import com.composables.icons.lucide.Zap
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.AspectRatio
import com.kashif.cameraK.enums.CameraDeviceType
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.QualityPrioritization
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.permissions.Permissions
import com.kashif.cameraK.permissions.providePermissions
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.ui.CameraPreview
import com.kashif.imagesaverplugin.ImageSaverPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.company.app.theme.AppTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun LegacyAppContent(imageSaverPlugin: ImageSaverPlugin, onToggleApi: () -> Unit = {}) = AppTheme {
    val permissions: Permissions = providePermissions()

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
    ) {
        val cameraPermissionState = remember { mutableStateOf(permissions.hasCameraPermission()) }
        val storagePermissionState = remember { mutableStateOf(permissions.hasStoragePermission()) }

        val cameraController = remember { mutableStateOf<CameraController?>(null) }

        PermissionsHandler(
            permissions = permissions,
            cameraPermissionState = cameraPermissionState,
            storagePermissionState = storagePermissionState,
        )

        if (cameraPermissionState.value && storagePermissionState.value) {
            CameraContent(
                cameraController = cameraController,
                imageSaverPlugin = imageSaverPlugin,
                onToggleApi = onToggleApi,
            )
        }
    }
}

@Composable
private fun PermissionsHandler(
    permissions: Permissions,
    cameraPermissionState: MutableState<Boolean>,
    storagePermissionState: MutableState<Boolean>,
) {
    if (!cameraPermissionState.value) {
        permissions.RequestCameraPermission(
            onGranted = { cameraPermissionState.value = true },
            onDenied = { println("Camera Permission Denied") },
        )
    }

    if (!storagePermissionState.value) {
        permissions.RequestStoragePermission(
            onGranted = { storagePermissionState.value = true },
            onDenied = { println("Storage Permission Denied") },
        )
    }
}

@Composable
private fun CameraContent(
    cameraController: MutableState<CameraController?>,
    imageSaverPlugin: ImageSaverPlugin,
    onToggleApi: () -> Unit = {},
) {
    var aspectRatio by remember { mutableStateOf(AspectRatio.RATIO_4_3) }
    var resolution by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var imageFormat by remember { mutableStateOf(ImageFormat.JPEG) }
    var qualityPrioritization by remember { mutableStateOf(QualityPrioritization.BALANCED) }
    var cameraDeviceType by remember { mutableStateOf(CameraDeviceType.WIDE_ANGLE) }
    var configVersion by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        key(configVersion) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                cameraConfiguration = {
                    setCameraLens(CameraLens.BACK)
                    setFlashMode(FlashMode.OFF)
                    setImageFormat(imageFormat)
                    setDirectory(Directory.PICTURES)
                    setTorchMode(TorchMode.OFF)
                    setQualityPrioritization(qualityPrioritization)
                    setPreferredCameraDeviceType(cameraDeviceType)
                    setAspectRatio(aspectRatio)
                    resolution?.let { setResolution(it.first, it.second) }
                    addPlugin(imageSaverPlugin)
                },
                onCameraControllerReady = {
                    cameraController.value = it
                },
            )
        }

        cameraController.value?.let { controller ->
            EnhancedCameraScreen(
                cameraController = controller,
                imageSaverPlugin = imageSaverPlugin,
                aspectRatio = aspectRatio,
                resolution = resolution,
                imageFormat = imageFormat,
                qualityPrioritization = qualityPrioritization,
                cameraDeviceType = cameraDeviceType,
                onAspectRatioChange = {
                    aspectRatio = it
                    cameraController.value = null
                    configVersion++
                },
                onResolutionChange = {
                    resolution = it
                    cameraController.value = null
                    configVersion++
                },
                onImageFormatChange = {
                    imageFormat = it
                    cameraController.value = null
                    configVersion++
                },
                onQualityPrioritizationChange = {
                    qualityPrioritization = it
                    cameraController.value = null
                    configVersion++
                },
                onCameraDeviceTypeChange = {
                    cameraDeviceType = it
                    cameraController.value = null
                    configVersion++
                },
                onToggleApi = onToggleApi,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedCameraScreen(
    cameraController: CameraController,
    imageSaverPlugin: ImageSaverPlugin,
    aspectRatio: AspectRatio,
    resolution: Pair<Int, Int>?,
    imageFormat: ImageFormat,
    qualityPrioritization: QualityPrioritization,
    cameraDeviceType: CameraDeviceType,
    onAspectRatioChange: (AspectRatio) -> Unit,
    onResolutionChange: (Pair<Int, Int>?) -> Unit,
    onImageFormatChange: (ImageFormat) -> Unit,
    onQualityPrioritizationChange: (QualityPrioritization) -> Unit,
    onCameraDeviceTypeChange: (CameraDeviceType) -> Unit,
    onToggleApi: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // Camera settings state
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var torchMode by remember { mutableStateOf(TorchMode.OFF) }
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }

    // Bottom sheet state
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = false,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    LaunchedEffect(cameraController) {
        maxZoom = cameraController.getMaxZoom()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 80.dp,
        containerColor = Color.Transparent,
        sheetContainerColor = Color(0xFFF7F2E9),
        sheetContent = {
            CameraControlsBottomSheet(
                cameraController = cameraController,
                flashMode = flashMode,
                torchMode = torchMode,
                zoomLevel = zoomLevel,
                maxZoom = maxZoom,
                aspectRatio = aspectRatio,
                resolution = resolution,
                imageFormat = imageFormat,
                qualityPrioritization = qualityPrioritization,
                cameraDeviceType = cameraDeviceType,
                onFlashModeChange = {
                    flashMode = it
                    cameraController.setFlashMode(it)
                },
                onTorchModeChange = {
                    torchMode = it
                    cameraController.setTorchMode(it)
                },
                onZoomChange = {
                    zoomLevel = it
                    cameraController.setZoom(it)
                },
                onLensSwitch = {
                    cameraController.toggleCameraLens()
                    // Update max zoom for new camera
                    maxZoom = cameraController.getMaxZoom()
                    zoomLevel = 1f
                },
                onAspectRatioChange = {
                    onAspectRatioChange(it)
                },
                onResolutionChange = {
                    onResolutionChange(it)
                },
                onImageFormatChange = {
                    onImageFormatChange(it)
                },
                onQualityPrioritizationChange = {
                    onQualityPrioritizationChange(it)
                },
                onCameraDeviceTypeChange = {
                    onCameraDeviceTypeChange(it)
                },
                onToggleApi = onToggleApi,
            )
        },
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Quick controls overlay
            QuickControlsOverlay(
                modifier = Modifier.align(Alignment.TopEnd),
                flashMode = flashMode,
                torchMode = torchMode,
                onFlashToggle = {
                    cameraController.toggleFlashMode()
                    flashMode = cameraController.getFlashMode() ?: FlashMode.OFF
                },
                onTorchToggle = {
                    cameraController.toggleTorchMode()
                    torchMode = cameraController.getTorchMode() ?: TorchMode.OFF
                },
            )

            // Capture button
            CaptureButton(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
                isCapturing = isCapturing,
                onCapture = {
                    if (!isCapturing) {
                        isCapturing = true
                        scope.launch {
                            handleImageCapture(
                                cameraController = cameraController,
                                imageSaverPlugin = imageSaverPlugin,
                                onImageCaptured = { imageBitmap = it },
                            )
                            isCapturing = false
                        }
                    }
                },
            )

            // Captured image preview
            CapturedImagePreview(imageBitmap = imageBitmap) {
                imageBitmap = null
            }
        }
    }
}

@Composable
private fun QuickControlsOverlay(
    modifier: Modifier = Modifier,
    flashMode: FlashMode,
    torchMode: TorchMode,
    onFlashToggle: () -> Unit,
    onTorchToggle: () -> Unit,
) {
    Surface(
        modifier = modifier.padding(16.dp),
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onFlashToggle) {
                Icon(
                    imageVector = when (flashMode) {
                        FlashMode.ON -> Lucide.Flashlight
                        FlashMode.OFF -> Lucide.FlashlightOff
                        FlashMode.AUTO -> Lucide.Flashlight
                    },
                    contentDescription = "Flash: $flashMode",
                    tint = Color.White,
                )
            }
            IconButton(onClick = onTorchToggle) {
                Icon(
                    imageVector = if (torchMode != TorchMode.OFF) Lucide.Flashlight else Lucide.FlashlightOff,
                    contentDescription = "Torch: $torchMode",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CaptureButton(modifier: Modifier = Modifier, isCapturing: Boolean, onCapture: () -> Unit) {
    FilledTonalButton(
        onClick = onCapture,
        enabled = !isCapturing,
        modifier = modifier.size(80.dp).clip(CircleShape),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        ),
    ) {
        Icon(
            imageVector = Lucide.Camera,
            contentDescription = "Capture",
            tint = if (isCapturing) Color.White.copy(alpha = 0.5f) else Color.White,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun CameraControlsBottomSheet(
    cameraController: CameraController,
    flashMode: FlashMode,
    torchMode: TorchMode,
    zoomLevel: Float,
    maxZoom: Float,
    aspectRatio: AspectRatio,
    resolution: Pair<Int, Int>?,
    imageFormat: ImageFormat,
    qualityPrioritization: QualityPrioritization,
    cameraDeviceType: CameraDeviceType,
    onFlashModeChange: (FlashMode) -> Unit,
    onTorchModeChange: (TorchMode) -> Unit,
    onZoomChange: (Float) -> Unit,
    onLensSwitch: () -> Unit,
    onAspectRatioChange: (AspectRatio) -> Unit,
    onResolutionChange: (Pair<Int, Int>?) -> Unit,
    onImageFormatChange: (ImageFormat) -> Unit,
    onQualityPrioritizationChange: (QualityPrioritization) -> Unit,
    onCameraDeviceTypeChange: (CameraDeviceType) -> Unit,
    onToggleApi: () -> Unit = {},
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(3) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Camera Controls",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF221B00),
                    )
                    Text(
                        text = "Legacy API",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF221B00),
                    )
                }
                FilledTonalButton(
                    onClick = onToggleApi,
                    modifier = Modifier.widthIn(min = 100.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFD4A574),
                        contentColor = Color(0xFF221B00),
                    ),
                ) {
                    Text(
                        "Switch API",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Zoom Control
        if (maxZoom > 1f) {
            item(span = { GridItemSpan(2) }) {
                ZoomControl(
                    zoomLevel = zoomLevel,
                    maxZoom = maxZoom,
                    onZoomChange = onZoomChange,
                )
            }
        }

        // Flash Mode Control
        item {
            FlashModeControl(
                flashMode = flashMode,
                onFlashModeChange = onFlashModeChange,
            )
        }

        // Torch Mode Control
        item {
            TorchModeControl(
                torchMode = torchMode,
                onTorchModeChange = onTorchModeChange,
            )
        }

        item {
            AspectRatioControl(
                aspectRatio = aspectRatio,
                onAspectRatioChange = onAspectRatioChange,
            )
        }

        item {
            ResolutionControl(
                resolution = resolution,
                onResolutionChange = onResolutionChange,
            )
        }

        item {
            ImageFormatControl(
                imageFormat = imageFormat,
                onImageFormatChange = onImageFormatChange,
            )
        }

        item {
            QualityPrioritizationControl(
                qualityPrioritization = qualityPrioritization,
                onQualityPrioritizationChange = onQualityPrioritizationChange,
            )
        }

        item {
            CameraDeviceTypeControl(
                cameraDeviceType = cameraDeviceType,
                onCameraDeviceTypeChange = onCameraDeviceTypeChange,
            )
        }

        // Camera Lens Switch
        item {
            CameraLensControl(onLensSwitch = onLensSwitch)
        }
    }
}

@Composable
private fun ZoomControl(zoomLevel: Float, maxZoom: Float, onZoomChange: (Float) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Zoom",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${formatString("%.1f", zoomLevel)}x",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = zoomLevel,
            onValueChange = onZoomChange,
            valueRange = 1f..maxZoom,
            steps = ((maxZoom - 1f) * 10).toInt().coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "1.0x",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${formatString("%.1f", maxZoom)}x",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FlashModeControl(flashMode: FlashMode, onFlashModeChange: (FlashMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = when (flashMode) {
                FlashMode.ON -> Lucide.Flashlight
                FlashMode.OFF -> Lucide.FlashlightOff
                FlashMode.AUTO -> Lucide.Flashlight
            },
            contentDescription = "Flash Mode",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )

        Text(
            text = flashMode.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF221B00),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            FlashMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.name) },
                    onClick = {
                        onFlashModeChange(mode)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (mode) {
                                FlashMode.ON -> Lucide.Flashlight
                                FlashMode.OFF -> Lucide.FlashlightOff
                                FlashMode.AUTO -> Lucide.Flashlight
                            },
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TorchModeControl(torchMode: TorchMode, onTorchModeChange: (TorchMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (torchMode != TorchMode.OFF) Lucide.Flashlight else Lucide.FlashlightOff,
            contentDescription = "Torch Mode",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )

        Text(
            text = torchMode.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF221B00),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TorchMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.name) },
                    onClick = {
                        onTorchModeChange(mode)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (mode != TorchMode.OFF) Lucide.Flashlight else Lucide.FlashlightOff,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AspectRatioControl(aspectRatio: AspectRatio, onAspectRatioChange: (AspectRatio) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Lucide.Crop,
            contentDescription = "Aspect Ratio",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )

        Text(
            text = aspectRatio.name.replace("RATIO_", "").replace("_", ":"),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF221B00),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AspectRatio.entries.forEach { ratio ->
                DropdownMenuItem(
                    text = { Text(ratio.name.replace("RATIO_", "").replace("_", ":")) },
                    onClick = {
                        onAspectRatioChange(ratio)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ResolutionControl(resolution: Pair<Int, Int>?, onResolutionChange: (Pair<Int, Int>?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf<Pair<Int, Int>?>(null, 1920 to 1080, 1280 to 720, 640 to 480)

    fun label(pair: Pair<Int, Int>?): String = pair?.let { "${it.first}x${it.second}" } ?: "Auto"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Lucide.Frame,
            contentDescription = "Resolution",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )

        Text(
            text = label(resolution),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF221B00),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        onResolutionChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ImageFormatControl(imageFormat: ImageFormat, onImageFormatChange: (ImageFormat) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Lucide.Image,
            contentDescription = "Image Format",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )

        Text(
            text = imageFormat.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF221B00),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ImageFormat.entries.forEach { format ->
                DropdownMenuItem(
                    text = { Text(format.name) },
                    onClick = {
                        onImageFormatChange(format)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun QualityPrioritizationControl(
    qualityPrioritization: QualityPrioritization,
    onQualityPrioritizationChange: (QualityPrioritization) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Lucide.Zap,
            contentDescription = "Quality Priority",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )

        Text(
            text = qualityPrioritization.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF221B00),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            QualityPrioritization.entries.forEach { priority ->
                DropdownMenuItem(
                    text = { Text(priority.name) },
                    onClick = {
                        onQualityPrioritizationChange(priority)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CameraDeviceTypeControl(
    cameraDeviceType: CameraDeviceType,
    onCameraDeviceTypeChange: (CameraDeviceType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Lucide.SwitchCamera,
            contentDescription = "Camera Type",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )

        Text(
            text = cameraDeviceType.name.replace("_", " "),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF221B00),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CameraDeviceType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name.replace("_", " ")) },
                    onClick = {
                        onCameraDeviceTypeChange(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CameraLensControl(onLensSwitch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLensSwitch)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Lucide.SwitchCamera,
            contentDescription = "Switch Camera",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = "Switch Camera",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF221B00),
        )
    }
}

@Composable
private fun TopControlsBar(
    isFlashOn: Boolean,
    isTorchOn: Boolean,
    onFlashToggle: (Boolean) -> Unit,
    onTorchToggle: (Boolean) -> Unit,
    onLensToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CameraControlSwitch(
                    icon = if (isFlashOn) Lucide.Flashlight else Lucide.FlashlightOff,
                    text = "Flash",
                    checked = isFlashOn,
                    onCheckedChange = onFlashToggle,
                )

                CameraControlSwitch(
                    icon = if (isTorchOn) Lucide.Flashlight else Lucide.FlashlightOff,
                    text = "Torch",
                    checked = isTorchOn,
                    onCheckedChange = onTorchToggle,
                )
            }

            IconButton(
                onClick = onLensToggle,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Lucide.SwitchCamera,
                    contentDescription = "Toggle Camera",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CameraControlSwitch(icon: ImageVector, text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
    }
}

@Composable
private fun BottomControls(modifier: Modifier = Modifier, isCapturing: Boolean = false, onCapture: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        FilledTonalButton(
            onClick = onCapture,
            enabled = !isCapturing,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            ),
        ) {
            Icon(
                imageVector = Lucide.Camera,
                contentDescription = "Capture",
                tint = if (isCapturing) Color.White.copy(alpha = 0.5f) else Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun CapturedImagePreview(imageBitmap: ImageBitmap?, onDismiss: () -> Unit) {
    imageBitmap?.let { bitmap ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.9f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit,
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
                            CircleShape,
                        ),
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Close Preview",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.rotate(120f),
                    )
                }
            }
        }

        LaunchedEffect(bitmap) {
            delay(3000)
            onDismiss()
        }
    }
}

/**
 * Handles image capture results using the new takePictureToFile() method.
 *
 * This method is significantly faster than takePicture() as it:
 * - Saves directly to disk without ByteArray conversion (2-3 seconds faster)
 * - Skips decode/encode cycles
 * - Returns file path immediately
 *
 * The ImageSaverPlugin's isAutoSave configuration is respected:
 * - When isAutoSave = true: Plugin's listener saves the ByteArray in background
 * - File path is still returned for immediate use
 */
@OptIn(ExperimentalResourceApi::class, ExperimentalUuidApi::class)
private suspend fun handleImageCapture(
    cameraController: CameraController,
    imageSaverPlugin: ImageSaverPlugin,
    onImageCaptured: (ImageBitmap) -> Unit,
) {
    when (val result = cameraController.takePictureToFile()) {
        is ImageCaptureResult.SuccessWithFile -> {
            // Image saved directly to file - significantly faster!
            println("Image captured and saved at: ${result.filePath}")

            // Optional: Load image from file for preview
            // val bitmap = imageSaverPlugin.getByteArrayFrom(result.filePath).decodeToImageBitmap()
            // onImageCaptured(bitmap)
        }

        is ImageCaptureResult.Success -> {
            // Fallback for platforms that don't support direct file capture
            println("Image captured successfully (${result.byteArray.size} bytes)")
        }

        is ImageCaptureResult.Error -> {
            println("Image Capture Error: ${result.exception.message}")
        }
    }
}
