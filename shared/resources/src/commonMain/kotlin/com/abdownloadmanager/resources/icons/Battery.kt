package com.abdownloadmanager.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
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
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                // cap sitting on top of the body
                moveTo(10f, 2f)
                lineTo(14f, 2f)
                lineTo(14f, 4f)
                // body, rounded corners
                lineTo(15.67f, 4f)
                quadTo(17f, 4f, 17f, 5.33f)
                lineTo(17f, 20.67f)
                quadTo(17f, 22f, 15.67f, 22f)
                lineTo(8.33f, 22f)
                quadTo(7f, 22f, 7f, 20.67f)
                lineTo(7f, 5.33f)
                quadTo(7f, 4f, 8.33f, 4f)
                lineTo(10f, 4f)
                close()
            }
        }.build()

        return _Battery!!
    }

@Suppress("ObjectPropertyName")
private var _Battery: ImageVector? = null
