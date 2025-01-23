package org.company.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.Directory
import com.kashif.cameraK.enums.FlashMode
import com.kashif.cameraK.enums.ImageFormat
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.permissions.Permissions
import com.kashif.cameraK.permissions.providePermissions
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.ui.CameraPreview
import com.kashif.imagesaverplugin.ImageSaverConfig
import com.kashif.imagesaverplugin.ImageSaverPlugin
import com.kashif.imagesaverplugin.rememberImageSaverPlugin
import com.kashif.qrscannerplugin.QRScannerPlugin
import com.kashif.qrscannerplugin.rememberQRScannerPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.company.app.theme.AppTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun App() = AppTheme {
    val permissions: Permissions = providePermissions()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
    ) {
        val cameraPermissionState = remember { mutableStateOf(permissions.hasCameraPermission()) }
        val storagePermissionState = remember { mutableStateOf(permissions.hasStoragePermission()) }
        val qrScannerPlugin = rememberQRScannerPlugin(coroutineScope = coroutineScope)

        // QR Code scanning effect
        LaunchedEffect(Unit) {
            qrScannerPlugin.getQrCodeFlow().distinctUntilChanged()
                .collectLatest { qrCode ->
                    snackbarHostState.showSnackbar(
                        message = "QR Code Detected: $qrCode",
                        duration = SnackbarDuration.Short
                    )
                    qrScannerPlugin.pauseScanning()
                }
        }

        val cameraController = remember { mutableStateOf<CameraController?>(null) }
        val imageSaverPlugin = rememberImageSaverPlugin(
            config = ImageSaverConfig(
                isAutoSave = false,
                prefix = "MyApp",
                directory = Directory.PICTURES,
                customFolderName = "CustomFolder"
            )
        )

        // Handle permissions
        PermissionsHandler(
            permissions = permissions,
            cameraPermissionState = cameraPermissionState,
            storagePermissionState = storagePermissionState
        )

        // Camera preview and controls
        if (cameraPermissionState.value && storagePermissionState.value) {
            CameraContent(
                cameraController = cameraController,
                imageSaverPlugin = imageSaverPlugin,
                qrScannerPlugin = qrScannerPlugin
            )
        }
    }
}

@Composable
private fun PermissionsHandler(
    permissions: Permissions,
    cameraPermissionState: MutableState<Boolean>,
    storagePermissionState: MutableState<Boolean>
) {
    if (!cameraPermissionState.value) {
        permissions.RequestCameraPermission(
            onGranted = { cameraPermissionState.value = true },
            onDenied = { println("Camera Permission Denied") }
        )
    }

    if (!storagePermissionState.value) {
        permissions.RequestStoragePermission(
            onGranted = { storagePermissionState.value = true },
            onDenied = { println("Storage Permission Denied") }
        )
    }
}

@Composable
private fun CameraContent(
    cameraController: MutableState<CameraController?>,
    imageSaverPlugin: ImageSaverPlugin,
    qrScannerPlugin: QRScannerPlugin
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            cameraConfiguration = {
                setCameraLens(CameraLens.BACK)
                setFlashMode(FlashMode.OFF)
                setImageFormat(ImageFormat.JPEG)
                setDirectory(Directory.PICTURES)
                setTorchMode(TorchMode.OFF)
                addPlugin(imageSaverPlugin)
                addPlugin(qrScannerPlugin)
            },
            onCameraControllerReady = {
                cameraController.value = it

            }
        )

        cameraController.value?.let { controller ->
            LaunchedEffect(controller) {
                qrScannerPlugin.startScanning()
            }
            EnhancedCameraScreen(
                cameraController = controller,
                imageSaverPlugin = imageSaverPlugin
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class, ExperimentalUuidApi::class)
@Composable
fun EnhancedCameraScreen(
    cameraController: CameraController,
    imageSaverPlugin: ImageSaverPlugin
) {
    val scope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Top controls bar
        TopControlsBar(
            isFlashOn = isFlashOn,
            isTorchOn = isTorchOn,
            onFlashToggle = {
                isFlashOn = it
                cameraController.toggleFlashMode()
            },
            onTorchToggle = {
                isTorchOn = it
                cameraController.toggleTorchMode()
            },
            onLensToggle = { cameraController.toggleCameraLens() }
        )

        // Capture button and preview
        BottomControls(
            modifier = Modifier.align(Alignment.BottomCenter),
            onCapture = {
                scope.launch {
                    handleImageCapture(
                        cameraController = cameraController,
                        imageSaverPlugin = imageSaverPlugin,
                        onImageCaptured = { imageBitmap = it }
                    )
                }
            }
        )

        // Captured image preview
        CapturedImagePreview(imageBitmap = imageBitmap) {
            imageBitmap = null
        }
    }
}

