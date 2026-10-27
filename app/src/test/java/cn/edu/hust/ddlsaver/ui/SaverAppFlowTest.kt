package cn.edu.hust.ddlsaver.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.ViewModelProvider
import cn.edu.hust.ddlsaver.MainActivity
import cn.edu.hust.ddlsaver.data.RoomEventRepository
import cn.edu.hust.ddlsaver.data.SaverDatabase
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.EventDraft
import cn.edu.hust.ddlsaver.domain.CustomField
import cn.edu.hust.ddlsaver.domain.ExpiredPolicy
import cn.edu.hust.ddlsaver.domain.UrgencyRules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.SQLiteMode
import org.robolectric.shadows.ShadowDialog
import java.time.Instant
import java.time.YearMonth
import java.io.File

/** Runs the real Activity, Compose screens and Room repository in an isolated local Android runtime. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class SaverAppFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val databaseIsolation = object : ExternalResource() {
        override fun before() = removeTestDatabase()
        override fun after() = removeTestDatabase()

        private fun removeTestDatabase() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase("ddl-saver.db")
            File(context.filesDir, "attachments").listFiles().orEmpty().forEach { it.delete() }
            File(context.cacheDir, "ui-attachment-source.png").delete()
        }
    }

    private val failureDiagnostics = object : TestWatcher() {
        override fun failed(error: Throwable, description: Description) {
            // Never ask Espresso/Compose to become idle again while reporting an idle failure.
            println("UI-DIAGNOSTIC FAILED ${description.methodName}")
            println(error.stackTraceToString())
        }
    }

    // Reset only this test runtime's database and attachment fixtures, outside Activity lifetime.
    @get:Rule
    val rules: TestRule = RuleChain.outerRule(databaseIsolation).around(compose).around(failureDiagnostics)

    @Test
    fun requiredInputIsRejectedThenNewEventCanBeSavedAndEdited() {
        openNewEvent()
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        compose.onNodeWithText("请填写事项标题。").assertExists()
        compose.onNodeWithText("请选择完整的截止日期和时间。").assertExists()
        compose.onNodeWithText("事项详情").assertDoesNotExist()
        assertTrue(storedEvents().isEmpty())

        compose.onNodeWithTag("event-title").performScrollTo().performTextInput("创建后编辑的事项")
        selectDeadline()
        compose.onNodeWithTag("event-description").performScrollTo().performTextInput("第一次填写的描述")
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        awaitText("事项详情")
        compose.onNodeWithText("创建后编辑的事项").assertExists()
        compose.onNodeWithText("2030年1月2日 18:00").assertExists()
        val createdId = storedEvents().single().id

        compose.onNodeWithText("编辑事项").performScrollTo().performClick()
        compose.onNodeWithTag("event-title").performScrollTo().performTextReplacement("编辑后的标题")
        compose.onNodeWithTag("event-description").performScrollTo().performTextReplacement("修改后的完整描述")
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        awaitText("事项详情")
        awaitText("编辑后的标题")
        compose.onNodeWithText("修改后的完整描述").assertExists()
        val edited = storedEvents().single()
        assertEquals(createdId, edited.id)
        assertEquals("编辑后的标题", edited.title)
        assertEquals("修改后的完整描述", edited.description)
        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithTag("event-list").performScrollToNode(hasText("编辑后的标题"))
        compose.onNodeWithText("编辑后的标题").assertExists()
        compose.onNodeWithText("创建后编辑的事项").assertDoesNotExist()
    }

    @Test
    fun completedItemCanBeRecoveredFromHistoryAndDeleteCancellationPreservesIt() {
        createEvent("完成恢复与删除取消")
        val createdId = storedEvents().single().id
        compose.onNodeWithText("标记完成").performScrollTo().performClick()
        awaitText("恢复为未完成")
        val completed = storedEvents().single()
        assertEquals(createdId, completed.id)
        assertNotNull(completed.completedAt)
        assertFalse(UrgencyRules.isActive(completed))
        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithText("完成恢复与删除取消").assertDoesNotExist()

        compose.onNodeWithText("已完成").performClick()
        compose.onNodeWithTag("event-list").performScrollToNode(hasText("完成恢复与删除取消"))
        compose.onNodeWithText("完成恢复与删除取消").performClick()
        awaitText("事项详情")
        compose.onNodeWithText("恢复为未完成").performScrollTo().performClick()
        awaitText("标记完成")
        val restored = storedEvents().single()
        assertEquals(createdId, restored.id)
        assertNull(restored.completedAt)
        assertTrue(UrgencyRules.isActive(restored))
        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithText("待办").performClick()
        compose.onNodeWithTag("event-list").performScrollToNode(hasText("完成恢复与删除取消"))
        compose.onNodeWithText("完成恢复与删除取消").performClick()
        awaitText("事项详情")

        compose.onNodeWithText("永久删除").performScrollTo().performClick()
        compose.onNodeWithText("永久删除这件事项？").assertExists()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("永久删除这件事项？").assertDoesNotExist()
        assertEquals(restored, storedEvents().single())
        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithTag("event-list").performScrollToNode(hasText("完成恢复与删除取消"))
        compose.onNodeWithText("完成恢复与删除取消").assertExists()
    }

    @Test
    fun activityRecreationKeepsUnsavedDraftAndDiscardRequiresConfirmation() {
        openNewEvent()
        compose.onNodeWithTag("event-title").performTextInput("尚未保存的标题")
        compose.onNodeWithTag("event-description").performScrollTo().performTextInput("旋转后仍需保留的描述")
        compose.onNodeWithTag("field-add").performScrollTo().performClick()
        compose.onNodeWithTag("field-label-0").performScrollTo().performTextInput("地点")
        compose.onNodeWithTag("field-value-0").performScrollTo().performTextInput("东九楼")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("event-title").performScrollTo().assertTextContains("尚未保存的标题")
        compose.onNodeWithTag("event-description").performScrollTo().assertTextContains("旋转后仍需保留的描述")
        compose.onNodeWithTag("field-label-0").performScrollTo().assertTextContains("地点")
        compose.onNodeWithTag("field-value-0").performScrollTo().assertTextContains("东九楼")

        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithText("放弃尚未保存的修改？").assertExists()
        compose.onNodeWithText("继续编辑").performClick()
        compose.onNodeWithTag("event-title").performScrollTo().assertTextContains("尚未保存的标题")
        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithText("放弃修改").performClick()
        awaitText("DDL Saver")
        compose.onNodeWithText("尚未保存的标题").assertDoesNotExist()
        assertTrue(storedEvents().isEmpty())
    }

    @Test
    fun categoryManagementAndCustomFieldsPersistThenFilterTogether() {
        awaitText("DDL Saver")
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithTag("manage-categories").performScrollTo().performClick()
        compose.onNodeWithTag("catalog-add").performClick()
        compose.onNodeWithTag("catalog-name").performScrollTo().performTextInput("软件工程")
        compose.onNodeWithTag("catalog-save").performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("catalog-name").fetchSemanticsNodes().isEmpty() }
        awaitText("软件工程")
        val source = withRepository { sources.first().single() }
        compose.onNodeWithTag("catalog-tags").performClick()
        compose.onNodeWithTag("catalog-add").performClick()
        compose.onNodeWithTag("catalog-name").performScrollTo().performTextInput("实验")
        compose.onNodeWithTag("catalog-save").performScrollTo().performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("catalog-name").fetchSemanticsNodes().isEmpty() }
        awaitText("实验")
        val tag = withRepository { tags.first().single() }
        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithText("‹ 返回").performClick()
        openNewEvent()
        compose.onNodeWithTag("event-title").performTextInput("带分类的实验")
        selectDeadline()
        compose.onNodeWithTag("editor-source-options").performScrollTo()
        compose.onNodeWithTag("editor-source-${source.id}").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("editor-tag-options").performScrollTo()
        compose.onNodeWithTag("editor-tag-${tag.id}").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("field-add").performScrollTo().performClick()
        compose.onNodeWithTag("field-label-0").performScrollTo().performTextInput("地点")
        compose.onNodeWithTag("field-value-0").performScrollTo().performTextInput("东九楼")
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        awaitText("事项详情")
        val first = storedEvents().single()
        assertEquals(source.id, first.source?.id)
        assertEquals(listOf(tag.id), first.tags.map { it.id })
        assertEquals("东九楼", first.customFields.single().value)
        val other = withRepository {
            save(EventDraft(title = "另一个事项", deadlineAt = first.deadlineAt, sourceId = source.id,
                customFields = listOf(CustomField("independent", "地点", "西十二楼"))))
        }
        reloadModel()
        compose.onNodeWithText("编辑事项").performScrollTo().performClick()
        compose.onNodeWithTag("field-value-0").performScrollTo().performTextReplacement("东九楼 401")
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        awaitText("事项详情")
        val saved = storedEvents().associateBy { it.id }
        assertEquals("东九楼 401", saved.getValue(first.id).customFields.single().value)
        assertEquals(other, saved.getValue(other.id))
        compose.onNodeWithText("‹ 返回").performClick()
        scrollHomeTag("toggle-filters")
        compose.onNodeWithTag("toggle-filters").performClick()
        scrollHomeTag("filter-source-${source.id}")
        compose.onNodeWithTag("filter-source-${source.id}").performClick()
        scrollHomeTag("filter-tag-${tag.id}")
        compose.onNodeWithTag("filter-tag-${tag.id}").performClick()
        scrollHomeTag("event-search")
        compose.onNodeWithTag("event-search").performTextInput("东九楼 401")
        scrollHomeTag("list-summary")
        compose.onNodeWithTag("list-summary").assertTextEquals("全部待办 · 1")
        compose.onNodeWithTag("event-list").performScrollToNode(hasText("带分类的实验"))
        compose.onNodeWithText("带分类的实验").assertExists()
        assertEquals(2, storedEvents().size)
    }

    @Test
    fun calendarCrossesMonthAndTodayShortcutClearsDateFilter() {
        awaitText("DDL Saver")
        val today = Instant.now().atZone(BeijingZone).toLocalDate()
        val nextDate = YearMonth.from(today).plusMonths(1).atDay(15)
        val next = withRepository {
            save(EventDraft(title = "当天事项", deadlineAt = today.atTime(23, 59).atZone(BeijingZone).toInstant().toEpochMilli()))
            save(EventDraft(title = "跨月事项", deadlineAt = nextDate.atTime(18, 0).atZone(BeijingZone).toInstant().toEpochMilli()))
        }
        reloadModel()
        compose.onNodeWithTag("today-count").assertTextContains("1")
        scrollHomeTag("toggle-calendar")
        compose.onNodeWithTag("toggle-calendar").performClick()
        scrollHomeTag("calendar-next")
        compose.onNodeWithTag("calendar-next").performClick()
        scrollHomeTag("calendar-day-$nextDate")
        compose.onNodeWithTag("calendar-day-$nextDate").performClick()
        scrollHomeTag("list-summary")
        compose.onNodeWithTag("list-summary").assertTextContains("$nextDate 的事项 · 1")
        compose.onNodeWithTag("event-list").performScrollToNode(hasText(next.title))
        compose.onNodeWithText(next.title).assertExists()
        scrollHomeTag("today-count")
        compose.onNodeWithTag("today-count").assertTextContains("1").performClick()
        scrollHomeTag("list-summary")
        compose.onNodeWithTag("list-summary").assertTextContains("今日待办 · 1")
        assertEquals(2, storedEvents().size)
    }

    @Test
    fun batchDeleteCancellationPreservesAllAndConfirmationOnlyDeletesSelected() {
        awaitText("DDL Saver")
        val seeded = withRepository {
            (1..3).map { save(EventDraft(title = "批量事项 $it", deadlineAt = Instant.now().plusSeconds(86400L * it).toEpochMilli())) }
        }
        reloadModel()
        scrollHomeTag("toggle-batch")
        compose.onNodeWithTag("toggle-batch").performClick()
        seeded.take(2).forEach {
            scrollHomeTag("select-${it.id}")
            compose.onNodeWithTag("select-${it.id}").performClick()
        }
        scrollHomeTag("batch-delete")
        compose.onNodeWithTag("batch-delete").performClick()
        compose.onNodeWithText("永久删除选中的 2 件事项？").assertExists()
        compose.onNodeWithText("取消").performClick()
        assertEquals(seeded.toSet(), storedEvents().toSet())
        compose.onNodeWithTag("batch-delete").performClick()
        compose.onNodeWithTag("batch-confirm-delete").performClick()
        compose.waitUntil(20_000) { storedEvents().size == 1 }
        assertEquals(seeded.last(), storedEvents().single())
    }

    @Test
    fun automaticAndManualExpiredArchiveKeepCompletionFlags() {
        awaitText("DDL Saver")
        val seeded = withRepository {
            val expired = save(EventDraft(title = "过期待办", deadlineAt = Instant.now().minusSeconds(7200).toEpochMilli()))
            val completed = save(EventDraft(title = "过期已完成", deadlineAt = expired.deadlineAt))
            setCompleted(completed.id, true)
            val future = save(EventDraft(title = "未来事项", deadlineAt = Instant.now().plusSeconds(86400).toEpochMilli()))
            Triple(expired.id, completed.id, future.id)
        }
        reloadModel()
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithTag("policy-auto").performScrollTo().performClick()
        compose.onNodeWithText("保存设置").performScrollTo().performClick()
        awaitText("DDL Saver")
        val automatic = storedEvents().associateBy { it.id }
        assertNotNull(automatic.getValue(seeded.first).archivedAt)
        assertNull(automatic.getValue(seeded.second).archivedAt)
        assertNotNull(automatic.getValue(seeded.second).completedAt)
        assertNull(automatic.getValue(seeded.third).archivedAt)
        assertEquals(ExpiredPolicy.AUTO_ARCHIVE, withRepository { settings.first().expiredPolicy })
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithTag("archive-expired").performScrollTo().performClick()
        compose.onNodeWithText("确认归档").performClick()
        compose.waitUntil(20_000) { storedEvents().first { it.id == seeded.second }.archivedAt != null }
        val manual = storedEvents().associateBy { it.id }
        assertEquals(automatic.getValue(seeded.second).completedAt, manual.getValue(seeded.second).completedAt)
        assertEquals(automatic.getValue(seeded.third), manual.getValue(seeded.third))
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun imageDraftSurvivesRotationAndCancel() {
        openNewEvent()
        val original = pasteTestImage()
        awaitImportedImage()
        val imported = privateImageFiles().single()
        assertTrue(imported.exists())
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("图片附件 · 1 张").assertExists()
        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithText("放弃修改").performClick()
        awaitText("DDL Saver")
        compose.waitUntil(20_000) { !imported.exists() }
        assertTrue(storedEvents().isEmpty())
        assertTrue(original.exists())
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun imageRemovalCommitsOnlyAfterSave() {
        openNewEvent()
        compose.onNodeWithTag("event-title").performTextInput("图片生命周期")
        selectDeadline()
        val original = pasteTestImage()
        awaitImportedImage()
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        awaitText("事项详情")
        val saved = storedEvents().single()
        val ref = saved.attachments.single()
        val privateCopy = privateImageFiles().single()
        assertTrue(privateCopy.exists())
        compose.onNodeWithText("编辑事项").performScrollTo().performClick()
        compose.onNodeWithTag("attachment-editor").performScrollTo()
        compose.onNodeWithTag("attachment-remove-${ref.id}").performScrollTo().performClick()
        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithText("放弃修改").performClick()
        awaitText("事项详情")
        assertEquals(saved, storedEvents().single())
        assertTrue(privateCopy.exists())
        compose.onNodeWithText("编辑事项").performScrollTo().performClick()
        compose.onNodeWithTag("attachment-editor").performScrollTo()
        compose.onNodeWithTag("attachment-remove-${ref.id}").performScrollTo().performClick()
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        awaitText("事项详情")
        assertTrue(storedEvents().single().attachments.isEmpty())
        compose.waitUntil(20_000) { !privateCopy.exists() }
        assertTrue(original.exists())
    }

    private fun pasteTestImage(): File {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File(context.cacheDir, "ui-attachment-source.png")
        val bitmap = Bitmap.createBitmap(24, 16, Bitmap.Config.ARGB_8888)
        source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        compose.runOnIdle {
            // Populate the exact Activity service read by the paste button; Robolectric services
            // can have separate shadows for Application and Activity contexts.
            (compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newRawUri("测试图片", Uri.fromFile(source)))
        }
        compose.onNodeWithTag("attachment-paste").performScrollTo().performClick()
        return source
    }

    private fun awaitImportedImage() {
        val model = ViewModelProvider(compose.activity)[SaverViewModel::class.java]
        try {
            compose.waitUntil(20_000) {
                model.state.value.operationError != null ||
                    compose.onAllNodesWithText("图片附件 · 1 张").fetchSemanticsNodes().isNotEmpty()
            }
        } catch (failure: Throwable) {
            throw AssertionError("Image import did not reach the editor. VM snapshot: ${model.state.value}", failure)
        }
        assertNull("Image import failed: ${model.state.value.operationError}", model.state.value.operationError)
        compose.onNodeWithText("图片附件 · 1 张").assertExists()
    }

    private fun privateImageFiles(): List<File> = File(ApplicationProvider.getApplicationContext<Context>().filesDir,
        "attachments").listFiles().orEmpty().filter { it.extension == "img" }

    private fun scrollHomeTag(tag: String) {
        compose.onNodeWithTag("event-list").performScrollToNode(hasTestTag(tag))
    }

    private fun reloadModel() {
        compose.runOnIdle { ViewModelProvider(compose.activity)[SaverViewModel::class.java].reload() }
        compose.waitUntil(20_000) { !ViewModelProvider(compose.activity)[SaverViewModel::class.java].state.value.loading }
        compose.waitForIdle()
    }

    private fun <T> withRepository(action: suspend RoomEventRepository.() -> T): T = runBlocking {
        withTimeout(10_000) {
            val database = SaverDatabase.create(ApplicationProvider.getApplicationContext())
            try { RoomEventRepository(database).action() } finally { database.close() }
        }
    }

    private fun openNewEvent() {
        awaitText("DDL Saver")
        compose.onNodeWithTag("create-event").performClick()
        compose.onNodeWithTag("event-title").assertExists()
    }

    private fun createEvent(title: String) {
        openNewEvent()
        compose.onNodeWithTag("event-title").performTextInput(title)
        selectDeadline()
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        awaitText("事项详情")
        awaitText(title)
    }

    private fun selectDeadline() {
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

    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Query the actual database instead of treating an offscreen LazyColumn row as absent. */
    private fun storedEvents(): List<DdlEvent> = runBlocking {
        withTimeout(10_000) {
            val database = SaverDatabase.create(ApplicationProvider.getApplicationContext())
            try {
                RoomEventRepository(database).events.first()
            } finally {
                database.close()
            }
        }
    }

}
