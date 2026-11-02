package cn.edu.hust.ddlsaver.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class RecurrenceRulesTest {
    private fun deadline(year: Int, month: Int, day: Int, hour: Int = 18) =
        ZonedDateTime.of(year, month, day, hour, 0, 0, 0, UrgencyRules.BEIJING_ZONE).toInstant()

    @Test fun missingPeriodsNeverRecreateKnownOrSkippedIndexes() {
        val rule = RecurrenceRule(deadline(2026, 11, 2), RecurrenceFrequency.DAILY, occurrenceCount = 4)
        val firstPass = RecurrenceRules.missingBetween(rule, deadline(2026, 11, 2), deadline(2026, 11, 5), setOf(0L, 2L))
        assertEquals(listOf(1L, 3L), firstPass.map { it.index })
        assertTrue(RecurrenceRules.missingBetween(rule, deadline(2026, 11, 2), deadline(2026, 11, 5),
            setOf(0L, 2L) + firstPass.map { it.index }).isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            RecurrenceRules.missingBetween(rule, deadline(2026, 11, 2), deadline(2026, 11, 5), setOf(-1L))
        }
    }

    @Test fun monthlyUsesOriginalDayAcrossShortAndLeapMonths() {
        val rule = RecurrenceRule(deadline(2028, 1, 31), RecurrenceFrequency.MONTHLY)
        assertEquals(deadline(2028, 2, 29), RecurrenceRules.occurrenceAt(rule, 1)?.deadline)
        assertEquals(deadline(2028, 3, 31), RecurrenceRules.occurrenceAt(rule, 2)?.deadline)
        assertEquals(deadline(2028, 4, 30), RecurrenceRules.occurrenceAt(rule, 3)?.deadline)
        val ordinary = rule.copy(firstDeadline = deadline(2027, 1, 31))
        assertEquals(deadline(2027, 2, 28), RecurrenceRules.occurrenceAt(ordinary, 1)?.deadline)
    }

    @Test fun endDateIsInclusiveAndCountIncludesFirst() {
        val start = deadline(2026, 12, 30)
        val byDate = RecurrenceRule(start, RecurrenceFrequency.DAILY, untilDate = LocalDate.of(2027, 1, 1))
        assertEquals(listOf(start, deadline(2026, 12, 31), deadline(2027, 1, 1)),
            RecurrenceRules.between(byDate, start, deadline(2027, 1, 10)).map { it.deadline })
        val byCount = RecurrenceRule(start, RecurrenceFrequency.WEEKLY, occurrenceCount = 2)
        assertNotNull(RecurrenceRules.occurrenceAt(byCount, 1))
        assertNull(RecurrenceRules.occurrenceAt(byCount, 2))
    }

    @Test fun rangeIsInclusiveAndDoesNotInventAnEarlierOccurrence() {
        val start = deadline(2026, 11, 2)
        val rule = RecurrenceRule(start, RecurrenceFrequency.WEEKLY)
        assertTrue(RecurrenceRules.between(rule, deadline(2026, 10, 1), deadline(2026, 11, 1)).isEmpty())
        val occurrence = deadline(2026, 11, 9)
        assertEquals(listOf(1L), RecurrenceRules.between(rule, occurrence, occurrence).map { it.index })
        assertTrue(RecurrenceRules.between(rule, occurrence.plusSeconds(1), deadline(2026, 11, 15)).isEmpty())
    }

    @Test fun oldSeriesCanBeQueriedDirectlyAndResultLimitIsEnforced() {
        val rule = RecurrenceRule(deadline(2000, 1, 1), RecurrenceFrequency.DAILY)
        val result = RecurrenceRules.between(rule, deadline(2026, 1, 1), deadline(2026, 1, 2), 2)
        assertEquals(2, result.size)
        assertEquals(deadline(2026, 1, 1), result.first().deadline)
        assertThrows(IllegalArgumentException::class.java) {
            RecurrenceRules.between(rule, deadline(2026, 1, 1), deadline(2026, 1, 3), 2)
        }
    }

    @Test fun rejectsInvalidRulesAndRanges() {
        val first = deadline(2026, 11, 2)
        assertThrows(IllegalArgumentException::class.java) { RecurrenceRule(first, RecurrenceFrequency.DAILY, occurrenceCount = 0) }
        assertThrows(IllegalArgumentException::class.java) { RecurrenceRule(first, RecurrenceFrequency.DAILY, untilDate = LocalDate.of(2026, 11, 1)) }
        assertThrows(IllegalArgumentException::class.java) { RecurrenceRule(first, RecurrenceFrequency.DAILY, LocalDate.of(2026, 11, 3), 3) }
        assertThrows(IllegalArgumentException::class.java) { RecurrenceRules.between(RecurrenceRule(first, RecurrenceFrequency.DAILY), first, first.minusSeconds(1)) }
    }
}
