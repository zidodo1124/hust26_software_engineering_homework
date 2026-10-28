package cn.edu.hust.ddlsaver.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import cn.edu.hust.ddlsaver.domain.Urgency
import org.junit.Assert.assertTrue
import org.junit.Test

/** The ocean palette must not make labels or deadline status harder to read. */
class OceanThemeContrastTest {
    @Test fun themeTextPairsAndUrgencyBadgesMeetNormalTextContrast() {
        val pairs = listOf(
            "primary button" to (SaverColors.onPrimary to SaverColors.primary),
            "primary container" to (SaverColors.onPrimaryContainer to SaverColors.primaryContainer),
            "secondary button" to (SaverColors.onSecondary to SaverColors.secondary),
            "secondary container" to (SaverColors.onSecondaryContainer to SaverColors.secondaryContainer),
            "tertiary button" to (SaverColors.onTertiary to SaverColors.tertiary),
            "tertiary container" to (SaverColors.onTertiaryContainer to SaverColors.tertiaryContainer),
            "background" to (SaverColors.onBackground to SaverColors.background),
            "card text" to (SaverColors.onSurface to SaverColors.surface),
            "card secondary text" to (Muted to SaverColors.surface),
            "variant text" to (SaverColors.onSurfaceVariant to SaverColors.surfaceVariant),
            "hero title" to (Ink to OceanMist),
            "hero date" to (Muted to OceanMist),
            "statistics" to (Ocean to SaverColors.surface),
            "danger statistics" to (SaverColors.error to SaverColors.surface),
            "error button" to (SaverColors.onError to SaverColors.error),
            "error notice" to (SaverColors.onErrorContainer to SaverColors.errorContainer),
        ) + Urgency.entries.map { "urgency $it" to urgencyColors(it) }
        pairs.forEach { (name, colors) ->
            val contrast = contrast(colors.first, colors.second)
            assertTrue("$name contrast was $contrast, expected at least 4.5:1", contrast >= 4.5)
        }
    }

    private fun contrast(first: Color, second: Color): Double {
        val a = first.luminance().toDouble()
        val b = second.luminance().toDouble()
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }
}
