package cn.edu.hust.ddlsaver.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import cn.edu.hust.ddlsaver.domain.CharacterState
import cn.edu.hust.ddlsaver.domain.CharacterStyle
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CharacterArtworkTest {
    @Test fun everyStateAndStyleDecodesAndRetainsEveryPixelOutsideReviewedExclusions() {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        val sheet = assets.open("character/chibi/spritesheet.webp").use { BitmapFactory.decodeStream(it) }
        assertNotNull(sheet)
        val original = requireNotNull(sheet)
        try {
            val chibiFrames = mutableSetOf<Int>()
            for (style in CharacterStyle.entries) for (state in CharacterState.entries) {
                val spec = characterArtwork(style, state)
                val decoded = decodeCharacterArtwork(assets, spec)
                assertNotNull("$style / $state must remain available offline", decoded)
                val bitmap = requireNotNull(decoded)
                try {
                    assertTrue(bitmap.width > 0 && bitmap.height > 0)
                    if (style == CharacterStyle.CHIBI) {
                        assertEquals(192, bitmap.width)
                        assertEquals(208, bitmap.height)
                        val pixels = IntArray(bitmap.width * bitmap.height)
                        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                        val expectedPixels = IntArray(bitmap.width * bitmap.height)
                        original.getPixels(expectedPixels, 0, bitmap.width,
                            requireNotNull(spec.column) * 192, requireNotNull(spec.row) * 208, bitmap.width, bitmap.height)
                        reviewedExclusions(state).forEach { rect ->
                            for (y in rect[1] until rect[3]) for (x in rect[0] until rect[2]) {
                                expectedPixels[y * 192 + x] = Color.TRANSPARENT
                            }
                        }
                        assertArrayEquals("Keep every source pixel outside the reviewed transparent regions", expectedPixels, pixels)
                        chibiFrames += pixels.contentHashCode()
                    }
                } finally { bitmap.recycle() }
            }
            assertEquals("Normal, happy, urging and thoughtful frames must differ", 4, chibiFrames.size)
        } finally { original.recycle() }
    }

    @Test fun knownNeighborFragmentsDisappearWithoutRemovingAdjacentHair() {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        val original = requireNotNull(assets.open("character/chibi/spritesheet.webp").use { BitmapFactory.decodeStream(it) })
        // These source coordinates reproduce the remnants seen on the phone, not just empty margins.
        data class Sample(val state: CharacterState, val row: Int, val debris: List<Pair<Int, Int>>, val hair: Pair<Int, Int>)
        val samples = listOf(
            Sample(CharacterState.COMPLETED, 4, listOf(5 to 195), 35 to 130),
            Sample(CharacterState.DANGER, 7, listOf(35 to 150), 46 to 146),
            Sample(CharacterState.OVERDUE, 8, listOf(24 to 135, 30 to 160), 42 to 125),
        )
        try {
            samples.forEach { sample ->
                val bitmap = requireNotNull(decodeCharacterArtwork(assets, characterArtwork(CharacterStyle.CHIBI, sample.state)))
                try {
                    sample.debris.forEach { (x, y) ->
                        assertTrue("The regression fixture must contain visible debris", Color.alpha(original.getPixel(192 + x, sample.row * 208 + y)) > 0)
                        assertEquals("The remnant must be transparent", Color.TRANSPARENT, bitmap.getPixel(x, y))
                    }
                    val (x, y) = sample.hair
                    val hair = original.getPixel(192 + x, sample.row * 208 + y)
                    assertTrue("The nearby hair sample must be visible", Color.alpha(hair) > 0)
                    assertEquals("Do not replace a remnant fix with a broad strip crop", hair, bitmap.getPixel(x, y))
                } finally { bitmap.recycle() }
            }
        } finally { original.recycle() }
    }

    @Test fun unreadableArtworkReturnsNullSoTextualStatusCanRemainAvailable() {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        assertNull(decodeCharacterArtwork(assets, CharacterArtworkSpec("character/missing.webp")))
        assertNull(decodeCharacterArtwork(assets, CharacterArtworkSpec("character/chibi/spritesheet.webp", 100, 100)))
        assertNull(decodeCharacterArtwork(assets, CharacterArtworkSpec("character/chibi/spritesheet.webp", -1, 0)))
        assertNull(decodeCharacterArtwork(assets, CharacterArtworkSpec("character/maid-left.webp", 0, 0)))
        assertNull(decodeCharacterArtwork(assets, CharacterArtworkSpec("character/chibi/spritesheet.webp", 1, null)))
        for (rect in listOf(ArtworkExcludedRect(-1, 0, 10, 10), ArtworkExcludedRect(0, 0, 193, 10),
            ArtworkExcludedRect(0, 200, 10, 209), ArtworkExcludedRect(5, 5, 5, 10))) {
            assertNull(decodeCharacterArtwork(assets, CharacterArtworkSpec("character/chibi/spritesheet.webp", 1, 8, listOf(rect))))
        }
        assertNull(decodeCharacterArtwork(assets, CharacterArtworkSpec("character/maid-left.webp",
            excludedRects = listOf(ArtworkExcludedRect(0, 0, 1, 1)))))
    }

    // Independent reviewed fixture: do not derive the expected image from production exclusion values.
    private fun reviewedExclusions(state: CharacterState): List<IntArray> = when (state) {
        CharacterState.NORMAL -> emptyList()
        CharacterState.COMPLETED -> listOf(intArrayOf(0, 184, 10, 208))
        CharacterState.DANGER -> listOf(
            intArrayOf(0, 124, 38, 128), intArrayOf(0, 128, 41, 140), intArrayOf(0, 140, 43, 144),
            intArrayOf(0, 144, 40, 148), intArrayOf(0, 148, 39, 156), intArrayOf(0, 156, 37, 164),
            intArrayOf(0, 164, 35, 208),
        )
        CharacterState.SOON, CharacterState.OVERDUE -> listOf(intArrayOf(0, 0, 30, 144), intArrayOf(0, 144, 45, 208))
    }
}
