package com.composables.icons.lucide

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Lucide.Camera: ImageVector
    get() {
        if (_Camera != null) return _Camera!!

        _Camera = ImageVector.Builder(
            name = "camera",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.997f, 4f)
                arcToRelative(2f, 2f, 0f, false, true, 1.76f, 1.05f)
                lineToRelative(0.486f, 0.9f)
                arcTo(2f, 2f, 0f, false, false, 18.003f, 7f)
                horizontalLineTo(20f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
                verticalLineToRelative(9f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
                horizontalLineTo(4f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
                verticalLineTo(9f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                horizontalLineToRelative(1.997f)
                arcToRelative(2f, 2f, 0f, false, false, 1.759f, -1.048f)
                lineToRelative(0.489f, -0.904f)
                arcTo(2f, 2f, 0f, false, true, 10.004f, 4f)
                close()
            }
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 13f)
                arcTo(3f, 3f, 0f, false, true, 12f, 16f)
                arcTo(3f, 3f, 0f, false, true, 9f, 13f)
                arcTo(3f, 3f, 0f, false, true, 15f, 13f)
                close()
            }
        }.build()

        return _Camera!!
    }

private var _Camera: ImageVector? = null
