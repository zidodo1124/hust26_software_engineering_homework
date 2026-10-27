package cn.edu.hust.ddlsaver.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

object UrgencyRules {
    val BEIJING_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")

    fun classify(event: DdlEvent, now: Instant, urgentMinutes: Long = 1440): Urgency {
        if (event.archivedAt != null) return Urgency.ARCHIVED
        if (event.completedAt != null) return Urgency.COMPLETED
        val remaining = Duration.between(now, Instant.ofEpochMilli(event.deadlineAt))
        if (remaining.isNegative || remaining.isZero) return Urgency.OVERDUE
        val sprint = event.sprintMinutes
        if (sprint != null && sprint > 0 && remaining <= Duration.ofMinutes(sprint)) {
            return Urgency.DANGER
        }
        return if (remaining <= Duration.ofMinutes(urgentMinutes)) Urgency.SOON else Urgency.NORMAL
    }

    fun isActive(event: DdlEvent): Boolean = event.completedAt == null && event.archivedAt == null

    /** Date membership is independent from completion/archive state; callers choose their scope. */
    fun isToday(event: DdlEvent, now: Instant): Boolean =
        Instant.ofEpochMilli(event.deadlineAt).atZone(BEIJING_ZONE).toLocalDate() ==
            now.atZone(BEIJING_ZONE).toLocalDate()
}
