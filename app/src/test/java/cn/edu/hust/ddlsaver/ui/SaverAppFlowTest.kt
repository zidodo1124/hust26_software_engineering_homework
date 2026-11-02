package cn.edu.hust.ddlsaver.ui

import android.Manifest
import android.provider.Settings
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
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
import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.ViewModelProvider
import cn.edu.hust.ddlsaver.MainActivity
import cn.edu.hust.ddlsaver.data.RoomEventRepository
import cn.edu.hust.ddlsaver.data.SaverDatabase
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.CharacterStyle
import cn.edu.hust.ddlsaver.reminders.ReminderRuntime
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
import org.junit.Before
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
import org.robolectric.Shadows.shadowOf
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
    @Volatile private var diagnosticModel: SaverViewModel? = null
    private val databaseIsolation = object : ExternalResource() {
        override fun before() = removeTestDatabase()
        override fun after() = removeTestDatabase()

        private fun removeTestDatabase() {
            ReminderRuntime.resetForTests()
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase("ddl-saver.db")
            File(context.filesDir, "attachments").listFiles().orEmpty().forEach { it.delete() }
            File(context.cacheDir, "ui-attachment-source.png").delete()
        }
    }

    private val failureDiagnostics = object : TestWatcher() {
        override fun failed(error: Throwable, description: Description) {
            // Never ask Espresso/Compose to become idle again while reporting an idle failure.
            println("UI-DIAGNOSTIC FAILED ${description.methodName} error_type=${error.javaClass.simpleName}")
            val snapshot = diagnosticModel?.state?.value
            if (snapshot == null) {
                println("UI-DIAGNOSTIC vm_cached=false")
            } else {
                // Only cached StateFlow data; no Activity, semantics or database access on failure.
                println("UI-DIAGNOSTIC vm_cached=true loading=${snapshot.loading} busy=${snapshot.busy} " +
                    "action_kind=${snapshot.actionResult?.kind} action_token=${snapshot.actionResult?.token} " +
                    "image_import_result_present=${snapshot.imageImportResult != null} " +
                    "character_style=${snapshot.settings.characterStyle} load_error_present=${snapshot.loadError != null} " +
                    "operation_error_present=${snapshot.operationError != null} cleanup_warning_present=${snapshot.cleanupWarning != null}")
            }
        }
    }

    // Reset only this test runtime's database and attachment fixtures, outside Activity lifetime.
    @get:Rule
    val rules: TestRule = RuleChain.outerRule(databaseIsolation).around(compose).around(failureDiagnostics)

    @Before
    fun cacheModelForFailureDiagnostics() {
        diagnosticModel = ViewModelProvider(compose.activity)[SaverViewModel::class.java]
    }

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
    fun staleEditShowsConflictAndKeepsDraftWithoutOverwritingNewerData() {
        createEvent("冲突前的事项")
        val original = storedEvents().single()
        compose.onNodeWithText("编辑事项").performScrollTo().performClick()
        compose.onNodeWithTag("event-title").performScrollTo().performTextReplacement("尚未保存的标题")
        compose.onNodeWithTag("event-description").performScrollTo().performTextInput("尚未保存的描述")
        compose.onNodeWithTag("field-add").performScrollTo().performClick()
        compose.onNodeWithTag("field-label-0").performScrollTo().performTextInput("联系人")
        compose.onNodeWithTag("field-value-0").performScrollTo().performTextInput("草稿联系人")

        val newer = withRepository {
            save(EventDraft(id = original.id, title = "其他位置的新标题", deadlineAt = original.deadlineAt,
                description = "其他位置的新描述", expectedRevision = original.revision))
        }
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        awaitText("这条事项已被修改，请重新打开后再编辑。")
        compose.onNodeWithTag("event-title").performScrollTo().assertTextContains("尚未保存的标题")
        compose.onNodeWithTag("event-description").performScrollTo().assertTextContains("尚未保存的描述")
        compose.onNodeWithTag("field-label-0").performScrollTo().assertTextContains("联系人")
        compose.onNodeWithTag("field-value-0").performScrollTo().assertTextContains("草稿联系人")
        assertEquals(newer, storedEvents().single())
        compose.onNodeWithText("事项详情").assertDoesNotExist()
    }

    @Test
    fun failedFieldWriteKeepsDraftAndOriginalDataUntilRetrySucceeds() {
        createEvent("保存前的事项")
        val original = storedEvents().single()
        compose.onNodeWithText("编辑事项").performScrollTo().performClick()
        compose.onNodeWithTag("event-title").performScrollTo().performTextReplacement("重试后的标题")
        compose.onNodeWithTag("event-description").performScrollTo().performTextInput("需要保留的描述")
        compose.onNodeWithTag("field-add").performScrollTo().performClick()
        compose.onNodeWithTag("field-label-0").performScrollTo().performTextInput("触发测试")
        compose.onNodeWithTag("field-value-0").performScrollTo().performTextInput("需要保留的字段")

        executeTestDatabaseSql(
            """CREATE TRIGGER fail_ui_field BEFORE INSERT ON custom_fields
               WHEN NEW.label = '触发测试'
               BEGIN SELECT RAISE(ABORT, 'simulated write failure'); END""".trimIndent()
        )
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        awaitText("操作未完成，请重试。当前填写的内容已保留。")
        compose.onNodeWithTag("event-title").performScrollTo().assertTextContains("重试后的标题")
        compose.onNodeWithTag("event-description").performScrollTo().assertTextContains("需要保留的描述")
        compose.onNodeWithTag("field-label-0").performScrollTo().assertTextContains("触发测试")
        compose.onNodeWithTag("field-value-0").performScrollTo().assertTextContains("需要保留的字段")
        assertEquals(original, storedEvents().single())
        compose.onNodeWithText("事项详情").assertDoesNotExist()

        executeTestDatabaseSql("DROP TRIGGER fail_ui_field")
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        awaitText("事项详情")
        awaitText("重试后的标题")
        val saved = storedEvents().single()
        assertEquals(original.id, saved.id)
        assertEquals(original.revision + 1, saved.revision)
        assertEquals("需要保留的描述", saved.description)
        assertEquals("触发测试", saved.customFields.single().label)
        assertEquals("需要保留的字段", saved.customFields.single().value)
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
        recreateAndVerifyLaunchIdentity(compose.activityRule.scenario)
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
    fun combinedSourceTagsKeywordAndDateFilterTheVisibleListWithoutChangingTodayCount() {
        awaitText("DDL Saver")
        val today = Instant.now().atZone(BeijingZone).toLocalDate()
        val nextMonth = YearMonth.from(today).plusMonths(1)
        val day15 = nextMonth.atDay(15)
        val day16 = nextMonth.atDay(16)
        fun deadline(date: java.time.LocalDate) = date.atTime(18, 0).atZone(BeijingZone).toInstant().toEpochMilli()
        val categories = withRepository {
            val source = saveSource(null, "学习")
            val otherSource = saveSource(null, "生活")
            val course = saveTag(null, "课程")
            val submit = saveTag(null, "需提交")
            save(EventDraft(title = "软件项目甲", deadlineAt = deadline(day15), sourceId = source.id,
                tagIds = setOf(course.id, submit.id)))
            save(EventDraft(title = "软件项目乙", deadlineAt = deadline(day15), sourceId = source.id,
                tagIds = setOf(course.id)))
            save(EventDraft(title = "软件项目丙", deadlineAt = deadline(day16), sourceId = source.id,
                tagIds = setOf(course.id, submit.id)))
            save(EventDraft(title = "软件项目丁", deadlineAt = deadline(day15), sourceId = otherSource.id,
                tagIds = setOf(course.id, submit.id)))
            val completed = save(EventDraft(title = "软件项目已完成", deadlineAt = deadline(day15), sourceId = source.id,
                tagIds = setOf(course.id, submit.id)))
            setCompleted(completed.id, true)
            val archived = save(EventDraft(title = "软件项目已归档", deadlineAt = deadline(day15), sourceId = source.id,
                tagIds = setOf(course.id, submit.id)))
            setArchived(archived.id, true)
            save(EventDraft(title = "今日独立事项", deadlineAt = today.atStartOfDay(BeijingZone).toInstant().toEpochMilli()))
            Triple(source, course, submit)
        }
        reloadModel()
        compose.onNodeWithTag("today-count").assertTextContains("1")
        scrollHomeTag("toggle-filters")
        compose.onNodeWithTag("toggle-filters").performClick()
        scrollHomeTag("filter-source-${categories.first.id}")
        compose.onNodeWithTag("filter-source-${categories.first.id}").performScrollTo().performClick().assertIsSelected()
        scrollHomeTag("filter-tag-${categories.second.id}")
        compose.onNodeWithTag("filter-tag-${categories.second.id}").performScrollTo().performClick().assertIsSelected()
        scrollHomeTag("filter-tag-${categories.third.id}")
        compose.onNodeWithTag("filter-tag-${categories.third.id}").performScrollTo().performClick().assertIsSelected()
        scrollHomeTag("event-search")
        compose.onNodeWithTag("event-search").performTextInput("软件项目")
        scrollHomeTag("list-summary")
        compose.onNodeWithTag("list-summary").assertTextEquals("全部待办 · 2")

        scrollHomeTag("toggle-calendar")
        compose.onNodeWithTag("toggle-calendar").performClick()
        scrollHomeTag("calendar-next")
        compose.onNodeWithTag("calendar-next").performClick()
        compose.onNodeWithTag("calendar-count-$day15", useUnmergedTree = true).assertTextEquals("1件")
        compose.onNodeWithTag("calendar-count-$day16", useUnmergedTree = true).assertTextEquals("1件")
        scrollHomeTag("calendar-day-$day15")
        compose.onNodeWithTag("calendar-day-$day15").performClick()
        compose.onNodeWithTag("calendar-count-$day16", useUnmergedTree = true).assertTextEquals("1件")
        scrollHomeTag("list-summary")
        compose.onNodeWithTag("list-summary").assertTextEquals("$day15 的事项 · 1")
        compose.onNodeWithTag("event-list").performScrollToNode(hasText("软件项目甲"))
        compose.onNodeWithText("软件项目甲").assertExists()
        scrollHomeTag("calendar-view-week")
        compose.onNodeWithTag("calendar-view-week").performClick()
        val weekStart = day15.minusDays((day15.dayOfWeek.value - 1).toLong())
        scrollHomeTag("period-title")
        compose.onNodeWithTag("period-title").assertTextEquals("$weekStart 至 ${weekStart.plusDays(6)}")
        compose.onNodeWithTag("week-day-$day15").performClick()
        compose.onNodeWithTag("period-title").assertTextEquals(day15.toString())
        scrollHomeTag("list-summary")
        compose.onNodeWithTag("list-summary").assertTextEquals("$day15 的事项 · 1")
        scrollHomeTag("today-count")
        compose.onNodeWithTag("today-count").assertTextContains("1")
        scrollHomeTag("clear-filters")
        compose.onNodeWithTag("clear-filters").performClick()
        scrollHomeTag("list-summary")
        compose.onNodeWithTag("list-summary").assertTextEquals("全部待办 · 5")
        assertEquals(7, storedEvents().size)
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
        recreateAndVerifyLaunchIdentity(compose.activityRule.scenario)
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

    @Test
    fun reminderSettingsValidateAndPreserveIndependentOptions() {
        val original = AppSettings(urgentMinutes = 120, dailySummaryEnabled = false, dailySummaryMinuteOfDay = 547,
            dangerReminderEnabled = false, deadlineReminderEnabled = true, deadlineLeadMinutes = 45)
        withRepository { saveSettings(original) }
        reloadModel()
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithTag("settings-urgent-minutes").performScrollTo().performTextReplacement("480")
        compose.onNodeWithTag("settings-save").performScrollTo().performClick()
        awaitText("DDL Saver")
        assertEquals(original.copy(urgentMinutes = 480), withRepository { settings.first() })
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithTag("settings-summary-time").performScrollTo().performTextReplacement("24:00")
        compose.onNodeWithTag("settings-save").performScrollTo().performClick()
        compose.onNodeWithText("请输入 00:00 至 23:59 的时间。").assertExists()
        assertEquals(original.copy(urgentMinutes = 480), withRepository { settings.first() })
        compose.onNodeWithTag("settings-summary-time").performScrollTo().performTextReplacement("07:15")
        compose.onNodeWithTag("settings-deadline-lead").performScrollTo().performTextReplacement("0")
        compose.onNodeWithTag("settings-save").performScrollTo().performClick()
        compose.onNodeWithText("请输入有效的正整数分钟数。").assertExists()
        compose.onNodeWithTag("settings-deadline-lead").performScrollTo().performTextReplacement("30")
        compose.onNodeWithTag("settings-daily-enabled").performScrollTo().performClick()
        compose.onNodeWithTag("settings-save").performScrollTo().performClick()
        awaitText("DDL Saver")
        assertEquals(original.copy(urgentMinutes = 480, dailySummaryEnabled = true,
            dailySummaryMinuteOfDay = 435, deadlineLeadMinutes = 30), withRepository { settings.first() })
    }

    @Test
    fun characterStylePreviewRequiresSaveAndCanBeSwitchedBackAfterRecreation() {
        val original = AppSettings(urgentMinutes = 120, dailySummaryEnabled = false,
            dailySummaryMinuteOfDay = 547, dangerReminderEnabled = false, deadlineLeadMinutes = 45)
        withRepository { saveSettings(original) }
        reloadModel()
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithTag("character-style-CLASSIC").assertIsSelected()
        compose.onNodeWithTag("character-style-CHIBI").performScrollTo().performClick()
        compose.onNodeWithTag("character-style-CHIBI").assertIsSelected()
        assertEquals(original, withRepository { settings.first() })
        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithText("放弃修改").performClick()
        awaitText("DDL Saver")
        assertEquals(original, withRepository { settings.first() })

        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithTag("character-style-CLASSIC").assertIsSelected()
        compose.onNodeWithTag("character-style-CHIBI").performClick()
        recreateAndVerifyLaunchIdentity(compose.activityRule.scenario)
        compose.onNodeWithTag("character-style-CHIBI").assertIsSelected()
        assertEquals(original, withRepository { settings.first() })
        compose.onNodeWithTag("settings-save").performScrollTo().performClick()
        awaitText("DDL Saver")
        assertEquals(original.copy(characterStyle = CharacterStyle.CHIBI), withRepository { settings.first() })
        recreateAndVerifyLaunchIdentity(compose.activityRule.scenario)
        awaitText("DDL Saver")
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithTag("character-style-CHIBI").assertIsSelected()
        compose.onNodeWithTag("character-style-CLASSIC").performClick().assertIsSelected()
        compose.onNodeWithTag("settings-save").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        awaitText("DDL Saver")
        assertEquals(original, withRepository { settings.first() })
    }

    @Test
    fun reminderTapResetsHomeFiltersAndRoutesToLatestDetailOnce() {
        val today = Instant.now().atZone(BeijingZone).toLocalDate()
        val target = withRepository { save(EventDraft(title = "通知目标事项", deadlineAt = today.atTime(23, 59).atZone(BeijingZone).toInstant().toEpochMilli())) }
        withRepository { save(EventDraft(title = "另一天事项", deadlineAt = today.plusDays(2).atTime(12, 0).atZone(BeijingZone).toInstant().toEpochMilli())) }
        reloadModel()
        compose.onNodeWithTag("event-search").performTextInput("没有匹配的搜索词")
        scrollHomeTag("toggle-batch")
        compose.onNodeWithTag("toggle-batch").performClick()
        sendReminderIntent(today = true)
        scrollHomeTag("toggle-batch")
        compose.onNodeWithText("批量管理").assertExists()
        compose.onNodeWithText("退出批量").assertDoesNotExist()
        scrollHomeTag("list-summary")
        compose.onNodeWithTag("list-summary").assertTextEquals("今日待办 · 1")
        sendReminderIntent(eventId = target.id)
        awaitText("事项详情")
        awaitText("通知目标事项")
        recreateAndVerifyLaunchIdentity(compose.activityRule.scenario)
        awaitText("事项详情")
        assertNull(ViewModelProvider(compose.activity)[SaverViewModel::class.java].state.value.reminderNavigation)
        assertEquals(2, storedEvents().size)
    }

    @Test
    fun reminderTapDuringDraftCanBeDeferredAcrossRotationWithoutLosingInput() {
        val target = withRepository { save(EventDraft(title = "稍后查看的事项", deadlineAt = Instant.now().plusSeconds(86400).toEpochMilli())) }
        reloadModel()
        openNewEvent()
        compose.onNodeWithTag("event-title").performTextInput("尚未保存的草稿")
        sendReminderIntent(eventId = target.id)
        awaitText("查看通知中的事项？")
        compose.onNodeWithTag("reminder-defer").performClick()
        compose.onNodeWithTag("event-title").assertTextContains("尚未保存的草稿")
        recreateAndVerifyLaunchIdentity(compose.activityRule.scenario)
        compose.onNodeWithTag("event-title").assertTextContains("尚未保存的草稿")
        compose.onNodeWithText("查看通知中的事项？").assertDoesNotExist()
        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithText("放弃修改").performClick()
        awaitText("事项详情")
        awaitText("稍后查看的事项")
        assertEquals(listOf(target.id), storedEvents().map { it.id })
    }

    @Test
    fun missingReminderTargetReturnsHomeWithExplanation() {
        awaitText("DDL Saver")
        sendReminderIntent(eventId = "deleted-test-item")
        awaitText("这条提醒对应的事项已不存在。")
        compose.onNodeWithText("DDL Saver").assertExists()
        assertNull(ViewModelProvider(compose.activity)[SaverViewModel::class.java].state.value.reminderNavigation)
        assertTrue(storedEvents().isEmpty())
    }

    @Test
    fun deniedNotificationPermissionAlwaysHasDirectSystemSettingsRecovery() {
        val application = compose.activity.application
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val model = ViewModelProvider(compose.activity)[SaverViewModel::class.java]
        compose.runOnIdle { model.refreshReminders() }
        compose.waitUntil(20_000) { !model.reminderHealth.value.canPost }
        awaitText("DDL Saver")
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithTag("open-notification-settings").performScrollTo().performClick()
        val launched = shadowOf(compose.activity).nextStartedActivity
        assertNotNull("A denied permission must have a recovery route independent from RequestPermission", launched)
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, checkNotNull(launched).action)
        assertEquals(application.packageName, checkNotNull(launched).getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }

    private fun sendReminderIntent(eventId: String? = null, today: Boolean = false) {
        val launchIdentity = Intent(compose.activity.intent)
        compose.runOnIdle {
            compose.activity.onNewIntent(Intent(compose.activity, MainActivity::class.java)
                .setAction("cn.edu.hust.ddlsaver.OPEN_REMINDER")
                .setData(Uri.parse("ddlsaver://test-reminder/${eventId ?: "today"}"))
                .putExtra("reminder_event_id", eventId).putExtra("reminder_today", today))
        }
        compose.waitForIdle()
        // ActivityScenario tracks lifecycle by its original action/data identity. Fail immediately
        // if a delivery replaces it, before recreate() can enter an unbounded paused-looper wait.
        assertTrue("A notification delivery must preserve the Activity launch identity",
            launchIdentity.filterEquals(compose.activity.intent))
    }

    private fun recreateAndVerifyLaunchIdentity(scenario: ActivityScenario<MainActivity>) {
        var previousIdentity = 0
        var launchIdentity: Intent? = null
        scenario.onActivity { activity ->
            previousIdentity = System.identityHashCode(activity)
            launchIdentity = Intent(activity.intent)
        }
        println("UI-RECREATE-BEGIN")
        scenario.recreate()
        scenario.onActivity { activity ->
            diagnosticModel = ViewModelProvider(activity)[SaverViewModel::class.java]
            assertTrue("recreate() must construct a different Activity instance",
                previousIdentity != System.identityHashCode(activity))
            assertTrue("Recreation must retain the original launch identity",
                checkNotNull(launchIdentity).filterEquals(activity.intent))
        }
        println("UI-RECREATE-END")
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

    private fun executeTestDatabaseSql(sql: String) {
        val database = SaverDatabase.create(ApplicationProvider.getApplicationContext())
        try { database.openHelper.writableDatabase.execSQL(sql) } finally { database.close() }
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
