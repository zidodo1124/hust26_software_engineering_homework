package cn.edu.hust.ddlsaver.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import cn.edu.hust.ddlsaver.MainActivity
import cn.edu.hust.ddlsaver.data.RoomEventRepository
import cn.edu.hust.ddlsaver.data.SaverDatabase
import cn.edu.hust.ddlsaver.domain.AttachmentRef
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.platform.AttachmentStore
import cn.edu.hust.ddlsaver.reminders.ReminderRuntime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.SQLiteMode
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

/** Real Activity, clipboard, decoder, private files and Room; no device URI grant claims. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class AttachmentClipboardFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val copies: File get() = File(context.filesDir, "attachments")
    private val sources: File get() = File(context.cacheDir, "clipboard-flow-sources")
    private val model: SaverViewModel get() = ViewModelProvider(compose.activity)[SaverViewModel::class.java]

    private val isolation = object : ExternalResource() {
        override fun before() {
            cleanTestFiles()
            assertTrue(sources.mkdirs())
        }

        override fun after() = cleanTestFiles()

        private fun cleanTestFiles() {
            ReminderRuntime.resetForTests()
            context.deleteDatabase("ddl-saver.db")
            copies.listFiles().orEmpty().forEach { it.delete() }
            sources.deleteRecursively()
        }
    }

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(isolation).around(compose)

    @Test
    fun successivePasteActionsAppendDifferentImagesAndPrivateCopiesSurviveSourceRemoval() {
        val first = png("first.png", 36, 24, 0xff3c79a8.toInt())
        val second = png("second.png", 17, 29, 0xffc64f71.toInt())
        val firstBytes = first.readBytes()
        val secondBytes = second.readBytes()
        beginEvent("连续粘贴两张图片")

        paste(Uri.fromFile(first))
        awaitAttachmentCount(1)
        val firstPrivateName = privateNames().single()
        assertArrayEquals(firstBytes, File(copies, firstPrivateName).readBytes())
        assertTrue("Importing a draft must not save the event", storedEvents().isEmpty())

        paste(Uri.fromFile(second))
        awaitAttachmentCount(2)
        assertEquals(2, privateNames().size)
        assertArrayEquals(firstBytes, File(copies, firstPrivateName).readBytes())
        saveEvent()
        val saved = storedEvents().single()
        val refs = saved.attachments
        assertEquals(listOf("first.png", "second.png"), refs.map { it.displayName })
        assertEquals(2, refs.map { it.id }.distinct().size)
        assertPrivateImage(refs[0], firstBytes, 36, 24)
        assertPrivateImage(refs[1], secondBytes, 17, 29)
        assertArrayEquals(firstBytes, first.readBytes())
        assertArrayEquals(secondBytes, second.readBytes())

        assertTrue(first.delete())
        assertTrue(second.delete())
        reopenEvent(saved.title)
        compose.onNodeWithTag("attachment-gallery").performScrollTo()
        compose.onNodeWithText("图片附件 · 2 张").assertExists()
        assertEquals(saved, storedEvents().single())
        assertPrivateImage(refs[0], firstBytes, 36, 24)
        assertPrivateImage(refs[1], secondBytes, 17, 29)
        assertEquals(refs.map { it.relativePath }.toSet(), privateNames())
    }

    @Test
    fun mixedPasteFailureKeepsDraftAndSavedAttachmentThenFreshClipboardCanBeRetried() {
        val originalSource = png("saved.png", 28, 20, 0xff648347.toInt())
        val originalBytes = originalSource.readBytes()
        beginEvent("图片失败重试")
        compose.onNodeWithTag("event-description").performScrollTo().performTextInput("已保存的描述")
        compose.onNodeWithTag("field-add").performScrollTo().performClick()
        compose.onNodeWithTag("field-label-0").performScrollTo().performTextInput("备注")
        compose.onNodeWithTag("field-value-0").performScrollTo().performTextInput("已保存的字段")
        paste(Uri.fromFile(originalSource))
        awaitAttachmentCount(1)
        saveEvent()
        val saved = storedEvents().single()
        val originalRef = saved.attachments.single()
        val originalPrivateNames = privateNames()

        compose.onNodeWithText("编辑事项").performScrollTo().performClick()
        compose.onNodeWithTag("event-title").performScrollTo().performTextReplacement("保留输入后重试")
        compose.onNodeWithTag("event-description").performScrollTo().performTextReplacement("尚未保存的描述\n第二行")
        compose.onNodeWithTag("field-value-0").performScrollTo().performTextReplacement("尚未保存的字段")
        val newSource = png("new.png", 19, 31, 0xff7349a2.toInt())
        val newBytes = newSource.readBytes()
        val deniedUri = Uri.parse("content://clipboard-flow-tests/unreadable-image")
        val deniedReads = AtomicInteger()
        val completedCopiesAtFailure = AtomicInteger()
        shadowOf(context.contentResolver).registerInputStream(deniedUri, object : InputStream() {
            override fun read(): Int = denied()
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = denied()

            private fun denied(): Nothing {
                deniedReads.incrementAndGet()
                completedCopiesAtFailure.set(copies.listFiles().orEmpty().count { it.extension == "img" })
                throw SecurityException("Controlled unreadable URI for clipboard flow test")
            }
        })

        paste(Uri.fromFile(newSource), deniedUri)
        compose.waitUntil(20_000) { !model.state.value.busy && model.state.value.operationError != null }
        val error = checkNotNull(model.state.value.operationError)
        assertTrue(error.contains("图片导入失败"))
        assertTrue(error.contains("来源访问权限不可用"))
        assertTrue(error.contains("重新复制图片"))
        assertTrue(error.contains("从相册选择"))
        compose.onNodeWithText(error).assertExists()
        assertTrue("The controlled URI must actually be read", deniedReads.get() > 0)
        assertEquals("The first image must have been imported before the second failed", 2, completedCopiesAtFailure.get())
        assertEquals("Rollback must remove both completed and temporary batch files", originalPrivateNames, privateNames())
        assertEquals(saved, storedEvents().single())
        assertPrivateImage(originalRef, originalBytes, 28, 20)
        compose.onNodeWithText("图片附件 · 1 张").assertExists()
        compose.onNodeWithTag("event-title").performScrollTo().assertTextContains("保留输入后重试")
        compose.onNodeWithTag("event-description").performScrollTo().assertTextContains("尚未保存的描述\n第二行")
        compose.onNodeWithTag("field-value-0").performScrollTo().assertTextContains("尚未保存的字段")

        // A new clip replaces the failed mixed batch; the next click must read it afresh.
        paste(Uri.fromFile(newSource))
        awaitAttachmentCount(2)
        compose.onNodeWithText(error).assertDoesNotExist()
        assertEquals("Retrying an import still must not save the draft", saved, storedEvents().single())
        saveEvent()
        val updated = storedEvents().single()
        assertEquals(saved.id, updated.id)
        assertEquals("保留输入后重试", updated.title)
        assertEquals("尚未保存的描述\n第二行", updated.description)
        assertEquals(saved.customFields.single().copy(value = "尚未保存的字段"), updated.customFields.single())
        assertEquals(originalRef, updated.attachments[0])
        assertEquals(listOf("saved.png", "new.png"), updated.attachments.map { it.displayName })
        assertPrivateImage(updated.attachments[0], originalBytes, 28, 20)
        assertPrivateImage(updated.attachments[1], newBytes, 19, 31)
        assertEquals(updated.attachments.map { it.relativePath }.toSet(), privateNames())
        assertArrayEquals(originalBytes, originalSource.readBytes())
        assertArrayEquals(newBytes, newSource.readBytes())
        reopenEvent(updated.title)
        assertEquals(updated, storedEvents().single())
        compose.onNodeWithTag("attachment-gallery").performScrollTo()
        compose.onNodeWithText("图片附件 · 2 张").assertExists()
    }

    private fun paste(vararg uris: Uri) {
        compose.runOnIdle {
            val clip = ClipData.newRawUri("合成图片", uris.first())
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            (compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
        }
        compose.onNodeWithTag("attachment-paste").performScrollTo().performClick()
    }

    private fun awaitAttachmentCount(count: Int) {
        val label = "图片附件 · $count 张"
        compose.waitUntil(20_000) {
            model.state.value.operationError != null ||
                (!model.state.value.busy && compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty())
        }
        assertNull("Image import failed", model.state.value.operationError)
        compose.onNodeWithText(label).assertExists()
    }

    private fun beginEvent(title: String) {
        awaitText("DDL Saver")
        compose.onNodeWithTag("create-event").performClick()
        compose.onNodeWithTag("event-title").performTextInput(title)
        compose.onNodeWithText("选择日期").performScrollTo().performClick()
        compose.runOnIdle {
            val dialog = ShadowDialog.getLatestDialog() as DatePickerDialog
            dialog.datePicker.updateDate(2030, 0, 2)
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        }
        compose.onNodeWithText("选择时间").performScrollTo().performClick()
        compose.runOnIdle {
            val dialog = ShadowDialog.getLatestDialog() as TimePickerDialog
            dialog.updateTime(18, 0)
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        }
    }

    private fun saveEvent() {
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        awaitText("事项详情")
    }

    private fun reopenEvent(title: String) {
        compose.onNodeWithText("‹ 返回").performClick()
        awaitText("DDL Saver")
        compose.onNodeWithTag("event-list").performScrollToNode(hasText(title))
        compose.onNodeWithText(title).performClick()
        awaitText("事项详情")
    }

    private fun awaitText(text: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun storedEvents(): List<DdlEvent> = runBlocking {
        withTimeout(10_000) {
            val database = SaverDatabase.create(context)
            try { RoomEventRepository(database).events.first() } finally { database.close() }
        }
    }

    private fun png(name: String, width: Int, height: Int, color: Int): File {
        val source = File(sources, name)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        try { source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
        finally { bitmap.recycle() }
        return source
    }

    private fun privateNames(): Set<String> = copies.listFiles().orEmpty().map { it.name }.toSet()

    private fun assertPrivateImage(ref: AttachmentRef, bytes: ByteArray, width: Int, height: Int) = runBlocking {
        assertEquals("image/png", ref.mimeType)
        assertEquals(width, ref.width)
        assertEquals(height, ref.height)
        assertEquals(bytes.size.toLong(), ref.byteSize)
        assertArrayEquals(bytes, File(copies, ref.relativePath).readBytes())
        val preview = AttachmentStore(context).loadPreview(ref)
        try {
            assertEquals(width, preview.width)
            assertEquals(height, preview.height)
        } finally { preview.recycle() }
        assertFalse(privateNames().any { it.endsWith(".part") })
    }
}
