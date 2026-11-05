package cn.edu.hust.ddlsaver.ui

import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.Importance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CalendarTimelineLayoutTest {
    @Test fun tappedSlotsUseOneHourAndCarryAcrossMidnight() {
        val date = LocalDate.of(2030, 3, 11)
        val daytime = calendarSlotDeadline(date, 9 * 60 + 15)
        assertEquals(date.atTime(10, 15).atZone(BeijingZone).toInstant().toEpochMilli(), daytime)
        val nextDay = calendarSlotDeadline(date, 23 * 60 + 45)
        assertEquals(date.plusDays(1).atTime(0, 45).atZone(BeijingZone).toInstant().toEpochMilli(), nextDay)
        val blank = event("blank", "2030-03-11T10:15", null)
        assertEquals(date.atTime(9, 15).atZone(BeijingZone).toInstant(), calendarStart(blank))
        assertEquals("默认 60 分钟展示", calendarDurationLabel(blank))
    }
    private fun event(id: String, deadline: String, minutes: Long?): DdlEvent = DdlEvent(
        id = id, title = id, deadlineAt = LocalDateTime.parse(deadline).atZone(BeijingZone).toInstant().toEpochMilli(),
        sprintMinutes = minutes, description = "", importance = Importance.NORMAL, completedAt = null,
        archivedAt = null, createdAt = 0, updatedAt = 0, revision = 1,
    )

    @Test fun sprintAndDefaultDurationProduceAccurateBlocks() {
        val date = LocalDate.of(2030, 3, 11)
        val explicit = event("explicit", "2030-03-11T12:00", 120)
        val default = event("default", "2030-03-11T15:30", null)
        val rows = timelinePlacements(date, listOf(default, explicit)).associateBy { it.event.id }
        assertEquals(600, rows.getValue("explicit").startMinute)
        assertEquals(720, rows.getValue("explicit").endMinute)
        assertEquals(870, rows.getValue("default").startMinute)
        assertEquals(930, rows.getValue("default").endMinute)
        assertEquals(null, default.sprintMinutes)
    }

    @Test fun overlapsUseSeparateLanesAndAdjacentIntervalsCanReuseSpace() {
        val date = LocalDate.of(2030, 3, 11)
        val first = event("first", "2030-03-11T12:00", 120)
        val overlap = event("overlap", "2030-03-11T11:30", 60)
        val next = event("next", "2030-03-11T13:00", 60)
        val rows = timelinePlacements(date, listOf(first, overlap, next)).associateBy { it.event.id }
        assertEquals(2, rows.getValue("first").laneCount)
        assertEquals(2, rows.getValue("overlap").laneCount)
        assertTrue(rows.getValue("first").lane != rows.getValue("overlap").lane)
        assertEquals(1, rows.getValue("next").laneCount)
    }

    @Test fun midnightAndMultiDayIntervalsAreClippedToEachDay() {
        val spanning = event("long", "2030-03-12T01:00", 180)
        val previous = LocalDate.of(2030, 3, 11)
        val deadlineDay = previous.plusDays(1)
        val first = timelinePlacements(previous, listOf(spanning)).single()
        val second = timelinePlacements(deadlineDay, listOf(spanning)).single()
        assertEquals(1320, first.startMinute)
        assertEquals(1440, first.endMinute)
        assertEquals(0, second.startMinute)
        assertEquals(60, second.endMinute)
        assertFalse(calendarEventsOn(previous.minusDays(1), listOf(spanning)).contains(spanning))
        val midnight = event("midnight", "2030-03-12T00:00", null)
        assertTrue(calendarEventsOn(deadlineDay, listOf(midnight)).contains(midnight))
        assertEquals(0, timelinePlacements(deadlineDay, listOf(midnight)).single().startMinute)
    }
}
