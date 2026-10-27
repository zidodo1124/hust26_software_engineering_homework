package cn.edu.hust.ddlsaver.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import cn.edu.hust.ddlsaver.MainActivity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/** Real platform adapter + framework service shadows, not the engine's injectable fake port. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
class AndroidReminderPlatformTest {
    private lateinit var app: Application
    private lateinit var alarms: AlarmManager
    private lateinit var notifications: NotificationManager
    private lateinit var platform: AndroidReminderPlatform

    @Before fun prepareFrameworkServices() {
        app = ApplicationProvider.getApplicationContext()
        if (Build.VERSION.SDK_INT >= 33) shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        alarms = app.getSystemService(AlarmManager::class.java)
        notifications = app.getSystemService(NotificationManager::class.java)
        shadowOf(notifications).setNotificationsEnabled(true)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        // Inspect registered alarms without dispatching a receiver or starting unrelated background work.
        ShadowAlarmManager.setAutoSchedule(false)
        shadowOf(app).clearNextStartedActivities()
        platform = AndroidReminderPlatform(app)
        assertTrue(platform.capabilities().canPost)
    }

    @After fun clearFrameworkRegistrations() {
        platform.cancelAlarm()
        notifications.cancelAll()
    }

    @Test fun eventAndDailyNotificationsDispatchIndependentImmutableActivityRoutes() {
        val event = ReminderNotification("qa:event:1:danger", "事项 A", "进入速通危险期", eventId = "qa-event-1")
        val daily = ReminderNotification("qa:daily:2030-01-02", "今日 DDL · 2 项", "查看今日事项",
            today = true, lines = listOf("事项 A", "事项 B"))
        platform.post(event)
        platform.post(daily)

        val manager = shadowOf(notifications)
        assertEquals(2, manager.size())
        val eventNotification = checkNotNull(manager.getNotification(event.tag, 0))
        val dailyNotification = checkNotNull(manager.getNotification(daily.tag, 0))
        assertEquals(REMINDER_CHANNEL, eventNotification.channelId)
        assertEquals("事项 A", eventNotification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals(2, dailyNotification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)!!.size)

        val eventClick = checkNotNull(eventNotification.contentIntent)
        val dailyClick = checkNotNull(dailyNotification.contentIntent)
        assertNotEquals(eventClick, dailyClick) // requestCode is shared; data URI must distinguish targets.
        assertTrue(shadowOf(eventClick).isActivity())
        assertTrue(shadowOf(eventClick).isImmutable())
        assertTrue(shadowOf(dailyClick).isImmutable())

        eventClick.send()
        val openedEvent = checkNotNull(shadowOf(app).getNextStartedActivity())
        assertEquals(ComponentName(app, MainActivity::class.java), openedEvent.component)
        assertEquals(AndroidReminderPlatform.ACTION_OPEN, openedEvent.action)
        assertEquals("qa-event-1", openedEvent.getStringExtra(AndroidReminderPlatform.EXTRA_EVENT_ID))
        assertFalse(openedEvent.getBooleanExtra(AndroidReminderPlatform.EXTRA_TODAY, true))
        assertEquals(event.tag, openedEvent.data!!.pathSegments.single())
        assertTrue(openedEvent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(openedEvent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)

        dailyClick.send()
        val openedToday = checkNotNull(shadowOf(app).getNextStartedActivity())
        assertEquals(ComponentName(app, MainActivity::class.java), openedToday.component)
        assertEquals(AndroidReminderPlatform.ACTION_OPEN, openedToday.action)
        assertTrue(openedToday.getBooleanExtra(AndroidReminderPlatform.EXTRA_TODAY, false))
        assertNull(openedToday.getStringExtra(AndroidReminderPlatform.EXTRA_EVENT_ID))
        assertEquals(daily.tag, openedToday.data!!.pathSegments.single())
    }

    @Test fun sameTagRetryReplacesWhileOtherReasonsAndEventsStayIndependent() {
        val first = ReminderNotification("qa:event:1:early", "初次标题", "临近截止", eventId = "qa-event-1")
        val laterReason = ReminderNotification("qa:event:1:danger", "事项 A 危险期", "进入危险期", eventId = "qa-event-1")
        val otherEvent = ReminderNotification("qa:event:2:early", "事项 B", "临近截止", eventId = "qa-event-2")
        val manager = shadowOf(notifications)
        platform.post(first)
        val initialClick = checkNotNull(manager.getNotification(first.tag, 0)).contentIntent
        platform.post(first.copy(title = "恢复后的标题", text = "同一发送身份重试"))
        platform.post(laterReason)
        platform.post(otherEvent)

        assertEquals(3, manager.size())
        val replacement = checkNotNull(manager.getNotification(first.tag, 0))
        assertEquals("恢复后的标题", replacement.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals(initialClick, replacement.contentIntent)
        assertTrue(replacement.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        assertEquals("事项 A 危险期", checkNotNull(manager.getNotification(laterReason.tag, 0))
            .extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("事项 B", checkNotNull(manager.getNotification(otherEvent.tag, 0))
            .extras.getCharSequence(Notification.EXTRA_TITLE).toString())

        platform.cancelNotification(first.tag)
        assertNull(manager.getNotification(first.tag, 0))
        assertEquals(2, manager.size())
        assertNotNull(manager.getNotification(laterReason.tag, 0))
        assertNotNull(manager.getNotification(otherEvent.tag, 0))
    }

    @Test fun systemAlarmUsesStableExplicitImmutableTokenAndSupportsCancelAndExactDenial() {
        val manager = shadowOf(alarms)
        val firstAt = System.currentTimeMillis() + 120_000L
        assertEquals("exact", platform.scheduleAlarm(firstAt, exact = true))
        val first = manager.getScheduledAlarms().single()
        assertEquals(AlarmManager.RTC_WAKEUP, first.getType())
        assertEquals(firstAt, first.getTriggerAtMs())
        assertEquals(ShadowAlarmManager.WINDOW_EXACT, first.getWindowLengthMs())
        assertTrue(first.isAllowWhileIdle())
        val originalOperation = checkNotNull(first.operation)
        val pending = shadowOf(originalOperation)
        assertTrue(pending.isBroadcast())
        assertTrue(pending.isImmutable())
        assertEquals(0, pending.getRequestCode())
        assertEquals(ComponentName(app, ReminderAlarmReceiver::class.java), pending.getSavedIntent().component)
        assertEquals(AndroidReminderPlatform.ACTION_WAKE, pending.getSavedIntent().action)
        assertEquals(ALARM_IDENTITY, pending.getSavedIntent().dataString)

        val movedAt = firstAt + 300_000L
        platform.scheduleAlarm(movedAt, exact = true)
        val moved = manager.getScheduledAlarms().single()
        assertEquals(movedAt, moved.getTriggerAtMs())
        assertEquals(originalOperation, moved.operation)
        platform.cancelAlarm()
        assertTrue(manager.getScheduledAlarms().isEmpty())

        if (Build.VERSION.SDK_INT >= 31) {
            ShadowAlarmManager.setCanScheduleExactAlarms(false)
            assertFalse(platform.capabilities().canScheduleExact)
            assertEquals("approximate", platform.scheduleAlarm(movedAt, exact = true))
        } else {
            // Before special exact-alarm access exists, the explicit approximate branch is still supported.
            assertEquals("approximate", platform.scheduleAlarm(movedAt, exact = false))
        }
        val approximate = manager.getScheduledAlarms().single()
        assertEquals(ShadowAlarmManager.WINDOW_HEURISTIC, approximate.getWindowLengthMs())
        assertTrue(approximate.isAllowWhileIdle())
        assertEquals(originalOperation, approximate.operation)
        platform.cancelAlarm()
        assertTrue(manager.getScheduledAlarms().isEmpty())
    }
}
