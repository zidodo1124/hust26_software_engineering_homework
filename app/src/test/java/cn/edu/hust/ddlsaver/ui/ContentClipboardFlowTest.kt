package cn.edu.hust.ddlsaver.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
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
import cn.edu.hust.ddlsaver.domain.CustomField
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.EventDraft
import cn.edu.hust.ddlsaver.reminders.ReminderRuntime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.SQLiteMode
import org.robolectric.shadows.ShadowDialog
import java.time.Instant

/** Local Activity/Compose/Room checks for TC-07/08 and the DA-04/05 field flows. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class ContentClipboardFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val databaseIsolation = object : ExternalResource() {
        override fun before() = resetDatabase()
        override fun after() = resetDatabase()

        private fun resetDatabase() {
            ReminderRuntime.resetForTests()
            ApplicationProvider.getApplicationContext<Context>().deleteDatabase("ddl-saver.db")
        }
    }

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(databaseIsolation).around(compose)

    @Test
    fun longDescriptionCopiesEveryCharacterAfterSavingAndReopening() {
        val description = (1..48).joinToString("\n") { line ->
            "第 $line 行：完整描述与中文标点；🐟 e\u0301。链接 https://example.invalid/report/$line?q=ddl%20saver&lang=zh#section-$line"
        } + "\n末尾校验：全部内容到此结束。"
        openNewEvent("长描述完整复制")
        compose.onNodeWithTag("event-description").performScrollTo().performTextInput(description)
        saveEditor()
        val saved = storedEvents().single()
        assertEquals(description, saved.description)

        reopenDetail(saved)
        compose.onNodeWithText(description).assertExists()
        setClipboard("复制前的旧内容")
        compose.onNodeWithText("复制全文").performScrollTo().performClick()
        compose.onNodeWithText("已复制").assertExists()
        assertEquals(description, clipboardText())
        assertEquals(saved, storedEvents().single())
    }

    @Test
    fun sameNamedFieldsEditAndCopyIndependentlyWhileEmptyValueAndOtherEventRemain() {
        val other = seedOtherEvent()
        createFieldEvent("同名字段独立复制")
        val original = storedEvents().single { it.id != other.id }
        val changedValue = "丙：第二联系人\n邮箱 contact@example.invalid；🐟"
        compose.onNodeWithText("编辑事项").performScrollTo().performClick()
        compose.onNodeWithTag("field-value-1").performScrollTo().performTextReplacement(changedValue)
        saveEditor()
        val edited = storedEvents().single { it.id == original.id }
        val expectedFields = original.customFields.mapIndexed { index, field ->
            if (index == 1) field.copy(value = changedValue) else field
        }
        assertEquals(expectedFields, edited.customFields)
        assertEquals(other, storedEvents().single { it.id == other.id })

        reopenDetail(edited)
        edited.customFields.take(2).forEach { field ->
            compose.onNodeWithTag("detail-field-${field.id}").assertTextEquals(field.value)
            setClipboard("前一个字段的内容不能冒充本次复制")
            compose.onNodeWithTag("copy-field-${field.id}").performScrollTo().performClick()
            assertEquals(field.value, clipboardText())
        }
        compose.onNodeWithTag("detail-field-${edited.customFields[2].id}").assertTextEquals("")
        assertEquals(expectedFields, storedEvents().single { it.id == original.id }.customFields)
        assertEquals(other, storedEvents().single { it.id == other.id })
    }

    @Test
    fun cancellingFieldEditsThenRemovingOneDuplicatePreservesRemainingOrderAndOtherEvent() {
        val other = seedOtherEvent()
        createFieldEvent("同名字段取消与移除")
        val original = storedEvents().single { it.id != other.id }
        compose.onNodeWithText("编辑事项").performScrollTo().performClick()
        compose.onNodeWithTag("field-label-1").performScrollTo().performTextReplacement("尚未保存的新名称")
        compose.onNodeWithTag("field-value-1").performScrollTo().performTextReplacement("尚未保存的新内容")
        compose.onNodeWithText("‹ 返回").performClick()
        compose.onNodeWithText("放弃未保存的修改？").assertExists()
        compose.onNodeWithText("放弃修改").performClick()
        awaitText("事项详情")
        assertEquals(original, storedEvents().single { it.id == original.id })
        assertEquals(other, storedEvents().single { it.id == other.id })

        compose.onNodeWithText("编辑事项").performScrollTo().performClick()
        compose.onNodeWithTag("field-label-1").performScrollTo().assertTextContains("联系人")
        compose.onNodeWithTag("field-value-1").performScrollTo().assertTextContains("乙：第二联系人")
        compose.onNodeWithTag("field-remove-1").performScrollTo().performClick()
        saveEditor()
        val saved = storedEvents().single { it.id == original.id }
        val expectedFields = listOf(original.customFields[0], original.customFields[2])
        assertEquals(expectedFields, saved.customFields)
        assertEquals(other, storedEvents().single { it.id == other.id })

        reopenDetail(saved)
        compose.onNodeWithTag("detail-field-${original.customFields[1].id}").assertDoesNotExist()
        compose.onNodeWithTag("detail-field-${expectedFields[0].id}").assertTextEquals(expectedFields[0].value)
        compose.onNodeWithText("编辑事项").performScrollTo().performClick()
        compose.onNodeWithTag("field-label-0").performScrollTo().assertTextContains("联系人")
        compose.onNodeWithTag("field-value-0").performScrollTo().assertTextContains("甲：第一联系人")
        compose.onNodeWithTag("field-label-1").performScrollTo().assertTextContains("备注")
        compose.onNodeWithTag("field-label-2").assertDoesNotExist()
        assertEquals(expectedFields, storedEvents().single { it.id == original.id }.customFields)
    }

    private fun createFieldEvent(title: String) {
        openNewEvent(title)
        listOf("联系人" to "甲：第一联系人", "联系人" to "乙：第二联系人", "备注" to "")
            .forEachIndexed { index, (label, value) ->
                compose.onNodeWithTag("field-add").performScrollTo().performClick()
                compose.onNodeWithTag("field-label-$index").performScrollTo().performTextInput(label)
                if (value.isNotEmpty()) {
                    compose.onNodeWithTag("field-value-$index").performScrollTo().performTextInput(value)
                }
            }
        saveEditor()
        val fields = storedEvents().single { it.title == title }.customFields
        assertEquals(3, fields.map { it.id }.toSet().size)
        assertEquals(listOf("联系人", "联系人", "备注"), fields.map { it.label })
        assertEquals(listOf("甲：第一联系人", "乙：第二联系人", ""), fields.map { it.value })
    }

    private fun seedOtherEvent(): DdlEvent {
        awaitText("DDL Saver")
        val other = withRepository {
            save(EventDraft(title = "其他事项的联系人", deadlineAt = Instant.parse("2030-01-03T10:00:00Z").toEpochMilli(),
                customFields = listOf(CustomField("other-contact", "联系人", "其他事项原内容"))))
        }
        compose.runOnIdle { ViewModelProvider(compose.activity)[SaverViewModel::class.java].reload() }
        compose.waitUntil(20_000) {
            ViewModelProvider(compose.activity)[SaverViewModel::class.java].state.value.events.any { it.id == other.id }
        }
        return other
    }

    private fun openNewEvent(title: String) {
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

    private fun saveEditor() {
        compose.onNodeWithTag("event-save").performScrollTo().performClick()
        awaitText("事项详情")
    }

    private fun reopenDetail(event: DdlEvent) {
        compose.onNodeWithText("‹ 返回").performClick()
        awaitText("DDL Saver")
        compose.onNodeWithTag("event-list").performScrollToNode(hasText(event.title))
        compose.onNodeWithText(event.title).performClick()
        awaitText("事项详情")
    }

    private fun setClipboard(value: String) {
        compose.runOnIdle {
            (compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("test", value))
        }
    }

    private fun clipboardText(): String = compose.runOnIdle {
        val clip = (compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
        checkNotNull(clip).getItemAt(0).text.toString()
    }

    private fun awaitText(text: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun storedEvents(): List<DdlEvent> = withRepository { events.first() }

    private fun <T> withRepository(action: suspend RoomEventRepository.() -> T): T = runBlocking {
        withTimeout(10_000) {
            val database = SaverDatabase.create(ApplicationProvider.getApplicationContext())
            try { RoomEventRepository(database).action() } finally { database.close() }
        }
    }
}
