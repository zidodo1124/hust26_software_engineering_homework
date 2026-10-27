package cn.edu.hust.ddlsaver.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.ViewModelProvider
import cn.edu.hust.ddlsaver.MainActivity
import cn.edu.hust.ddlsaver.data.RoomEventRepository
import cn.edu.hust.ddlsaver.data.SaverDatabase
import cn.edu.hust.ddlsaver.domain.DdlEvent
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
import org.robolectric.shadows.ShadowDialog

/** Runs the real Activity, Compose screens and Room repository in an isolated local Android runtime. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
class SaverAppFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val databaseIsolation = object : ExternalResource() {
        override fun before() = removeTestDatabase()
        override fun after() = removeTestDatabase()

        private fun removeTestDatabase() {
            ApplicationProvider.getApplicationContext<Context>().deleteDatabase("ddl-saver.db")
        }
    }

    private val failureDiagnostics = object : TestWatcher() {
        override fun failed(error: Throwable, description: Description) {
            dumpDiagnostics("FAILED ${description.methodName}")
        }
    }

    // Delete only the sandbox database, before Activity launch and after Activity destruction.
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
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("event-title").performScrollTo().assertTextContains("尚未保存的标题")
        compose.onNodeWithTag("event-description").performScrollTo().assertTextContains("旋转后仍需保留的描述")

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

    private fun dumpDiagnostics(label: String) {
        println("UI-DIAGNOSTIC $label")
        runCatching {
            println(compose.onRoot(useUnmergedTree = true).printToString())
        }.onFailure { println("Tree unavailable: ${it.message}") }
        runCatching {
            val state = compose.runOnIdle {
                ViewModelProvider(compose.activity)[SaverViewModel::class.java].state.value
            }
            println("ViewModel: $state")
            println("Database: ${storedEvents()}")
        }.onFailure { println("State unavailable: ${it.message}") }
    }
}
