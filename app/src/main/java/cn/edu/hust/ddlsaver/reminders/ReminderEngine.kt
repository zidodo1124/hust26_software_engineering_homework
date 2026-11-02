package cn.edu.hust.ddlsaver.reminders

import androidx.room.withTransaction
import cn.edu.hust.ddlsaver.data.SaverDatabase
import cn.edu.hust.ddlsaver.data.RoomEventRepository
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.UrgencyRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

interface ReminderPlatform {
    fun capabilities(): ReminderHealth
    fun cancelAlarm()
    /** Returns the mode actually used, including an exact-permission race fallback. */
    fun scheduleAlarm(triggerAt: Long, exact: Boolean): String
    fun post(notification: ReminderNotification)
    fun cancelNotification(tag: String)
}

/** All callbacks reconcile current durable data; callback extras are never a source of truth. */
class ReminderEngine(
    private val database: SaverDatabase,
    private val platform: ReminderPlatform,
    private val clock: Clock = Clock.systemUTC(),
    private val afterPost: (() -> Unit)? = null,
) {
    private val dao = database.dao()

    suspend fun reconcile(forceReschedule: Boolean = false): ReminderHealth = serial.withLock {
        val now = clock.millis()
        val capability = platform.capabilities()
        RoomEventRepository(database, clock).extendRecurrencesThrough(Instant.ofEpochMilli(now).plusSeconds(90L * 86_400L))
        database.withTransaction { ReminderPlanStore.rebuild(dao, now) }
        cancelInvalidNotifications()
        if (capability.canPost) {
            val due = dao.readReminderPlans().filter { ready(it, now) }
            // Same event + deadline + planned instant share one notification, with both ledger keys.
            due.groupBy { if (it.eventId == null) it.key else "${it.eventId}:${it.deadlineAt}:${it.triggerAt}" }
                .values.forEach { group -> deliver(group.map { it.key }, now) }
        }
        database.withTransaction { ReminderPlanStore.rebuild(dao, now) }
        if (forceReschedule) database.withTransaction {
            val previous = dao.readReminderControl() ?: ReminderControlEntity()
            dao.putReminderControl(previous.copy(alarmDirty = true))
        }
        database.withTransaction {
            val pending = dao.readReminderPlans().filter { it.active && it.state != "SENT" }
            // Keep business plans durable, but do not poll/wake while notification permission/channel is off.
            val next = if (capability.canPost) ReminderRules.nextWake(pending, now) else null
            var control = dao.readReminderControl() ?: ReminderControlEntity()
            val mode = if (capability.canScheduleExact) "exact" else "approximate"
            val needsSystemUpdate = forceReschedule || control.alarmDirty ||
                control.scheduledTriggerAt != next || (next != null && control.scheduleMode != mode)
            if (needsSystemUpdate) {
                // Commit the desired intent first. If cancel/set fails, retain the previous snapshot.
                control = control.copy(desiredTriggerAt = next, alarmDirty = true)
                dao.putReminderControl(control)
                try {
                    platform.cancelAlarm()
                    control = control.copy(scheduledTriggerAt = null, scheduleMode = null)
                    dao.putReminderControl(control)
                    val actualMode = next?.let { platform.scheduleAlarm(it, capability.canScheduleExact) }
                    control = control.copy(scheduledTriggerAt = next, scheduleMode = actualMode,
                        alarmDirty = false, lastError = null)
                    dao.putReminderControl(control)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    control = control.copy(alarmDirty = true,
                        lastError = "提醒安排暂未完成（${error.javaClass.simpleName}），打开应用后将重试。")
                    dao.putReminderControl(control)
                }
            }
            val errors = dao.readReminderPlans().firstOrNull { (it.active || it.cancelPending) && it.lastError != null }?.lastError
            platform.capabilities().copy(lastError = control.lastError ?: errors, pendingCount = pending.size)
        }
    }

    private fun ready(plan: ReminderPlanEntity, now: Long): Boolean = plan.active &&
        plan.state != "SENT" && plan.triggerAt <= now &&
        (plan.state !in setOf("FAILED", "CLAIMED") || plan.lastAttemptAt == null ||
            ReminderRules.safeAdd(plan.lastAttemptAt, RETRY_MILLIS) <= now)

    private suspend fun cancelInvalidNotifications() {
        dao.readReminderPlans().filter { it.cancelPending }.forEach { snapshot ->
            database.withTransaction {
                val current = dao.readReminderPlan(snapshot.key) ?: return@withTransaction
                if (current.active || !current.cancelPending) return@withTransaction
                try {
                    // A merged notification may still be valid for the other enabled reason.
                    if (dao.readReminderPlans().none { it.active && it.notificationTag == current.notificationTag }) {
                        platform.cancelNotification(current.notificationTag)
                    }
                    dao.putReminderPlan(current.copy(cancelPending = false, lastError = null))
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    dao.putReminderPlan(current.copy(lastError = "旧提醒取消暂未完成（${error.javaClass.simpleName}），将重试。"))
                }
            }
        }
    }

    private suspend fun deliver(keys: List<String>, now: Long) {
        // Persist the claim independently, so interruption during system notify leaves an auditable state.
        val claimed = database.withTransaction {
            ReminderPlanStore.rebuild(dao, now)
            keys.mapNotNull { dao.readReminderPlan(it) }.filter { ready(it, now) }.onEach {
                dao.putReminderPlan(it.copy(state = "CLAIMED", lastAttemptAt = now, lastError = null))
            }.map { it.key }
        }
        if (claimed.isEmpty()) return
        database.withTransaction {
            ReminderPlanStore.rebuild(dao, clock.millis())
            val currentNow = clock.millis()
            val current = claimed.mapNotNull { dao.readReminderPlan(it) }
                .filter { it.active && it.state == "CLAIMED" && it.triggerAt <= currentNow }
            if (current.isEmpty()) return@withTransaction
            // Read the latest content/settings while holding the same DB write transaction as mutations.
            val events = dao.readAllEventEntities().map { it.toDomain() }
            val notification = content(current, events, currentNow)
            try {
                if (notification != null) {
                    check(platform.capabilities().canPost) { "Notifications unavailable" }
                    platform.post(notification)
                    // A test hook can simulate the notify-success / DB-commit interruption window.
                    afterPost?.invoke()
                }
                current.forEach { dao.putReminderPlan(it.copy(state = "SENT", lastError = null)) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                current.forEach { dao.putReminderPlan(it.copy(state = "FAILED", lastAttemptAt = currentNow,
                    lastError = "通知暂未提交（${error.javaClass.simpleName}），将重试。")) }
            }
        }
    }

    private fun content(plans: List<ReminderPlanEntity>, events: List<DdlEvent>, now: Long): ReminderNotification? {
        val first = plans.first()
        if (first.kind == ReminderKind.DAILY.name) {
            if (first.day != Instant.ofEpochMilli(now).atZone(UrgencyRules.BEIJING_ZONE).toLocalDate().toString()) return null
            val today = ReminderRules.todayEvents(events, now)
            if (today.isEmpty()) return null
            return ReminderNotification(first.notificationTag, "今日 DDL · ${today.size} 项",
                "查看今天截止且未完成的事项，按截止时间安排进度。", today = true,
                lines = today.take(7).map { (if (it.deadlineAt <= now) "[已逾期] " else "") + it.title })
        }
        val event = events.firstOrNull { it.id == first.eventId && it.deadlineAt == first.deadlineAt &&
            UrgencyRules.isActive(it) && it.deadlineAt > now } ?: return null
        val reasons = plans.map { if (it.kind == ReminderKind.DANGER.name) "进入速通危险期" else "临近截止" }.distinct()
        val time = deadlineFormat.format(Instant.ofEpochMilli(event.deadlineAt))
        return ReminderNotification(first.notificationTag, event.title,
            reasons.joinToString(" · ") + "，截止 $time（北京时间）。", eventId = event.id)
    }

    companion object {
        private val serial = Mutex()
        private val deadlineFormat = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
            .withZone(UrgencyRules.BEIJING_ZONE)
    }
}
