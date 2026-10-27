package cn.edu.hust.ddlsaver.ui

import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class CalendarGridTest {
    @Test fun leapFebruaryContainsEveryDateOnceWithMondayFirstAlignment() {
        val month = YearMonth.of(2028, 2)
        val weeks = calendarWeeks(month)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals((1..29).map { month.atDay(it) }, weeks.flatten().filterNotNull())
        assertNull(weeks.first().first())
        assertEquals(month.atDay(1), weeks.first()[1])
    }

    @Test fun sundayStartAndMonthBoundaryHaveCompleteRows() {
        val month = YearMonth.of(2026, 11)
        val weeks = calendarWeeks(month)
        assertEquals(6, weeks.size)
        assertEquals(month.atDay(1), weeks.first()[6])
        assertEquals(month.atDay(30), weeks.last()[0])
        assertTrue(weeks.last().drop(1).all { it == null })
    }
}
