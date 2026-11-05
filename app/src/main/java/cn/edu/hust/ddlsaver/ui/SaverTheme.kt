package cn.edu.hust.ddlsaver.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Ink = Color(0xFF173653)
internal val Muted = Color(0xFF52687E)
internal val Ocean = Color(0xFF365DB8)
internal val Paper = Color(0xFFF5F7FB)
internal val OceanMist = Color(0xFFE4EFFF)
internal val OceanBorder = Color(0xFFC8D9F1)

// Keep text and interactive controls dark enough against the light ocean surfaces.
internal val SaverColors = lightColorScheme(
    primary = Ocean,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Ink,
    secondary = Color(0xFF52688B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4EDFA),
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF66538B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0E6FF),
    onTertiaryContainer = Color(0xFF35244E),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEBF1FA),
    onSurfaceVariant = Muted,
    surfaceTint = Ocean,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDDE7F5),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F7FE),
    surfaceContainer = Color(0xFFEDF3FC),
    surfaceContainerHigh = Color(0xFFE7EFFA),
    surfaceContainerHighest = Color(0xFFE0EAF7),
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    inversePrimary = Color(0xFFB7CEFF),
    outline = Color(0xFF71849B),
    outlineVariant = OceanBorder,
    error = Color(0xFFAA2935),
    onError = Color.White,
    errorContainer = Color(0xFFFCE6E8),
    onErrorContainer = Color(0xFF6F1723),
)

@Composable
fun SaverTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SaverColors, shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(24.dp)),
        typography = Typography(
            headlineSmall = TextStyle(fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 19.sp),
            labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
            labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
            labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
        ), content = content)
}
