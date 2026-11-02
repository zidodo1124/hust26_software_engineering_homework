package cn.edu.hust.ddlsaver.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** A rule describes a series; occurrence indexes are zero-based and include the first deadline. */
enum class RecurrenceFrequency { DAILY, WEEKLY, MONTHLY }

data class RecurrenceRule(
    val firstDeadline: Instant,
    val frequency: RecurrenceFrequency,
    val untilDate: LocalDate? = null,
    val occurrenceCount: Int? = null,
    val monthlyAnchorDay: Int? = null,
) {
    init {
        require(occurrenceCount == null || occurrenceCount > 0)
        require(untilDate == null || untilDate >= firstDeadline.atZone(UrgencyRules.BEIJING_ZONE).toLocalDate())
        require(untilDate == null || occurrenceCount == null)
        require(monthlyAnchorDay == null || (frequency == RecurrenceFrequency.MONTHLY && monthlyAnchorDay in 1..31))
    }
}

data class RecurrenceOccurrence(val index: Long, val deadline: Instant)

object RecurrenceRules {
    /** Existing indexes include skipped occurrences; never recreate those when extending a window. */
    fun missingBetween(rule: RecurrenceRule, from: Instant, through: Instant, knownIndexes: Set<Long>,
                       maxResults: Int = 10_000): List<RecurrenceOccurrence> {
        require(knownIndexes.all { it >= 0 })
        return between(rule, from, through, maxResults).filterNot { it.index in knownIndexes }
    }

    /** Compute from the original date each time, so a short month never shifts later months. */
    fun occurrenceAt(rule: RecurrenceRule, index: Long): RecurrenceOccurrence? {
        require(index >= 0)
        if (rule.occurrenceCount != null && index >= rule.occurrenceCount.toLong()) return null
        val first = rule.firstDeadline.atZone(UrgencyRules.BEIJING_ZONE)
        val date = when (rule.frequency) {
            RecurrenceFrequency.DAILY -> first.toLocalDate().plusDays(index)
            RecurrenceFrequency.WEEKLY -> first.toLocalDate().plusWeeks(index)
            RecurrenceFrequency.MONTHLY -> {
                val month = YearMonth.from(first).plusMonths(index)
                month.atDay(minOf(rule.monthlyAnchorDay ?: first.dayOfMonth, month.lengthOfMonth()))
            }
        }
        if (rule.untilDate != null && date > rule.untilDate) return null
        return RecurrenceOccurrence(index, date.atTime(first.toLocalTime()).atZone(UrgencyRules.BEIJING_ZONE).toInstant())
    }

    /** Return occurrences in the inclusive range, without walking from an old series' beginning. */
    fun between(rule: RecurrenceRule, from: Instant, through: Instant, maxResults: Int = 10_000): List<RecurrenceOccurrence> {
        require(!through.isBefore(from))
        require(maxResults > 0)
        val firstDate = rule.firstDeadline.atZone(UrgencyRules.BEIJING_ZONE).toLocalDate()
        val fromDate = from.atZone(UrgencyRules.BEIJING_ZONE).toLocalDate()
        val offset = when (rule.frequency) {
            RecurrenceFrequency.DAILY -> ChronoUnit.DAYS.between(firstDate, fromDate)
            RecurrenceFrequency.WEEKLY -> ChronoUnit.DAYS.between(firstDate, fromDate) / 7
            RecurrenceFrequency.MONTHLY -> ChronoUnit.MONTHS.between(YearMonth.from(firstDate), YearMonth.from(fromDate))
        }.coerceAtLeast(0)
        val result = mutableListOf<RecurrenceOccurrence>()
        var index = offset
        while (true) {
            val occurrence = occurrenceAt(rule, index) ?: break
            if (occurrence.deadline > through) break
            if (occurrence.deadline >= from) {
                require(result.size < maxResults) { "查询周期过大，请缩小日期范围" }
                result.add(occurrence)
            }
            index++
        }
        return result
    }
}
