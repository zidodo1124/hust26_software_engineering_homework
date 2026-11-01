package cn.edu.hust.ddlsaver.reminders

import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.EventValidation
import cn.edu.hust.ddlsaver.domain.EventValidationException
import cn.edu.hust.ddlsaver.domain.Importance
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ReminderRulesTest {
    private val deadline = Instant.parse("2030-01-01T04:00:00Z").toEpochMilli()
    private fun event(sprint: Long? = 60) = DdlEvent("a", "测试", deadline, sprint, "", Importance.NORMAL,
        null, null, 0, 0, 1)

    @Test fun exactBoundaryAndMergedIdentityAreDerivedFromMinutes() {
        val plans = ReminderRules.eventPlans(event(), AppSettings())
        assertEquals(2, plans.size)
        assertTrue(plans.all { it.triggerAt == deadline - 60 * 60_000L })
        assertEquals(1, plans.map { it.notificationTag }.distinct().size)
        assertEquals(2, plans.map { it.key }.distinct().size)
    }

    @Test fun separateReasonTimesHaveSeparateAlertIdentities() {
        val plans = ReminderRules.eventPlans(event(20), AppSettings())
        assertEquals(2, plans.map { it.notificationTag }.distinct().size)
        assertEquals(setOf(deadline - 20 * 60_000L, deadline - 60 * 60_000L), plans.map { it.triggerAt }.toSet())
        assertEquals(plans, ReminderRules.eventPlans(event(20), AppSettings(urgentMinutes = 2)))
    }

    @Test fun noSprintAndDisabledReasonsDoNotCreateDangerPlans() {
        assertEquals(listOf("DEADLINE"), ReminderRules.eventPlans(event(null), AppSettings()).map { it.kind })
        assertTrue(ReminderRules.eventPlans(event(), AppSettings(dangerReminderEnabled = false,
            deadlineReminderEnabled = false)).isEmpty())
        assertTrue(ReminderRules.eventPlans(event().copy(completedAt = 1), AppSettings()).isEmpty())
        assertTrue(ReminderRules.eventPlans(event().copy(archivedAt = 1), AppSettings()).isEmpty())
    }

    @Test fun dailyNextOccurrenceUsesBeijingAndDoesNotCatchUpNewInstall() {
        val before = Instant.parse("2030-01-01T23:59:00Z").toEpochMilli()
        val day = LocalDate.parse("2030-01-02")
        assertEquals(day, ReminderRules.nextDailyDate(before, 480))
        assertEquals(Instant.parse("2030-01-02T00:00:00Z").toEpochMilli(), ReminderRules.dailyTrigger(day, 480))
        assertEquals(day.plusDays(1), ReminderRules.nextDailyDate(before + 120_000, 480))
    }

    @Test fun summaryIncludesTodayOverdueButExcludesCompletedArchivedAndOtherDays() {
        val now = Instant.parse("2030-01-01T04:00:00Z").toEpochMilli()
        val rows = listOf(event().copy(id = "overdue", deadlineAt = now - 60_000),
            event().copy(id = "upcoming", deadlineAt = now + 60_000),
            event().copy(id = "done", completedAt = now), event().copy(id = "archive", archivedAt = now),
            event().copy(id = "yesterday", deadlineAt = now - 86_400_000))
        assertEquals(listOf("overdue", "upcoming"), ReminderRules.todayEvents(rows, now).map { it.id })
    }

    @Test fun settingsValidateBeforeAnyStorageMutation() {
        listOf(AppSettings(dailySummaryMinuteOfDay = -1), AppSettings(dailySummaryMinuteOfDay = 1440),
            AppSettings(deadlineLeadMinutes = 0), AppSettings(deadlineLeadMinutes = Long.MAX_VALUE)).forEach {
            assertThrows(EventValidationException::class.java) { EventValidation.validateSettings(it) }
        }
        EventValidation.validateSettings(AppSettings(dailySummaryMinuteOfDay = 1439, deadlineLeadMinutes = 1))
    }

    /** 岳羽泉：发送键按 (kind, eventId, deadline) 稳定唯一，重复计算不得增生键。 */
    @Test fun planKeysAreStableUniquePerReasonAndDeadline() {
        val settings = AppSettings(deadlineLeadMinutes = 60)
        val first = ReminderRules.eventPlans(event(30), settings)
        val second = ReminderRules.eventPlans(event(30), settings)
        assertEquals(setOf("DANGER:a:$deadline", "DEADLINE:a:$deadline"), first.map { it.key }.toSet())
        assertEquals(first, second)
        assertEquals(first.size, first.map { it.key }.distinct().size)
    }

    /** 岳羽泉：改截止时间必须换新发送键，避免与旧账本碰撞后误去重。 */
    @Test fun deadlineChangeProducesNewKeysSoOldLedgerCannotCollide() {
        val original = ReminderRules.eventPlans(event(60), AppSettings()).map { it.key }.toSet()
        val shifted = ReminderRules.eventPlans(
            event(60).copy(deadlineAt = deadline + 60_000L), AppSettings()).map { it.key }.toSet()
        assertTrue(original.isNotEmpty())
        assertTrue(original.intersect(shifted).isEmpty())
    }

    /** 岳羽泉：不同事项即使同刻触发也不得共用 notificationTag。 */
    @Test fun differentEventsAtSameInstantKeepDistinctNotificationTags() {
        val tagsA = ReminderRules.eventPlans(event(60).copy(id = "a"), AppSettings()).map { it.notificationTag }
        val tagsB = ReminderRules.eventPlans(event(60).copy(id = "b"), AppSettings()).map { it.notificationTag }
        assertEquals(1, tagsA.distinct().size)
        assertEquals(1, tagsB.distinct().size)
        assertTrue(tagsA.intersect(tagsB.toSet()).isEmpty())
    }

    /** 岳羽泉：nextWake 跳过 SENT，取最早未发送；CLAIMED／FAILED 受重试间隔约束。 */
    @Test fun nextWakeSkipsSentUsesEarliestPendingAndBackoffForClaimed() {
        val now = 1_000_000L
        fun plan(key: String, trigger: Long, state: String, attempt: Long? = null) =
            ReminderPlanEntity(key, "DEADLINE", "a", deadline, 1, trigger, null, "tag-$key",
                active = true, state = state, lastAttemptAt = attempt)
        assertEquals(now + 50, ReminderRules.nextWake(listOf(
            plan("sent", now - 10, "SENT"),
            plan("soon", now + 50, "PENDING"),
            plan("later", now + 100, "PENDING"),
        ), now))
        assertEquals(now + RETRY_MILLIS, ReminderRules.nextWake(listOf(
            plan("claimed", now - 5, "CLAIMED", now),
        ), now))
        assertNull(ReminderRules.nextWake(listOf(plan("only-sent", now - 1, "SENT")), now))
    }
}