@Composable
private fun TopControlsBar(
    isFlashOn: Boolean,
    isTorchOn: Boolean,
    onFlashToggle: (Boolean) -> Unit,
    onTorchToggle: (Boolean) -> Unit,
    onLensToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CameraControlSwitch(
                    icon = if (isFlashOn) Flashlight else FlashlightOff,
                    text = "Flash",
                    checked = isFlashOn,
                    onCheckedChange = onFlashToggle
                )

                CameraControlSwitch(
                    icon = if (isTorchOn) Flash_on else Flash_off,
                    text = "Torch",
                    checked = isTorchOn,
                    onCheckedChange = onTorchToggle
                )
            }

            IconButton(
                onClick = onLensToggle,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = SwitchCamera,
                    contentDescription = "Toggle Camera",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CameraControlSwitch(
    icon: ImageVector,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun BottomControls(modifier: Modifier = Modifier, onCapture: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        FilledTonalButton(
            onClick = onCapture,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Camera,
                contentDescription = "Capture",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun CapturedImagePreview(
    imageBitmap: ImageBitmap?,
    onDismiss: () -> Unit
) {
    imageBitmap?.let { bitmap ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.9f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Cross,
                        contentDescription = "Close Preview",
                        tint =MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.rotate(120f)
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

@OptIn(ExperimentalResourceApi::class, ExperimentalUuidApi::class)
private suspend fun handleImageCapture(
    cameraController: CameraController,
    imageSaverPlugin: ImageSaverPlugin,
    onImageCaptured: (ImageBitmap) -> Unit
) {
    when (val result = cameraController.takePicture()) {
        is ImageCaptureResult.Success -> {
            val bitmap = result.byteArray.decodeToImageBitmap()
            onImageCaptured(bitmap)

            if (!imageSaverPlugin.config.isAutoSave) {
                val customName = "Manual_${Uuid.random().toHexString()}"
                imageSaverPlugin.saveImage(
                    byteArray = result.byteArray,
                    imageName = customName
                )?.let { path ->
                    println("Image saved at: $path")
                }
            }
        }

        is ImageCaptureResult.Error -> {
            println("Image Capture Error: ${result.exception.message}")
        }
    }
}


public val Camera: ImageVector
    get() {
        if (_Camera != null) {
            return _Camera!!
        }
        _Camera = ImageVector.Builder(
            name = "Camera",
            defaultWidth = 15.dp,
            defaultHeight = 15.dp,
            viewportWidth = 15f,
            viewportHeight = 15f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(2f, 3f)
                curveTo(1.4477f, 3f, 1f, 3.4477f, 1f, 4f)
                verticalLineTo(11f)
                curveTo(1f, 11.5523f, 1.4477f, 12f, 2f, 12f)
                horizontalLineTo(13f)
                curveTo(13.5523f, 12f, 14f, 11.5523f, 14f, 11f)
                verticalLineTo(4f)
                curveTo(14f, 3.4477f, 13.5523f, 3f, 13f, 3f)
                horizontalLineTo(2f)
                close()
                moveTo(0f, 4f)
                curveTo(0f, 2.8954f, 0.8954f, 2f, 2f, 2f)
                horizontalLineTo(13f)
                curveTo(14.1046f, 2f, 15f, 2.8954f, 15f, 4f)
                verticalLineTo(11f)
                curveTo(15f, 12.1046f, 14.1046f, 13f, 13f, 13f)
                horizontalLineTo(2f)
                curveTo(0.8954f, 13f, 0f, 12.1046f, 0f, 11f)
                verticalLineTo(4f)
                close()
                moveTo(2f, 4.25f)
                curveTo(2f, 4.1119f, 2.1119f, 4f, 2.25f, 4f)
                horizontalLineTo(4.75f)
                curveTo(4.8881f, 4f, 5f, 4.1119f, 5f, 4.25f)
                verticalLineTo(5.75454f)
                curveTo(5f, 5.8926f, 4.8881f, 6.0045f, 4.75f, 6.0045f)
                horizontalLineTo(2.25f)
                curveTo(2.1119f, 6.0045f, 2f, 5.8926f, 2f, 5.7545f)
                verticalLineTo(4.25f)
                close()
                moveTo(12.101f, 7.58421f)
                curveTo(12.101f, 9.0207f, 10.9365f, 10.1853f, 9.5f, 10.1853f)
                curveTo(8.0635f, 10.1853f, 6.8989f, 9.0207f, 6.8989f, 7.5842f)
                curveTo(6.8989f, 6.1477f, 8.0635f, 4.9832f, 9.5f, 4.9832f)
                curveTo(10.9365f, 4.9832f, 12.101f, 6.1477f, 12.101f, 7.5842f)
                close()
                moveTo(13.101f, 7.58421f)
                curveTo(13.101f, 9.573f, 11.4888f, 11.1853f, 9.5f, 11.1853f)
                curveTo(7.5112f, 11.1853f, 5.8989f, 9.573f, 5.8989f, 7.5842f)
                curveTo(5.8989f, 5.5954f, 7.5112f, 3.9832f, 9.5f, 3.9832f)
                curveTo(11.4888f, 3.9832f, 13.101f, 5.5954f, 13.101f, 7.5842f)
                close()
            }
        }.build()
        return _Camera!!
    }

private var _Camera: ImageVector? = null


public val FlashlightOff: ImageVector
    get() {
        if (_FlashlightOff != null) {
            return _FlashlightOff!!
        }
        _FlashlightOff = ImageVector.Builder(
            name = "FlashlightOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(16f, 16f)
                verticalLineToRelative(4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
                horizontalLineToRelative(-4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
                verticalLineTo(10f)
                curveToRelative(0f, -2f, -2f, -2f, -2f, -4f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(7f, 2f)
                horizontalLineToRelative(11f)
                verticalLineToRelative(4f)
                curveToRelative(0f, 2f, -2f, 2f, -2f, 4f)
                verticalLineToRelative(1f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(11f, 6f)
                lineTo(18f, 6f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(2f, 2f)
                lineTo(22f, 22f)
            }
        }.build()
        return _FlashlightOff!!
    }

private var _FlashlightOff: ImageVector? = null


public val Flashlight: ImageVector
    get() {
        if (_Flashlight != null) {
            return _Flashlight!!
        }
        _Flashlight = ImageVector.Builder(
            name = "Flashlight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(18f, 6f)
                curveToRelative(0f, 2f, -2f, 2f, -2f, 4f)
                verticalLineToRelative(10f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
                horizontalLineToRelative(-4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
                verticalLineTo(10f)
                curveToRelative(0f, -2f, -2f, -2f, -2f, -4f)
                verticalLineTo(2f)
                horizontalLineToRelative(12f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(6f, 6f)
                lineTo(18f, 6f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 12f)
                lineTo(12f, 12f)
            }
        }.build()
        return _Flashlight!!
    }

private var _Flashlight: ImageVector? = null


public val Flash_on: ImageVector
    get() {
        if (_Flash_on != null) {
            return _Flash_on!!
        }
        _Flash_on = ImageVector.Builder(
            name = "Flash_on",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(
                fill = SolidColor(androidx.compose.ui.graphics.Color.Black),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 0.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(480f, 624f)
                lineToRelative(128f, -184f)
                horizontalLineTo(494f)
                lineToRelative(80f, -280f)
                horizontalLineTo(360f)
                verticalLineToRelative(320f)
                horizontalLineToRelative(120f)
                close()
                moveTo(400f, 880f)
                verticalLineToRelative(-320f)
                horizontalLineTo(280f)
                verticalLineToRelative(-480f)
                horizontalLineToRelative(400f)
                lineToRelative(-80f, 280f)
                horizontalLineToRelative(160f)
                close()
                moveToRelative(80f, -400f)
                horizontalLineTo(360f)
                close()
            }
        }.build()
        return _Flash_on!!
    }

private var _Flash_on: ImageVector? = null


public val Flash_off: ImageVector
    get() {
        if (_Flash_off != null) {
            return _Flash_off!!
        }
        _Flash_off = ImageVector.Builder(
            name = "Flash_off",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(
                fill = SolidColor(androidx.compose.ui.graphics.Color.Black),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 0.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(280f, 80f)
                horizontalLineToRelative(400f)
                lineToRelative(-80f, 280f)
                horizontalLineToRelative(160f)
                lineTo(643f, 529f)
                lineToRelative(-57f, -57f)
                lineToRelative(22f, -32f)
                horizontalLineToRelative(-54f)
                lineToRelative(-47f, -47f)
                lineToRelative(67f, -233f)
                horizontalLineTo(360f)
                verticalLineToRelative(86f)
                lineToRelative(-80f, -80f)
                close()
                moveTo(400f, 880f)
                verticalLineToRelative(-320f)
                horizontalLineTo(280f)
                verticalLineToRelative(-166f)
                lineTo(55f, 169f)
                lineToRelative(57f, -57f)
                lineToRelative(736f, 736f)
                lineToRelative(-57f, 57f)
                lineToRelative(-241f, -241f)
                close()
                moveToRelative(73f, -521f)
            }
        }.build()
        return _Flash_off!!
    }

private var _Flash_off: ImageVector? = null


public val SwitchCamera: ImageVector
    get() {
        if (_SwitchCamera != null) {
            return _SwitchCamera!!
        }
        _SwitchCamera = ImageVector.Builder(
            name = "SwitchCamera",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(11f, 19f)
                horizontalLineTo(4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
                verticalLineTo(7f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, -2f)
                horizontalLineToRelative(5f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(13f, 5f)
                horizontalLineToRelative(7f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 2f)
                verticalLineToRelative(10f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
                horizontalLineToRelative(-5f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(15f, 12f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 15f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 9f, 12f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15f, 12f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(18f, 22f)
                lineToRelative(-3f, -3f)
                lineToRelative(3f, -3f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(6f, 2f)
                lineToRelative(3f, 3f)
                lineToRelative(-3f, 3f)
            }
        }.build()
        return _SwitchCamera!!
    }

private var _SwitchCamera: ImageVector? = null


public val Cross: ImageVector
    get() {
        if (_Cross != null) {
            return _Cross!!
        }
        _Cross = ImageVector.Builder(
            name = "Cross",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(11f, 2f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, 2f)
                verticalLineToRelative(5f)
                horizontalLineTo(4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, 2f)
                verticalLineToRelative(2f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(5f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(2f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, -2f)
                verticalLineToRelative(-5f)
                horizontalLineToRelative(5f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, -2f)
                verticalLineToRelative(-2f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, -2f)
                horizontalLineToRelative(-5f)
                verticalLineTo(4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, -2f)
                horizontalLineToRelative(-2f)
                close()
            }
        }.build()
        return _Cross!!
    }

private var _Cross: ImageVector? = null
