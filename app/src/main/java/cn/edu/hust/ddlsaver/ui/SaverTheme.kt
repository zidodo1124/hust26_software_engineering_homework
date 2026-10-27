package cn.edu.hust.ddlsaver.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Ink = Color(0xFF182B48)
internal val Muted = Color(0xFF5D6D80)
internal val Ocean = Color(0xFF155E75)
internal val Paper = Color(0xFFF3F6FA)

private val SaverColors = lightColorScheme(
    primary = Ocean,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EEF4),
    onPrimaryContainer = Color(0xFF133F50),
    secondary = Color(0xFF466177),
    secondaryContainer = Color(0xFFE0EAF3),
    onSecondaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEAF0F6),
    onSurfaceVariant = Muted,
    outline = Color(0xFF79879A),
    outlineVariant = Color(0xFFD6DFE9),
    error = Color(0xFFAA2935),
    errorContainer = Color(0xFFFCE6E8),
)

@Composable
fun SaverTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SaverColors, typography = Typography(), content = content)
}
