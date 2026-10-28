package cn.edu.hust.ddlsaver.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import cn.edu.hust.ddlsaver.domain.CharacterFeedback
import cn.edu.hust.ddlsaver.domain.CharacterState
import cn.edu.hust.ddlsaver.domain.CharacterStyle
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class CharacterBubbleLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val message = "时间紧了，先完成必交内容。附件、格式和提交入口也检查一下，给上传留点时间。" +
        "遇到卡住的地方，可以先记下来，再处理下一步。完成后，记得核对实际提交结果。"
    private val feedback = CharacterFeedback(CharacterState.DANGER, "进入速通危险期", message,
        "character/whale-angry.webp", "着急催促的表情")

    @Test fun bothStylesKeepTheBubbleBesideTheArtworkAndItsTextInside() {
        val style = mutableStateOf(CharacterStyle.CLASSIC)
        compose.setContent {
            SaverTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                        CharacterPanel(feedback, style = style.value)
                    }
                }
            }
        }
        for (appearance in CharacterStyle.entries) {
            compose.runOnIdle { style.value = appearance }
            awaitArtwork(appearance)
            compose.onNodeWithTag("character-bubble-tail-start").assertExists()
            compose.onNodeWithTag("character-bubble-tail-top").assertDoesNotExist()
            val image = compose.onNodeWithTag("character-image").fetchSemanticsNode().boundsInRoot
            val bubble = compose.onNodeWithTag("character-bubble").fetchSemanticsNode().boundsInRoot
            assertTrue("The bubble must not cover the artwork", image.right < bubble.left)
            assertCompleteMessage()
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h1000dp-mdpi")
    fun largeTextStacksTheBubbleUnderEitherArtworkWithoutClippingLongDialogue() {
        val style = mutableStateOf(CharacterStyle.CLASSIC)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.5f)) {
                SaverTheme {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                            CharacterPanel(feedback, style = style.value)
                        }
                    }
                }
            }
        }
        for (appearance in CharacterStyle.entries) {
            compose.runOnIdle { style.value = appearance }
            awaitArtwork(appearance)
            compose.onNodeWithTag("character-bubble-tail-top").assertExists()
            compose.onNodeWithTag("character-bubble-tail-start").assertDoesNotExist()
            val image = compose.onNodeWithTag("character-image").fetchSemanticsNode().boundsInRoot
            val bubble = compose.onNodeWithTag("character-bubble").fetchSemanticsNode().boundsInRoot
            assertTrue("Stacked bubble must start below the artwork", image.bottom < bubble.top)
            assertCompleteMessage()
            saveScreenshot("bubble-long-text-${appearance.name.lowercase()}.png")
        }
    }

    @Test fun textAndCreditsRemainUsableWhenTheArtworkSlotIsEmpty() {
        var creditsOpened = false
        compose.setContent {
            SaverTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                        // This is the same layout used while loading or after the decoder returns null.
                        CharacterPanelLayout(feedback, onCredits = { creditsOpened = true }, artwork = {})
                    }
                }
            }
        }
        compose.onNodeWithTag("character-image").assertDoesNotExist()
        compose.onNodeWithTag("character-status").assertTextEquals(feedback.title)
        compose.onNodeWithTag("character-bubble").assertIsDisplayed()
        assertCompleteMessage()
        compose.onNodeWithTag("character-credits").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(creditsOpened) }
    }

    private fun awaitArtwork(style: CharacterStyle) {
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasTestTag("character-image") and
                hasContentDescription("蓝色大肥鱼 · ${style.label} · 着急催促的表情")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun assertCompleteMessage() {
        val text = compose.onNodeWithTag("character-line", useUnmergedTree = true)
        text.assertTextEquals(message)
        val layouts = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertTrue(layout.lineCount > 0)
        assertEquals(message.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = false))
        for (line in 0 until layout.lineCount) {
            assertFalse("Dialogue must not be ellipsized", layout.isLineEllipsized(line))
            assertTrue("Every line must fit the measured width", layout.getLineLeft(line) >= 0f &&
                layout.getLineRight(line) <= layout.size.width)
            assertTrue("Every line must fit the measured height", layout.getLineTop(line) >= 0f &&
                layout.getLineBottom(line) <= layout.size.height)
        }
        val bubble = compose.onNodeWithTag("character-bubble").fetchSemanticsNode().boundsInRoot
        val lineBounds = text.fetchSemanticsNode().boundsInRoot
        assertTrue(lineBounds.left >= bubble.left && lineBounds.right <= bubble.right)
        assertTrue(lineBounds.top >= bubble.top && lineBounds.bottom <= bubble.bottom)
    }

    private fun saveScreenshot(name: String) {
        val output = File("build/outputs/ui-review", name)
        requireNotNull(output.parentFile).mkdirs()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                view.draw(Canvas(bitmap))
                output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { bitmap.recycle() }
        }
    }
}
