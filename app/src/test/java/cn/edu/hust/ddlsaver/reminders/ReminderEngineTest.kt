package cn.edu.hust.ddlsaver.reminders

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import cn.edu.hust.ddlsaver.data.RoomEventRepository
import cn.edu.hust.ddlsaver.data.SaverDatabase
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.CharacterStyle
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.EventDraft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class ReminderEngineTest {
    private lateinit var db: SaverDatabase
    private lateinit var repository: RoomEventRepository
    private lateinit var platform: FakePlatform
    private lateinit var engine: ReminderEngine
    private val clock = MutableClock(Instant.parse("2030-01-01T00:00:00Z"))

    @Before fun open() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SaverDatabase::class.java).build()
        repository = RoomEventRepository(db, clock)
        platform = FakePlatform()
        engine = ReminderEngine(db, platform, clock)
        repository.saveSettings(AppSettings(dailySummaryEnabled = false))
    }

    @After fun close() { db.close() }

    private suspend fun create(name: String = "事项", minutes: Long = 120, sprint: Long? = null): DdlEvent =
        repository.save(EventDraft(title = name, deadlineAt = clock.millis() + minutes * 60_000, sprintMinutes = sprint))

    @Test fun plansExistAtRepositoryCommitAndPlanningFailureRollsBackEvent() = runBlocking {
        val event = create(sprint = 30)
        assertEquals(2, db.dao().readReminderPlans().count { it.active && it.eventId == event.id })
        repository.delete(event.id)
        db.withTransaction {
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_plan BEFORE INSERT ON reminder_plans BEGIN SELECT RAISE(ABORT, 'planned fault'); END")
        }
        try { create("不应落库"); fail("Injected planning write must fail") }
        catch (_: Exception) { /* real SQLite trigger injected into the shared transaction */ }
        assertTrue(db.dao().readAllEventEntities().isEmpty())
        assertFalse(db.dao().readReminderPlans().any { it.active })
    }

    @Test fun reasonsAtDifferentTimesAlertSeparatelyAndReplaysDoNotRepeat() = runBlocking {
        val original = clock.millis()
        create(sprint = 30)
        engine.reconcile()
        assertTrue(platform.posts.isEmpty())
        clock.current = Instant.ofEpochMilli(original + 60 * 60_000)
        engine.reconcile()
        assertEquals(1, platform.posts.size)
        assertTrue(platform.posts.single().text.contains("临近截止"))
        clock.current = Instant.ofEpochMilli(original + 90 * 60_000)
        engine.reconcile()
        engine.reconcile()
        assertEquals(2, platform.posts.size)
        assertEquals(2, platform.posts.map { it.tag }.distinct().size)
        assertTrue(platform.posts.last().text.contains("速通危险期"))
    }

    @Test fun coincidentReasonsMergeButDifferentEventsRemainIndependent() = runBlocking {
        create("同刻双原因", minutes = 60, sprint = 60)
        create("另一事项", minutes = 60)
        engine.reconcile()
        engine.reconcile()
        assertEquals(2, platform.posts.size)
        assertEquals(2, platform.posts.map { it.eventId }.distinct().size)
        val combined = platform.posts.first { it.title == "同刻双原因" }
        assertTrue(combined.text.contains("速通危险期") && combined.text.contains("临近截止"))
        assertEquals(3, db.dao().readReminderPlans().count { it.active && it.state == "SENT" })
    }

    @Test fun oldCallbackAfterDeadlineAndLeadChangesCannotSendEarly() = runBlocking {
        val start = clock.millis()
        val event = create()
        engine.reconcile()
        repository.save(EventDraft(id = event.id, title = "已延后", deadlineAt = start + 240 * 60_000,
            expectedRevision = event.revision))
        repository.saveSettings(AppSettings(dailySummaryEnabled = false, deadlineLeadMinutes = 30))
        clock.current = Instant.ofEpochMilli(start + 60 * 60_000)
        engine.reconcile() // deliberately replay the old wake at its original time
        assertTrue(platform.posts.isEmpty())
        assertEquals(start + 210 * 60_000, platform.scheduled)
        clock.current = Instant.ofEpochMilli(start + 210 * 60_000)
        engine.reconcile()
        assertEquals("已延后", platform.posts.single().title)
    }

    @Test fun completedArchivedDeletedAndExpiredEventsCannotProduceStaleNotifications() = runBlocking {
        val completed = create("完成", 60)
        val archived = create("归档", 60)
        val deleted = create("删除", 60)
        create("过期", 0)
        repository.setCompleted(completed.id, true)
        repository.setArchived(archived.id, true)
        repository.delete(deleted.id)
        engine.reconcile()
        assertTrue(platform.posts.isEmpty())
        repository.setCompleted(completed.id, false)
        engine.reconcile()
        assertEquals("完成", platform.posts.single().title)
        repository.setCompleted(completed.id, true)
        repository.setCompleted(completed.id, false)
        engine.reconcile()
        assertEquals(1, platform.posts.size)
    }

    /** 岳羽泉：已送达后标记完成，应取消仍显示的通知并停用旧计划。 */
    @Test fun completingAfterDeliveryCancelsVisibleNotificationAndDeactivatesPlans() = runBlocking {
        val event = create("完成后失效", minutes = 60)
        engine.reconcile()
        assertEquals(1, platform.visible.size)
        val oldKeys = db.dao().readReminderPlans().filter { it.eventId == event.id }.map { it.key }.toSet()
        repository.setCompleted(event.id, true)
        engine.reconcile()
        assertTrue(platform.visible.isEmpty())
        assertTrue(db.dao().readReminderPlans().filter { it.key in oldKeys }.none { it.active })
        assertEquals(1, platform.posts.size)
    }

    /** 岳羽泉：已送达后归档，应取消仍显示的通知并停用旧计划。 */
    @Test fun archivingAfterDeliveryCancelsVisibleNotificationAndDeactivatesPlans() = runBlocking {
        val event = create("归档后失效", minutes = 60)
        engine.reconcile()
        assertEquals(1, platform.visible.size)
        val oldKeys = db.dao().readReminderPlans().filter { it.eventId == event.id }.map { it.key }.toSet()
        repository.setArchived(event.id, true)
        engine.reconcile()
        assertTrue(platform.visible.isEmpty())
        assertTrue(db.dao().readReminderPlans().filter { it.key in oldKeys }.none { it.active })
    }

    /** 岳羽泉：已送达后删除，应取消仍显示的通知；计划保留账本但不再 active。 */
    @Test fun deletingAfterDeliveryCancelsVisibleNotificationAndDeactivatesPlans() = runBlocking {
        val event = create("删除后失效", minutes = 60)
        engine.reconcile()
        assertEquals(1, platform.visible.size)
        repository.delete(event.id)
        engine.reconcile()
        assertTrue(platform.visible.isEmpty())
        assertTrue(db.dao().readReminderPlans().filter { it.eventId == event.id }.none { it.active })
        assertNull(platform.scheduled)
    }

    /** 岳羽泉：改期后旧截止的计划停用，旧唤醒时刻不得再投递；新唤醒按新截止重排。 */
    @Test fun rescheduleAfterDeliveryCancelsOldAlertAndIgnoresOldDeadlineWake() = runBlocking {
        val start = clock.millis()
        val event = create("改期事项", minutes = 60)
        engine.reconcile()
        assertEquals(1, platform.visible.size)
        val oldDeadline = event.deadlineAt
        repository.save(EventDraft(id = event.id, title = "改期后", deadlineAt = start + 300 * 60_000,
            expectedRevision = event.revision))
        engine.reconcile()
        assertTrue(platform.visible.isEmpty())
        assertTrue(db.dao().readReminderPlans().filter { it.deadlineAt == oldDeadline }.none { it.active })
        clock.current = Instant.ofEpochMilli(start + 60 * 60_000)
        engine.reconcile()
        assertEquals(1, platform.posts.size)
        assertEquals(start + 240 * 60_000, platform.scheduled)
        clock.current = Instant.ofEpochMilli(start + 240 * 60_000)
        engine.reconcile()
        assertEquals("改期后", platform.posts.last().title)
        assertEquals(2, platform.posts.size)
    }

    @Test fun dailySummaryUsesCurrentRowsMarksOverdueAndSkipsEmptyDay() = runBlocking {
        clock.current = Instant.parse("2030-01-01T23:59:00Z") // Beijing Jan 2 07:59
        repository.saveSettings(AppSettings(dangerReminderEnabled = false, deadlineReminderEnabled = false))
        create("今日逾期", -120)
        create("今日未完成", 120)
        val done = create("已完成", 120); repository.setCompleted(done.id, true)
        val archive = create("已归档", 120); repository.setArchived(archive.id, true)
        create("昨天", -1440)
        create("明天", 1440)
        engine.reconcile()
        assertTrue(platform.posts.isEmpty())
        clock.current = clock.current.plusSeconds(60)
        engine.reconcile()
        val summary = platform.posts.single()
        assertTrue(summary.today)
        assertEquals(2, summary.lines.size)
        assertTrue(summary.lines.first().contains("[已逾期]"))
        engine.reconcile()
        assertEquals(1, platform.posts.size)
        assertEquals(setOf(summary.tag), platform.visible.keys)
        // Remove tomorrow's row so the next day's summary is empty.
        db.dao().readAllEventEntities().filter { it.title == "明天" }.forEach { repository.delete(it.id) }
        clock.current = clock.current.plusSeconds(86400)
        engine.reconcile()
        assertEquals(1, platform.posts.size)
        assertTrue(platform.visible.isEmpty()) // previous day's delivered summary is now cancelled
        assertTrue(db.dao().readReminderPlans().any { it.key == "DAILY:2030-01-03" && it.state == "SENT" })
    }

    @Test fun clockRollbackRecalculatesNearestUnsentBeijingSummary() = runBlocking {
        clock.current = Instant.parse("2030-01-11T23:59:00Z") // wrong device date, Beijing Jan 12
        repository.saveSettings(AppSettings(dangerReminderEnabled = false, deadlineReminderEnabled = false))
        engine.reconcile()
        assertEquals(Instant.parse("2030-01-12T00:00:00Z").toEpochMilli(), platform.scheduled)
        clock.current = Instant.parse("2030-01-01T23:59:00Z") // corrected date, Beijing Jan 2
        engine.reconcile(forceReschedule = true)
        assertEquals(Instant.parse("2030-01-02T00:00:00Z").toEpochMilli(), platform.scheduled)
        assertEquals("2030-01-02", db.dao().readReminderControl()!!.dailyDate)
        assertFalse(db.dao().readReminderPlan("DAILY:2030-01-12")!!.active)
    }

    @Test fun sentLedgerKeepsAttemptTimestampAfterDescriptionEditWithoutResending() = runBlocking {
        val event = create(minutes = 60)
        engine.reconcile()
        val original = db.dao().readReminderPlans().single()
        assertEquals("SENT", original.state)
        assertEquals(clock.millis(), original.lastAttemptAt)
        clock.current = clock.current.plusSeconds(30)
        repository.save(EventDraft(id = event.id, title = event.title, deadlineAt = event.deadlineAt,
            description = "仅修改描述", expectedRevision = event.revision))
        val afterEdit = db.dao().readReminderPlans().single()
        assertEquals(original.lastAttemptAt, afterEdit.lastAttemptAt) // claim/attempt time, not observed delivery time
        assertEquals(original.notificationTag, afterEdit.notificationTag)
        engine.reconcile()
        assertEquals(1, platform.posts.size)
        assertEquals(original.lastAttemptAt, db.dao().readReminderPlans().single().lastAttemptAt)
    }

    @Test fun deliveredTodaySummarySurvivesReconcileAndIsCancelledWhenDisabled() = runBlocking {
        repository.saveSettings(AppSettings(dangerReminderEnabled = false, deadlineReminderEnabled = false))
        create("今日待办", 120)
        engine.reconcile()
        val summary = platform.posts.single()
        repeat(3) { engine.reconcile(forceReschedule = true) }
        assertEquals(setOf(summary.tag), platform.visible.keys)
        assertEquals(1, platform.posts.size)
        repository.saveSettings(AppSettings(dailySummaryEnabled = false,
            dangerReminderEnabled = false, deadlineReminderEnabled = false))
        engine.reconcile()
        assertTrue(platform.visible.isEmpty())
        assertTrue(db.dao().readReminderPlans().none { it.active })
        assertNull(platform.scheduled)
    }

    @Test fun enablingDailyAfterItsTimeSchedulesTomorrowRatherThanHistoricalBurst() = runBlocking {
        clock.current = clock.current.plusSeconds(60)
        repository.saveSettings(AppSettings(dangerReminderEnabled = false, deadlineReminderEnabled = false))
        create(minutes = 120)
        engine.reconcile()
        assertTrue(platform.posts.isEmpty())
        assertEquals(Instant.parse("2030-01-02T00:00:00Z").toEpochMilli(), platform.scheduled)
    }

    @Test fun schedulingFailureKeepsDurableDesiredPlanAndRetryDoesNotDuplicateEvent() = runBlocking {
        val event = create()
        platform.failSchedule = true
        assertNotNull(engine.reconcile().lastError)
        assertTrue(db.dao().readReminderControl()!!.alarmDirty)
        assertNotNull(db.dao().readReminderControl()!!.desiredTriggerAt)
        platform.failSchedule = false
        ReminderEngine(db, platform, clock).reconcile()
        assertFalse(db.dao().readReminderControl()!!.alarmDirty)
        assertEquals(listOf(event.id), db.dao().readAllEventEntities().map { it.id })
        assertNotNull(platform.scheduled)
    }

    @Test fun failedCancellationRetainsIdentityAndSnapshotAfterDeletionUntilRecovery() = runBlocking {
        val event = create()
        engine.reconcile()
        val oldWake = platform.scheduled
        repository.delete(event.id)
        platform.failCancelAlarm = true
        platform.failCancelNotification = true
        engine.reconcile()
        val retained = db.dao().readReminderPlans().first { it.eventId == event.id }
        assertFalse(retained.active)
        assertTrue(retained.cancelPending)
        assertEquals(event.deadlineAt, retained.deadlineAt)
        assertEquals(oldWake, db.dao().readReminderControl()!!.scheduledTriggerAt)
        assertTrue(db.dao().readReminderControl()!!.alarmDirty)
        platform.failCancelAlarm = false
        platform.failCancelNotification = false
        ReminderEngine(db, platform, clock).reconcile(forceReschedule = true)
        assertFalse(db.dao().readReminderPlans().first { it.eventId == event.id }.cancelPending)
        assertNull(platform.scheduled)
        assertTrue(platform.posts.isEmpty())
    }

    @Test fun failedPostBacksOffAndCanRecoverFromPersistedState() = runBlocking {
        create(minutes = 60)
        platform.failPost = true
        engine.reconcile()
        assertEquals("FAILED", db.dao().readReminderPlans().single().state)
        val attempts = platform.postAttempts
        engine.reconcile()
        assertEquals(attempts, platform.postAttempts)
        platform.failPost = false
        clock.current = clock.current.plusMillis(RETRY_MILLIS)
        ReminderEngine(db, platform, clock).reconcile()
        assertEquals(1, platform.posts.size)
        assertEquals("SENT", db.dao().readReminderPlans().single().state)
    }

    @Test fun disabledNotificationCapabilityPausesAlarmAndGrantReconcilesDuePlans() = runBlocking {
        val start = clock.millis()
        create()
        engine.reconcile()
        assertNotNull(platform.scheduled)
        platform.allowed = false
        assertEquals(1, engine.reconcile().pendingCount)
        assertNull(platform.scheduled)
        clock.current = Instant.ofEpochMilli(start + 61 * 60_000)
        engine.reconcile()
        assertTrue(platform.posts.isEmpty())
        assertNull(platform.scheduled)
        platform.allowed = true
        engine.reconcile()
        assertEquals(1, platform.posts.size)
    }

    @Test fun notifySuccessBeforeCommitLeavesClaimAndRetryReusesStableNotificationIdentity() = runBlocking {
        create(minutes = 60)
        val interrupted = ReminderEngine(db, platform, clock, afterPost = { throw SimulatedCrash() })
        try { interrupted.reconcile(); fail("Expected controlled process-interruption hook") }
        catch (_: SimulatedCrash) { }
        assertEquals("CLAIMED", db.dao().readReminderPlans().single().state)
        assertEquals(1, platform.posts.size)
        engine.reconcile()
        assertEquals(1, platform.posts.size) // uncertain claims also respect the retry delay
        clock.current = clock.current.plusMillis(RETRY_MILLIS)
        engine.reconcile()
        assertEquals(2, platform.posts.size)
        assertEquals(1, platform.visible.size)
        assertEquals(1, platform.posts.map { it.tag }.distinct().size)
        assertEquals("SENT", db.dao().readReminderPlans().single().state)
    }

    @Test fun characterStyleChangePreservesSentLedgerAndDoesNotPostAgain() = runBlocking {
        create("已提醒", minutes = 60, sprint = 60)
        create("稍后提醒", minutes = 180, sprint = 30)
        engine.reconcile()
        assertEquals(1, platform.posts.size)
        val plans = db.dao().readReminderPlans()
        val wake = platform.scheduled
        val eventRows = db.dao().readAllEventEntities()
        for (style in listOf(CharacterStyle.CHIBI, CharacterStyle.CLASSIC)) {
            repository.saveSettings(AppSettings(dailySummaryEnabled = false, characterStyle = style))
            engine.reconcile()
            assertEquals(plans, db.dao().readReminderPlans())
            assertEquals(wake, platform.scheduled)
            assertEquals(eventRows, db.dao().readAllEventEntities())
            assertEquals(1, platform.posts.size)
            assertEquals(1, platform.postAttempts)
        }
    }

    private class SimulatedCrash : Error()
    private class MutableClock(var current: Instant) : Clock() {
        override fun instant(): Instant = current
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(current, zone)
    }
    private class FakePlatform : ReminderPlatform {
        var allowed = true
        var exact = true
        var failSchedule = false
        var failCancelAlarm = false
        var failCancelNotification = false
        var failPost = false
        var scheduled: Long? = null
        var postAttempts = 0
        val posts = mutableListOf<ReminderNotification>()
        val visible = mutableMapOf<String, ReminderNotification>()
        override fun capabilities() = ReminderHealth(canPost = allowed, canScheduleExact = exact)
        override fun cancelAlarm() {
            if (failCancelAlarm) throw IOException("controlled cancel fault")
            scheduled = null
        }
        override fun scheduleAlarm(triggerAt: Long, exact: Boolean): String {
            if (failSchedule) throw IOException("controlled schedule fault")
            scheduled = triggerAt
            return if (exact) "exact" else "approximate"
        }
        override fun post(notification: ReminderNotification) {
            postAttempts++
            if (failPost) throw IOException("controlled notify fault")
            posts += notification
            visible[notification.tag] = notification
        }
        override fun cancelNotification(tag: String) {
            if (failCancelNotification) throw IOException("controlled notification cancel fault")
            visible.remove(tag)
        }
    }
}
