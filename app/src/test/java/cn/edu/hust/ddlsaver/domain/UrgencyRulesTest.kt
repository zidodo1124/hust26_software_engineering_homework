package cn.edu.hust.ddlsaver.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class UrgencyRulesTest {
    private val deadline = Instant.parse("2030-01-02T10:00:00Z") // Beijing 18:00

    private fun event(sprintMinutes: Long? = 120) = DdlEvent(
        id = "event", title = "课程报告", deadlineAt = deadline.toEpochMilli(),
        sprintMinutes = sprintMinutes, description = "", importance = Importance.NORMAL,
        completedAt = null, archivedAt = null, createdAt = 0, updatedAt = 0, revision = 1,
    )

    @Test
    fun dangerStartsExactlyAtSprintBoundaryAndEndsAtDeadline() {
        val cases = listOf(
            "2030-01-02T07:59:59Z" to Urgency.SOON,
            "2030-01-02T08:00:00Z" to Urgency.DANGER,
            "2030-01-02T09:59:59Z" to Urgency.DANGER,
            "2030-01-02T10:00:00Z" to Urgency.OVERDUE,
            "2030-01-02T10:00:01Z" to Urgency.OVERDUE,
        )
        cases.forEach { (time, expected) ->
            assertEquals(time, expected, UrgencyRules.classify(event(), Instant.parse(time)))
        }
    }

    @Test
    fun ordinaryUrgencyIncludesExactThreshold() {
        assertEquals(Urgency.NORMAL, UrgencyRules.classify(event(null), deadline.minusSeconds(86_401)))
        assertEquals(Urgency.SOON, UrgencyRules.classify(event(null), deadline.minusSeconds(86_400)))
    }

    @Test
    fun absentSprintNeverCreatesDanger() {
        assertEquals(Urgency.SOON, UrgencyRules.classify(event(null), deadline.minusSeconds(1)))
    }

    @Test
    fun dangerTakesPriorityWhenSprintExceedsOrdinaryThreshold() {
        assertEquals(Urgency.DANGER, UrgencyRules.classify(event(30 * 60), deadline.minusSeconds(26 * 3600)))
    }

    @Test
    fun importanceDoesNotChangeTimeClassification() {
        Importance.entries.forEach { importance ->
            assertEquals(
                Urgency.DANGER,
                UrgencyRules.classify(event().copy(importance = importance), deadline.minusSeconds(60)),
            )
        }
    }

    @Test
    fun ordinaryThresholdIsConfigurableIndependentlyOfSprint() {
        val now = deadline.minusSeconds(36 * 3600)
        assertEquals(Urgency.NORMAL, UrgencyRules.classify(event(null), now, 24 * 60))
        assertEquals(Urgency.SOON, UrgencyRules.classify(event(null), now, 48 * 60))
        assertEquals(Urgency.DANGER, UrgencyRules.classify(event(40 * 60), now, 24 * 60))
    }

    @Test
    fun completionAndArchiveAreIndependentAndExcludeActiveScope() {
        val item = event()
        val completed = item.copy(completedAt = 100)
        val archived = item.copy(archivedAt = 200)
        val both = completed.copy(archivedAt = 200)
        assertTrue(UrgencyRules.isActive(item))
        assertFalse(UrgencyRules.isActive(completed))
        assertFalse(UrgencyRules.isActive(archived))
        assertFalse(UrgencyRules.isActive(both))
        assertEquals(Urgency.COMPLETED, UrgencyRules.classify(completed, deadline))
        assertEquals(Urgency.ARCHIVED, UrgencyRules.classify(both, deadline))
        assertEquals(Urgency.COMPLETED, UrgencyRules.classify(both.copy(archivedAt = null), deadline))
        assertEquals(Urgency.OVERDUE, UrgencyRules.classify(item, deadline))
    }

    @Test
    fun beijingMidnightChangesTodayScope() {
        val nextDay = event().copy(deadlineAt = Instant.parse("2030-01-02T16:10:00Z").toEpochMilli())
        assertFalse(UrgencyRules.isToday(nextDay, Instant.parse("2030-01-02T15:59:59Z")))
        assertTrue(UrgencyRules.isToday(nextDay, Instant.parse("2030-01-02T16:00:00Z")))
        assertFalse(UrgencyRules.isToday(event(), Instant.parse("2030-01-02T16:00:00Z")))
    }

    @Test
    fun deviceTimeZoneDoesNotChangeBeijingDateMembership() {
        val previousZone = TimeZone.getDefault()
        try {
            val item = event().copy(deadlineAt = Instant.parse("2030-01-02T16:10:00Z").toEpochMilli())
            val now = Instant.parse("2030-01-02T16:00:00Z")
            listOf("America/Los_Angeles", "UTC", "Asia/Shanghai").forEach { zone ->
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                assertTrue(zone, UrgencyRules.isToday(item, now))
            }
        } finally {
            TimeZone.setDefault(previousZone)
        }
    }

    @Test
    fun todayMembershipDoesNotSilentlyDiscardCompletedOrOverdueEvents() {
        val now = deadline.plusSeconds(60)
        assertTrue(UrgencyRules.isToday(event(), now))
        assertTrue(UrgencyRules.isToday(event().copy(completedAt = 1), now))
    }
}
