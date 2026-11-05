package cn.edu.hust.ddlsaver.ui

import cn.edu.hust.ddlsaver.domain.DdlEvent
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class TimelinePlacement(
    val event: DdlEvent,
    val startMinute: Int,
    val endMinute: Int,
    val lane: Int,
    val laneCount: Int,
)

/** Display intervals are derived, never written back to the event or its reminder settings. */
internal fun calendarStart(event: DdlEvent): Instant =
    Instant.ofEpochMilli(event.deadlineAt).minus(event.sprintMinutes ?: 60L, ChronoUnit.MINUTES)

/** A tapped slot is the display start; the blank sprint field keeps the default one-hour interval. */
internal fun calendarSlotDeadline(date: LocalDate, minute: Int): Long =
    date.atStartOfDay(BeijingZone).plusMinutes(minute.coerceIn(0, 1439).toLong())
        .plusHours(1).toInstant().toEpochMilli()

internal fun calendarDurationLabel(event: DdlEvent): String =
    event.sprintMinutes?.let { "速通 ${formatDuration(it)}" } ?: "默认 60 分钟展示"

internal fun calendarEventsOn(date: LocalDate, events: List<DdlEvent>): List<DdlEvent> {
    val from = date.atStartOfDay(BeijingZone).toInstant()
    val through = date.plusDays(1).atStartOfDay(BeijingZone).toInstant()
    return events.filter { event ->
        val end = Instant.ofEpochMilli(event.deadlineAt)
        val start = calendarStart(event)
        (start < through && end > from) || end == from
    }.sortedWith(compareBy<DdlEvent> { it.deadlineAt }.thenBy { it.id })
}

/** Clip intervals to the local day and assign overlapping blocks to separate columns. */
internal fun timelinePlacements(date: LocalDate, events: List<DdlEvent>): List<TimelinePlacement> {
    val from = date.atStartOfDay(BeijingZone).toInstant()
    val through = date.plusDays(1).atStartOfDay(BeijingZone).toInstant()
    data class Block(val event: DdlEvent, val start: Int, val end: Int)
    val blocks = calendarEventsOn(date, events).map { event ->
        val start = maxOf(calendarStart(event), from)
        val end = minOf(Instant.ofEpochMilli(event.deadlineAt), through)
        val startMinute = Duration.between(from, start).toMinutes().toInt().coerceIn(0, 1439)
        val endMinute = Duration.between(from, end).toMinutes().toInt().coerceIn(startMinute, 1440)
        Block(event, startMinute, maxOf(endMinute, startMinute + 24))
    }.sortedWith(compareBy<Block> { it.start }.thenBy { it.end }.thenBy { it.event.id })
    val result = mutableListOf<TimelinePlacement>()
    var first = 0
    while (first < blocks.size) {
        var end = first + 1
        var groupEnd = blocks[first].end
        while (end < blocks.size && blocks[end].start < groupEnd) {
            groupEnd = maxOf(groupEnd, blocks[end].end)
            end++
        }
        val laneEnds = mutableListOf<Int>()
        val assigned = blocks.subList(first, end).map { block ->
            val available = laneEnds.indexOfFirst { it <= block.start }
            val lane = if (available < 0) laneEnds.size.also { laneEnds.add(block.end) }
                else available.also { laneEnds[it] = block.end }
            block to lane
        }
        result += assigned.map { (block, lane) -> TimelinePlacement(block.event, block.start, block.end, lane, laneEnds.size) }
        first = end
    }
    return result
}
