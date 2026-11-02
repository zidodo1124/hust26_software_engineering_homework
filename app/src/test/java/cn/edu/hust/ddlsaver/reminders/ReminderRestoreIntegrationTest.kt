package cn.edu.hust.ddlsaver.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import cn.edu.hust.ddlsaver.data.RoomEventRepository
import cn.edu.hust.ddlsaver.data.SaverDatabase
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.EventDraft
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.SQLiteMode
import org.robolectric.shadows.ShadowAlarmManager
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Manifest receiver -> async runtime -> real Room -> framework AlarmManager, without a real reboot. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class ReminderRestoreIntegrationTest {
    private lateinit var app: Application
    private lateinit var database: SaverDatabase
    private lateinit var alarms: AlarmManager
    private lateinit var notifications: NotificationManager
    private var originalFlags = 0

    @Before
    fun prepare() {
        app = ApplicationProvider.getApplicationContext()
        originalFlags = app.applicationInfo.flags
        app.applicationInfo.flags = originalFlags or ApplicationInfo.FLAG_DEBUGGABLE
        app.deleteSharedPreferences(ReminderRecoveryDiagnostics.FILE_NAME)
        ReminderRuntime.resetForTests()
        app.deleteDatabase("ddl-saver.db")
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        alarms = app.getSystemService(AlarmManager::class.java)
        notifications = app.getSystemService(NotificationManager::class.java)
        shadowOf(notifications).setNotificationsEnabled(true)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        ShadowAlarmManager.setAutoSchedule(false)
        database = SaverDatabase.create(app)
        runBlocking {
            val repository = RoomEventRepository(database)
            repository.saveSettings(AppSettings(dailySummaryEnabled = false, dangerReminderEnabled = false))
            repository.save(EventDraft(title = "广播恢复未来事项",
                deadlineAt = Instant.now().plusSeconds(7 * 86_400L).toEpochMilli()))
            val result = ReminderRuntime.get(app).reconcileNow()
            assertEquals(result, ReminderRuntime.get(app).health.value)
        }
        assertEquals(1, shadowOf(alarms).getScheduledAlarms().size)
        assertNull(ReminderRuntime.get(app).health.value.lastError)
    }

    @After
    fun closeResources() {
        ReminderRuntime.resetForTests()
        database.close()
        alarms.cancelAll()
        notifications.cancelAll()
        app.deleteDatabase("ddl-saver.db")
        app.deleteSharedPreferences(ReminderRecoveryDiagnostics.FILE_NAME)
        app.applicationInfo.flags = originalFlags
    }

    @Test
    fun everyDeclaredRestoreBroadcastRebuildsLostSystemAlarmFromUnchangedDurablePlans() = runBlocking {
        val originalPlans = database.dao().readReminderPlans()
        val originalControl = checkNotNull(database.dao().readReminderControl())
        assertTrue(originalPlans.all { it.active && it.state == "PENDING" })
        assertFalse(originalControl.alarmDirty)
        val actions = listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)

        actions.forEach { action ->
            // Simulate loss of framework registration and process-scoped objects, keeping real DB rows.
            alarms.cancelAll()
            ReminderRuntime.resetForTests()
            assertTrue(shadowOf(alarms).getScheduledAlarms().isEmpty())
            assertEquals(originalControl, database.dao().readReminderControl())

            sendRestoreBroadcastAndAwaitFinish(action)

            val restored = shadowOf(alarms).getScheduledAlarms().single()
            assertEquals(originalControl.scheduledTriggerAt, restored.getTriggerAtMs())
            assertEquals(ShadowAlarmManager.WINDOW_EXACT, restored.getWindowLengthMs())
            assertEquals(AndroidReminderPlatform.ACTION_WAKE,
                shadowOf(checkNotNull(restored.operation)).getSavedIntent().action)
            assertEquals(originalPlans, database.dao().readReminderPlans())
            assertEquals(originalControl, database.dao().readReminderControl())
            assertEquals(0, shadowOf(notifications).size())
            assertNull(ReminderRuntime.get(app).health.value.lastError)
            val diagnostic = diagnostic(checkNotNull(RecoveryTrigger.fromAction(action)))
            assertEquals("finished", diagnostic.getString("phase"))
            assertEquals("completed", diagnostic.getString("outcome"))
            assertFalse(diagnostic.getBoolean("has_error"))
            assertEquals(originalPlans.size, diagnostic.getInt("pending_count"))
        }
        assertEquals("completed", diagnostic(RecoveryTrigger.BOOT).getString("outcome"))
    }

    @Test
    fun restoreBroadcastRetainsPlansWithoutNotificationPermissionAndRebuildsAfterPermissionReturns() = runBlocking {
        val originalPlans = database.dao().readReminderPlans()
        val originalTrigger = checkNotNull(database.dao().readReminderControl()).scheduledTriggerAt
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        sendRestoreBroadcastAndAwaitFinish(Intent.ACTION_BOOT_COMPLETED)

        assertFalse(ReminderRuntime.get(app).health.value.canPost)
        assertTrue(shadowOf(alarms).getScheduledAlarms().isEmpty())
        assertEquals(originalPlans, database.dao().readReminderPlans())
        val blocked = checkNotNull(database.dao().readReminderControl())
        assertNull(blocked.scheduledTriggerAt)
        assertFalse(blocked.alarmDirty)
        assertEquals(0, shadowOf(notifications).size())
        assertEquals("notification_blocked", diagnostic(RecoveryTrigger.BOOT).getString("outcome"))
        assertFalse(diagnostic(RecoveryTrigger.BOOT).getBoolean("can_post"))

        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        // A supported restore event after permission returns; there is no invented POST_NOTIFICATIONS broadcast.
        sendRestoreBroadcastAndAwaitFinish(Intent.ACTION_BOOT_COMPLETED)
        assertTrue(ReminderRuntime.get(app).health.value.canPost)
        assertEquals(originalTrigger, shadowOf(alarms).getScheduledAlarms().single().getTriggerAtMs())
        assertEquals(originalPlans, database.dao().readReminderPlans())
        assertFalse(checkNotNull(database.dao().readReminderControl()).alarmDirty)
        assertEquals(0, shadowOf(notifications).size())
        assertEquals("completed", diagnostic(RecoveryTrigger.BOOT).getString("outcome"))
    }

    @Test
    fun unknownActionIsIgnoredByReceiverWithoutChangingDurableOrSystemState() = runBlocking {
        val originalPlans = database.dao().readReminderPlans()
        val originalControl = database.dao().readReminderControl()
        alarms.cancelAll()
        ReminderRuntime.resetForTests()
        val intent = Intent(app, ReminderRestoreReceiver::class.java)
            .setAction("cn.edu.hust.ddlsaver.TEST_UNSUPPORTED_RESTORE")
        // Directly exercise the production receiver's whitelist guard. This does not test manifest
        // resolution of an unsupported action; supported actions use framework dispatch below.
        val receiver = ReminderRestoreReceiver()
        receiver.onReceive(app, intent)

        assertFalse("Unknown actions must not start asynchronous reconciliation", shadowOf(receiver).wentAsync())
        assertTrue(shadowOf(alarms).getScheduledAlarms().isEmpty())
        assertEquals(originalPlans, database.dao().readReminderPlans())
        assertEquals(originalControl, database.dao().readReminderControl())
        assertEquals(0, shadowOf(notifications).size())
        assertTrue(app.getSharedPreferences(ReminderRecoveryDiagnostics.FILE_NAME, Context.MODE_PRIVATE).all.isEmpty())
    }

    private fun diagnostic(trigger: RecoveryTrigger): JSONObject = JSONObject(checkNotNull(
        app.getSharedPreferences(ReminderRecoveryDiagnostics.FILE_NAME, Context.MODE_PRIVATE).getString(trigger.key, null)))

    private suspend fun sendRestoreBroadcastAndAwaitFinish(action: String) {
        // Resolve the receiver registered from the production manifest, then let the framework dispatch it.
        val intent = Intent(action).setPackage(app.packageName)
        val receiver = shadowOf(app).getReceiversForIntent(intent).filterIsInstance<ReminderRestoreReceiver>().single()
        app.sendBroadcast(intent)
        shadowOf(Looper.getMainLooper()).idle()
        val receiverShadow = shadowOf(receiver)
        assertTrue("Expected goAsync for $action", receiverShadow.wentAsync())
        val pending = checkNotNull(receiverShadow.originalPendingResult)
        shadowOf(pending).future.get(10, TimeUnit.SECONDS)
        // Receiver finish intentionally does not wait for best-effort diagnostic disk writes.
        withTimeout(5_000) {
            while (!ReminderRecoveryDiagnostics.sharedWriter.isIdle) delay(1)
        }
    }
}
