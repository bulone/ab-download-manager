package com.abdownloadmanager.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ABDMIcons.Battery: ImageVector
    get() {
        if (_Battery != null) {
            return _Battery!!
        }
        _Battery = ImageVector.Builder(
            name = "Battery",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // outline only: stroke instead of fill, so it reads as a line-style icon
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // cap sitting on top of the body
                moveTo(10f, 5f)
                lineTo(10f, 2.5f)
                lineTo(14f, 2.5f)
                lineTo(14f, 5f)
                // body with rounded corners
                moveTo(9f, 5f)
                lineTo(15f, 5f)
                quadTo(17f, 5f, 17f, 7f)
                lineTo(17f, 20f)
                quadTo(17f, 22f, 15f, 22f)
                lineTo(9f, 22f)
                quadTo(7f, 22f, 7f, 20f)
                lineTo(7f, 7f)
                quadTo(7f, 5f, 9f, 5f)
                close()
            }
        }.build()

        return _Battery!!
    }

@Suppress("ObjectPropertyName")
private var _Battery: ImageVector? = null
