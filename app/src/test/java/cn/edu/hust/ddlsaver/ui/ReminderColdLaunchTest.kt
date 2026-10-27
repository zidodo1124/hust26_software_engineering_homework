package cn.edu.hust.ddlsaver.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import cn.edu.hust.ddlsaver.MainActivity
import cn.edu.hust.ddlsaver.data.RoomEventRepository
import cn.edu.hust.ddlsaver.data.SaverDatabase
import cn.edu.hust.ddlsaver.reminders.ReminderRuntime
import cn.edu.hust.ddlsaver.domain.EventDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
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
import java.time.Instant

/** Owns exactly one cold Activity; no already-closed Compose host is reused. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class ReminderColdLaunchTest {
    private val compose = createEmptyComposeRule()
    private val isolation = object : ExternalResource() {
        override fun before() = resetDatabase()
        override fun after() = resetDatabase()
        private fun resetDatabase() {
            ReminderRuntime.resetForTests()
            ApplicationProvider.getApplicationContext<Context>().deleteDatabase("ddl-saver.db")
        }
    }
    @get:Rule val rules: TestRule = RuleChain.outerRule(isolation).around(compose)

    @Test
    fun coldReminderLaunchOpensDetailAndRotationDoesNotReplayIt() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = SaverDatabase.create(context)
        val target = try {
            runBlocking { RoomEventRepository(database).save(EventDraft(title = "冷启动通知事项",
                deadlineAt = Instant.now().plusSeconds(86400).toEpochMilli())) }
        } finally { database.close() }
        val launch = Intent(context, MainActivity::class.java)
            .setAction("cn.edu.hust.ddlsaver.OPEN_REMINDER")
            .setData(Uri.parse("ddlsaver://test-reminder/cold-${target.id}"))
            .putExtra("reminder_event_id", target.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(launch).use { scenario ->
            awaitText("事项详情", scenario)
            awaitText("冷启动通知事项", scenario)
            scenario.onActivity { assertNull(ViewModelProvider(it)[SaverViewModel::class.java].state.value.reminderNavigation) }
            // Navigate away before recreation: replaying the original Intent would reopen detail.
            compose.onNodeWithText("‹ 返回").performClick()
            awaitText("DDL Saver", scenario)
            var beforeIdentity = 0
            scenario.onActivity { beforeIdentity = System.identityHashCode(it) }
            println("UI-COLD-RECREATE-BEGIN")
            scenario.recreate()
            println("UI-COLD-RECREATE-END")
            awaitText("DDL Saver", scenario)
            compose.onNodeWithText("事项详情").assertDoesNotExist()
            scenario.onActivity {
                assertTrue(beforeIdentity != System.identityHashCode(it))
                assertTrue(launch.filterEquals(it.intent))
                assertNull(ViewModelProvider(it)[SaverViewModel::class.java].state.value.reminderNavigation)
            }
            val reopened = SaverDatabase.create(context)
            try {
                val rows = runBlocking { withTimeout(10_000) { RoomEventRepository(reopened).events.first() } }
                assertEquals(listOf(target.id), rows.map { it.id })
            } finally { reopened.close() }
        }
    }

    private fun awaitText(text: String, scenario: ActivityScenario<MainActivity>) {
        try {
            compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: Throwable) {
            scenario.onActivity { activity ->
                val state = ViewModelProvider(activity)[SaverViewModel::class.java].state.value
                println("UI-COLD-STATE loading=${state.loading} busy=${state.busy} loadError=${state.loadError} " +
                    "operationError=${state.operationError} eventCount=${state.events.size} pending=${state.reminderNavigation != null}")
            }
            throw failure
        }
    }
}
