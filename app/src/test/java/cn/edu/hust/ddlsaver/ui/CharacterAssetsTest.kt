package cn.edu.hust.ddlsaver.ui

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CharacterAssetsTest {
    @Test fun chibiAtlasAndLicenseFilesMatchPinnedOriginalBytes() {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        val manifest = JSONObject(assets.open("character/chibi/sources.json").bufferedReader().use { it.readText() })
        assertEquals("3e7df1d76014e4f43ba9ed3c9de53c2a62ba0d14", manifest.getString("commit"))
        val files = manifest.getJSONArray("files")
        val verifiedNames = mutableSetOf<String>()
        for (i in 0 until files.length()) {
            val entry = files.getJSONObject(i)
            val name = entry.getString("file")
            val bytes = assets.open("character/chibi/$name").use { it.readBytes() }
            assertEquals(name, entry.getLong("bytes"), bytes.size.toLong())
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02X".format(it.toInt() and 0xff) }
            assertEquals(name, entry.getString("sha256"), hash)
            verifiedNames += name
            if (name == "spritesheet.webp") {
                val decoded = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                try {
                    assertEquals(entry.getInt("width"), decoded.width)
                    assertEquals(entry.getInt("height"), decoded.height)
                    assertEquals(entry.getBoolean("hasAlpha"), decoded.hasAlpha())
                } finally { decoded.recycle() }
            }
        }
        assertEquals(setOf("spritesheet.webp", "upstream-atlas-source.json", "upstream-LICENSE.txt", "origin-LICENSE.txt"), verifiedNames)
    }

    @Test fun bundledSourcesMatchPinnedHashesAndAllFourExpressionsDecodeOffline() {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        val manifest = JSONObject(assets.open("character/sources.json").bufferedReader().use { it.readText() })
        assertEquals("046b848f244450e6e636335766276bf3343fe84b", manifest.getString("commit"))
        val files = manifest.getJSONArray("files")
        var imageCount = 0
        for (i in 0 until files.length()) {
            val entry = files.getJSONObject(i)
            val bytes = assets.open("character/${entry.getString("file")}").use { it.readBytes() }
            assertEquals(entry.getLong("bytes"), bytes.size.toLong())
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it.toInt() and 0xff) }
            assertEquals(entry.getString("sha256"), hash)
            if (entry.getString("file").endsWith(".webp")) {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                assertNotNull(entry.getString("file"), bitmap)
                val decoded = requireNotNull(bitmap)
                assertTrue(decoded.width > 0 && decoded.height > 0)
                decoded.recycle()
                imageCount++
            }
        }
        assertEquals(4, imageCount)
    }
}
