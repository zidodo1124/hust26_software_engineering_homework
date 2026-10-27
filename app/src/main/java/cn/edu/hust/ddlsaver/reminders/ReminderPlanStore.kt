package cn.edu.hust.ddlsaver.reminders

import cn.edu.hust.ddlsaver.data.SaverDao
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.UrgencyRules
import java.time.Instant
import java.time.LocalDate

/** Caller must hold a Room transaction. Repository mutations and their plans commit together. */
object ReminderPlanStore {
    suspend fun rebuild(dao: SaverDao, now: Long) {
        val settings = dao.readSettings()?.toDomain() ?: AppSettings()
        val events = dao.readAllEventEntities().map { it.toDomain() }
        val existing = dao.readReminderPlans().associateBy { it.key }
        val control = dao.readReminderControl() ?: ReminderControlEntity()
        val desired = events.filter { it.deadlineAt > now }.flatMap { ReminderRules.eventPlans(it, settings) }.toMutableList()
        val today = Instant.ofEpochMilli(now).atZone(UrgencyRules.BEIJING_ZONE).toLocalDate()
        var dailyDate: LocalDate? = null
        if (settings.dailySummaryEnabled) {
            val savedDate = control.dailyDate?.let(LocalDate::parse)
            val nearest = ReminderRules.nextDailyDate(now, settings.dailySummaryMinuteOfDay)
            dailyDate = if (control.dailyMinuteOfDay == settings.dailySummaryMinuteOfDay &&
                savedDate != null && !savedDate.isBefore(today) && !savedDate.isAfter(nearest)
            ) savedDate else nearest
            while (existing["DAILY:$dailyDate"]?.state == "SENT") dailyDate = dailyDate!!.plusDays(1)
            desired += ReminderRules.dailyPlan(dailyDate!!, settings.dailySummaryMinuteOfDay)
        }
        val desiredKeys = desired.map { it.key }.toSet()
        existing.values.filter { it.active && it.key !in desiredKeys }.forEach { old ->
            // Advancing tomorrow's schedule must not dismiss today's delivered notification.
            val retainTodaySummary = old.kind == ReminderKind.DAILY.name && old.state == "SENT" &&
                old.day == today.toString() && settings.dailySummaryEnabled
            if (!retainTodaySummary) dao.putReminderPlan(old.copy(active = false, cancelPending = true))
        }
        desired.forEach { fresh ->
            val old = existing[fresh.key]
            val changed = old == null || !old.active || old.triggerAt != fresh.triggerAt ||
                old.eventRevision != fresh.eventRevision
            if (old != null && old.state != "SENT" && old.notificationTag != fresh.notificationTag) {
                // A previous process may have notified before committing. Preserve its actual cancel identity.
                dao.putReminderPlan(old.copy(key = "CANCEL:${old.notificationTag}",
                    active = false, cancelPending = true))
            }
            dao.putReminderPlan(if (old == null) fresh else fresh.copy(
                notificationTag = if (old.state == "SENT") old.notificationTag else fresh.notificationTag,
                state = if (old.state == "SENT") "SENT" else if (changed) "PENDING" else old.state,
                lastAttemptAt = if (old.state == "SENT" || !changed) old.lastAttemptAt else null,
                lastError = if (changed) null else old.lastError,
                cancelPending = false,
            ))
        }
        val wake = ReminderRules.nextWake(dao.readReminderPlans(), now)
        dao.putReminderControl(control.copy(
            dailyDate = dailyDate?.toString(),
            dailyMinuteOfDay = if (settings.dailySummaryEnabled) settings.dailySummaryMinuteOfDay else null,
            desiredTriggerAt = wake,
            alarmDirty = control.alarmDirty || control.scheduledTriggerAt != wake,
        ))
    }
}
