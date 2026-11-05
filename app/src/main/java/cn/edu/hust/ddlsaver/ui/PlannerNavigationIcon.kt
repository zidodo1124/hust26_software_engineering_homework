package cn.edu.hust.ddlsaver.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Code-drawn icons keep the four destinations visually consistent without additional assets. */
@Composable
internal fun PlannerNavigationIcon(view: String) {
    val color = LocalContentColor.current
    Canvas(Modifier.size(22.dp)) {
        val unit = size.width / 24f
        val stroke = Stroke(1.8f * unit)
        drawRoundRect(color, Offset(2 * unit, 3 * unit), Size(20 * unit, 18 * unit), CornerRadius(3 * unit), style = stroke)
        drawLine(color, Offset(2 * unit, 8 * unit), Offset(22 * unit, 8 * unit), 1.8f * unit)
        when (view) {
            "month" -> for (row in 0..1) for (col in 0..2) drawCircle(color, 1.2f * unit, Offset((7 + col * 5) * unit, (12 + row * 5) * unit))
            "week" -> for (col in 0..2) drawRoundRect(color, Offset((5 + col * 5) * unit, 11 * unit), Size(3 * unit, 7 * unit), CornerRadius(unit))
            "day" -> drawRoundRect(color, Offset(6 * unit, 11 * unit), Size(12 * unit, 7 * unit), CornerRadius(unit))
            else -> for (row in 0..1) drawLine(color, Offset(6 * unit, (12 + row * 5) * unit), Offset(18 * unit, (12 + row * 5) * unit), 1.8f * unit)
        }
    }
}
