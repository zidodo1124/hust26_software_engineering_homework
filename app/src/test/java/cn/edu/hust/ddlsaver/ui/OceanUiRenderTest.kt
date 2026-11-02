package cn.edu.hust.ddlsaver.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import cn.edu.hust.ddlsaver.domain.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File
import java.time.Instant

/** Real production Compose screens, using only synthetic events; output is local review evidence. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class OceanUiRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val now = Instant.parse("2026-10-28T04:00:00Z")

    @Test fun bothCharacterStylesRenderOnTheActualHomeScreen() {
        val style = mutableStateOf(CharacterStyle.CLASSIC)
        compose.setContent {
            SaverTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen(sampleState(style.value), 0, {}, {}, { _, _ -> })
                }
            }
        }
        awaitCharacter(CharacterStyle.CLASSIC)
        compose.onNodeWithText("蓝色大肥鱼").assertExists()
        compose.onNodeWithTag("character-status").assertTextEquals("该冲刺了")
        compose.onNodeWithTag("character-bubble").assertExists()
        compose.onNodeWithTag("character-bubble-tail-start").assertExists()
        saveScreenshot("home-classic.png")
        compose.runOnIdle { style.value = CharacterStyle.CHIBI }
        awaitCharacter(CharacterStyle.CHIBI)
        compose.onNodeWithText("蓝色大肥鱼").assertExists()
        compose.onNodeWithTag("character-status").assertTextEquals("该冲刺了")
        compose.onNodeWithTag("character-bubble").assertExists()
        compose.onNodeWithTag("character-bubble-tail-start").assertExists()
        saveScreenshot("home-chibi.png")
    }

    @Test
    @Config(qualifiers = "w320dp-h1000dp-mdpi")
    fun narrowScreenWithLargeTextKeepsStatisticsReadableAndBothFiltersWork() {
        compose.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = 1.5f)) {
                SaverTheme {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        HomeScreen(sampleState(CharacterStyle.CLASSIC), 0, {}, {}, { _, _ -> })
                    }
                }
            }
        }
        awaitCharacter(CharacterStyle.CLASSIC)
        saveScreenshot("home-320dp-font150.png")
        assertFullText("今日 DDL")
        assertFullText("速通危险期")
        val today = compose.onNodeWithTag("today-count").fetchSemanticsNode().boundsInRoot
        val danger = compose.onNodeWithTag("danger-count").fetchSemanticsNode().boundsInRoot
        assertTrue("Large text statistics should stack with no overlap", today.bottom <= danger.top)
        assertTrue(today.width > 200f && danger.width > 200f)

        compose.onNodeWithTag("today-count").performClick()
        compose.onNodeWithTag("event-list").performScrollToNode(hasTestTag("list-summary"))
        compose.onNodeWithTag("list-summary").assertTextEquals("今日待办 · 1")
        compose.onNodeWithTag("event-list").performScrollToIndex(0)
        compose.onNodeWithTag("danger-count").performClick()
        compose.onNodeWithTag("event-list").performScrollToNode(hasTestTag("list-summary"))
        compose.onNodeWithTag("list-summary").assertTextEquals("危险期待办 · 1")
    }

    private fun assertFullText(text: String) {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
        assertEquals("Expected actual layout for $text", 1, results.size)
        val layout = results.single()
        println("TEXT-LAYOUT $text size=${layout.size} constraints=${layout.layoutInput.constraints} " +
            "paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height} lines=${layout.lineCount} " +
            "overflowWidth=${layout.didOverflowWidth} overflowHeight=${layout.didOverflowHeight}")
        // BasicText can retain a max-width paragraph (208px) inside its tighter measured
        // text bounds (79px). That blank paragraph area makes hasVisualOverflow report true
        // even when every glyph is visible. Check the actual laid-out lines and full text.
        assertTrue("Text must have a rendered line: $text", layout.lineCount > 0)
        assertEquals("Every input character must be laid out: $text", text.length,
            layout.getLineEnd(layout.lineCount - 1, visibleEnd = false))
        for (line in 0 until layout.lineCount) {
            val left = layout.getLineLeft(line)
            val right = layout.getLineRight(line)
            val top = layout.getLineTop(line)
            val bottom = layout.getLineBottom(line)
            println("TEXT-LINE $text [$line] left=$left right=$right top=$top bottom=$bottom")
            assertFalse("Text must not be ellipsized: $text", layout.isLineEllipsized(line))
            assertTrue("Text must fit horizontally: $text ($left..$right, width=${layout.size.width})",
                left >= 0f && right <= layout.size.width)
            assertTrue("Text must fit vertically: $text ($top..$bottom, height=${layout.size.height})",
                top >= 0f && bottom <= layout.size.height)
        }
    }

    private fun awaitCharacter(style: CharacterStyle) {
        compose.onNodeWithTag("event-list").performScrollToNode(hasTestTag("character-panel"))
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodes(hasTestTag("character-image") and
                hasContentDescription("蓝色大肥鱼 · ${style.label} · 着急催促的表情")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("event-list").performScrollToIndex(0)
        compose.waitForIdle()
    }

    private fun saveScreenshot(name: String) {
        val output = File("build/outputs/ui-review", name)
        requireNotNull(output.parentFile).mkdirs()
        // PixelCopy requires a real display/vsync. Draw the same laid-out host view directly
        // through Android's native canvas so local Robolectric evidence uses actual UI code.
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            assertTrue("The rendered host must have a measured size", view.width > 0 && view.height > 0)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                view.draw(Canvas(bitmap))
                output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally {
                bitmap.recycle()
            }
        }
    }

    private fun sampleState(style: CharacterStyle): SaverUiState {
        fun event(id: String, title: String, seconds: Long, sprint: Long?) = DdlEvent(
            id = id, title = title, deadlineAt = now.plusSeconds(seconds).toEpochMilli(), sprintMinutes = sprint,
            description = "界面测试样本：请检查提交内容与附件。", importance = Importance.NORMAL,
            completedAt = null, archivedAt = null, createdAt = now.toEpochMilli(), updatedAt = now.toEpochMilli(), revision = 1,
            source = Category("course", "课程作业"), tags = listOf(Category("team", "小组协作")),
        )
        return SaverUiState(loading = false, now = now, settings = AppSettings(characterStyle = style), events = listOf(
            event("ui-danger", "软件工程阶段报告", 1800, 60),
            event("ui-normal", "整理课程资料", 172800, null),
        ))
    }
}
