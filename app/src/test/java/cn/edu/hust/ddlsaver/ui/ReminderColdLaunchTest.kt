package cn.edu.hust.ddlsaver.ui

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import cn.edu.hust.ddlsaver.MainActivity
import cn.edu.hust.ddlsaver.data.RoomEventRepository
import cn.edu.hust.ddlsaver.data.SaverDatabase
import cn.edu.hust.ddlsaver.reminders.ReminderRuntime
import cn.edu.hust.ddlsaver.reminders.AndroidReminderPlatform
import cn.edu.hust.ddlsaver.reminders.ReminderNotification
import cn.edu.hust.ddlsaver.domain.AppSettings
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
import org.robolectric.Shadows.shadowOf
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

    @Test
    fun realDailyNotificationPendingIntentColdLaunchesOnlyTodaysUnfinishedItems() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notifications = app.getSystemService(NotificationManager::class.java)
        shadowOf(notifications).setNotificationsEnabled(true)
        val today = Instant.now().atZone(BeijingZone).toLocalDate()
        val database = SaverDatabase.create(app)
        try {
            runBlocking {
                val repository = RoomEventRepository(database)
                repository.saveSettings(AppSettings(dailySummaryEnabled = false,
                    dangerReminderEnabled = false, deadlineReminderEnabled = false))
                repository.save(EventDraft(title = "今日待办目标",
                    deadlineAt = today.atTime(23, 59).atZone(BeijingZone).toInstant().toEpochMilli()))
                repository.save(EventDraft(title = "今日逾期仍待办",
                    deadlineAt = today.atStartOfDay(BeijingZone).toInstant().toEpochMilli()))
                repository.save(EventDraft(title = "明日不应出现",
                    deadlineAt = today.plusDays(1).atTime(12, 0).atZone(BeijingZone).toInstant().toEpochMilli()))
                val completed = repository.save(EventDraft(title = "今日已完成不应出现",
                    deadlineAt = today.atTime(12, 0).atZone(BeijingZone).toInstant().toEpochMilli()))
                repository.setCompleted(completed.id, true)
                val archived = repository.save(EventDraft(title = "今日归档不应出现",
                    deadlineAt = today.atTime(12, 0).atZone(BeijingZone).toInstant().toEpochMilli()))
                repository.setArchived(archived.id, true)
            }
        } finally { database.close() }

        val platform = AndroidReminderPlatform(app)
        val summary = ReminderNotification("qa:daily:cold:$today", "今日 DDL · 2 项", "查看今日事项",
            today = true, lines = listOf("今日待办目标", "今日逾期仍待办"))
        platform.post(summary)
        val posted = checkNotNull(shadowOf(notifications).getNotification(summary.tag, 0))
        val click = checkNotNull(posted.contentIntent)
        assertTrue(shadowOf(click).isImmutable())
        shadowOf(app).clearNextStartedActivities()
        click.send()
        val launch = checkNotNull(shadowOf(app).getNextStartedActivity())
        assertEquals(AndroidReminderPlatform.ACTION_OPEN, launch.action)
        assertTrue(launch.getBooleanExtra(AndroidReminderPlatform.EXTRA_TODAY, false))
        assertNull(launch.getStringExtra(AndroidReminderPlatform.EXTRA_EVENT_ID))

        // Launch the Intent produced by the real PendingIntent, not a separately synthesized route.
        ActivityScenario.launch<MainActivity>(launch).use { scenario ->
            awaitText("DDL Saver", scenario)
            compose.onNodeWithTag("event-list").performScrollToNode(hasTestTag("list-summary"))
            compose.onNodeWithTag("list-summary").assertTextEquals("今日待办 · 2")
            listOf("今日待办目标", "今日逾期仍待办").forEach { title ->
                compose.onNodeWithTag("event-list").performScrollToNode(hasText(title))
                compose.onNodeWithText(title).assertExists()
            }
            listOf("明日不应出现", "今日已完成不应出现", "今日归档不应出现").forEach { title ->
                compose.onNodeWithText(title).assertDoesNotExist()
            }
            scenario.onActivity {
                val state = ViewModelProvider(it)[SaverViewModel::class.java].state.value
                assertEquals(5, state.events.size)
                assertNull(state.reminderNavigation)
            }
            // The cold route starts without saved filters; existing warm-route tests cover clearing old filters.
            compose.onNodeWithTag("event-list").performScrollToNode(hasTestTag("clear-filters"))
            compose.onNodeWithTag("clear-filters").performClick()
            compose.onNodeWithTag("list-summary").assertTextEquals("全部待办 · 3")
            var beforeIdentity = 0
            scenario.onActivity { beforeIdentity = System.identityHashCode(it) }
            scenario.recreate()
            // LazyColumn restores its scroll position, so the offscreen title need not be composed.
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("event-list").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("event-list").performScrollToNode(hasTestTag("list-summary"))
            compose.onNodeWithTag("list-summary").assertTextEquals("全部待办 · 3")
            scenario.onActivity {
                assertNotEquals(beforeIdentity, System.identityHashCode(it))
                assertNull(ViewModelProvider(it)[SaverViewModel::class.java].state.value.reminderNavigation)
            }
        }
        notifications.cancelAll()
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
