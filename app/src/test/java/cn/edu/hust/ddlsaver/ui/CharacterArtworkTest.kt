package cn.edu.hust.ddlsaver.ui

import android.content.Context
import android.graphics.BitmapFactory
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
    @Test fun everyStateAndStyleDecodesAndSelectedSpritePixelsMatchOriginalSheet() {
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
                        assertArrayEquals("Every region pixel must come from the selected source frame", expectedPixels, pixels)
                        chibiFrames += pixels.contentHashCode()
                    }
                } finally { bitmap.recycle() }
            }
            assertEquals("Normal, happy, urging and thoughtful frames must differ", 4, chibiFrames.size)
        } finally { original.recycle() }
    }

    @Test fun unreadableArtworkReturnsNullSoTextualStatusCanRemainAvailable() {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        assertNull(decodeCharacterArtwork(assets, CharacterArtworkSpec("character/missing.webp")))
        assertNull(decodeCharacterArtwork(assets, CharacterArtworkSpec("character/chibi/spritesheet.webp", 100, 100)))
        assertNull(decodeCharacterArtwork(assets, CharacterArtworkSpec("character/chibi/spritesheet.webp", -1, 0)))
        assertNull(decodeCharacterArtwork(assets, CharacterArtworkSpec("character/maid-left.webp", 0, 0)))
    }
}
