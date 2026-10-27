package cn.edu.hust.ddlsaver.reminders

import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.UrgencyRules
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class ReminderKind { DAILY, DANGER, DEADLINE }

/** Retained after cancellation/sending: it is also the durable per-reason delivery ledger. */
@Entity(tableName = "reminder_plans")
data class ReminderPlanEntity(
    @PrimaryKey val key: String,
    val kind: String,
    val eventId: String?,
    val deadlineAt: Long?,
    val eventRevision: Long?,
    val triggerAt: Long,
    val day: String?,
    val notificationTag: String,
    val active: Boolean = true,
    val state: String = "PENDING",
    val lastAttemptAt: Long? = null,
    val lastError: String? = null,
    val cancelPending: Boolean = false,
)

/** scheduledTriggerAt survives failed cancellation; never discard the last system alarm snapshot. */
@Entity(tableName = "reminder_control")
data class ReminderControlEntity(
    @PrimaryKey val id: Int = 1,
    val alarmIdentity: String = ALARM_IDENTITY,
    val dailyDate: String? = null,
    val dailyMinuteOfDay: Int? = null,
    val scheduledTriggerAt: Long? = null,
    val desiredTriggerAt: Long? = null,
    val scheduleMode: String? = null,
    val alarmDirty: Boolean = true,
    val lastError: String? = null,
)

const val ALARM_IDENTITY = "ddl-saver://reminders/wake"
const val REMINDER_CHANNEL = "ddl_reminders_v1"
const val RETRY_MILLIS = 5 * 60_000L

data class ReminderHealth(
    val canPost: Boolean = false,
    val canScheduleExact: Boolean = false,
    val lastError: String? = null,
    val pendingCount: Int = 0,
)

data class ReminderNotification(
    val tag: String,
    val title: String,
    val text: String,
    val eventId: String? = null,
    val today: Boolean = false,
    val lines: List<String> = emptyList(),
)

object ReminderRules {
    fun eventPlans(event: DdlEvent, settings: AppSettings): List<ReminderPlanEntity> {
        if (!UrgencyRules.isActive(event)) return emptyList()
        val plans = buildList {
            if (settings.dangerReminderEnabled && event.sprintMinutes != null) {
                add(eventPlan(event, ReminderKind.DANGER, event.sprintMinutes))
            }
            if (settings.deadlineReminderEnabled) {
                add(eventPlan(event, ReminderKind.DEADLINE, settings.deadlineLeadMinutes))
            }
        }
        val signatures = plans.groupBy { it.triggerAt }.mapValues { (_, group) ->
            group.map { it.kind }.sorted().joinToString("+")
        }
        return plans.map { it.copy(notificationTag =
            "event:${event.id}:${event.deadlineAt}:${it.triggerAt}:${signatures.getValue(it.triggerAt)}") }
    }

    private fun eventPlan(event: DdlEvent, kind: ReminderKind, minutes: Long): ReminderPlanEntity {
        val duration = Math.multiplyExact(minutes, 60_000L)
        val trigger = try { Math.subtractExact(event.deadlineAt, duration) }
        catch (_: ArithmeticException) { Long.MIN_VALUE }
        return ReminderPlanEntity(
            key = "${kind.name}:${event.id}:${event.deadlineAt}", kind = kind.name,
            eventId = event.id, deadlineAt = event.deadlineAt, eventRevision = event.revision,
            triggerAt = trigger, day = null, notificationTag = "event:${event.id}:${event.deadlineAt}",
        )
    }

    fun nextDailyDate(now: Long, minute: Int): LocalDate {
        val local = Instant.ofEpochMilli(now).atZone(UrgencyRules.BEIJING_ZONE)
        val today = local.toLocalDate()
        return if (dailyTrigger(today, minute) < now) today.plusDays(1) else today
    }

    fun dailyTrigger(day: LocalDate, minute: Int): Long = day
        .atTime(LocalTime.of(minute / 60, minute % 60)).atZone(UrgencyRules.BEIJING_ZONE)
        .toInstant().toEpochMilli()

    fun dailyPlan(day: LocalDate, minute: Int) = ReminderPlanEntity(
        key = "DAILY:$day", kind = ReminderKind.DAILY.name, eventId = null,
        deadlineAt = null, eventRevision = null, triggerAt = dailyTrigger(day, minute),
        day = day.toString(), notificationTag = "daily:$day",
    )

    fun todayEvents(events: List<DdlEvent>, now: Long): List<DdlEvent> = events
        .filter { UrgencyRules.isActive(it) && UrgencyRules.isToday(it, Instant.ofEpochMilli(now)) }
        .sortedWith(compareBy<DdlEvent> { it.deadlineAt }.thenBy { it.id })

    fun nextWake(plans: List<ReminderPlanEntity>, now: Long): Long? = plans.asSequence()
        .filter { it.active && it.state != "SENT" }
        .map { plan -> maxOf(plan.triggerAt, if (plan.state in setOf("FAILED", "CLAIMED")) plan.lastAttemptAt?.let { safeAdd(it, RETRY_MILLIS) } ?: Long.MIN_VALUE else Long.MIN_VALUE) }
        .minOrNull()?.let { maxOf(it, safeAdd(now, 1)) }

    fun safeAdd(value: Long, amount: Long): Long = try { Math.addExact(value, amount) }
    catch (_: ArithmeticException) { Long.MAX_VALUE }
}
