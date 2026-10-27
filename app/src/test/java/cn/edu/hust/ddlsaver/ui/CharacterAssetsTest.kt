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
