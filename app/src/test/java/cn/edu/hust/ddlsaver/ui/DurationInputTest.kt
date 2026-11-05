package cn.edu.hust.ddlsaver.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DurationInputTest {
    private fun valid(text: String, unit: DurationUnit): Long =
        (parseDuration(text, unit) as DurationParse.Valid).minutes

    @Test fun convertsEachUnitToWholeMinutes() {
        assertEquals(45L, valid("45", DurationUnit.MINUTE))
        assertEquals(120L, valid("2", DurationUnit.HOUR))
        assertEquals(2880L, valid("2", DurationUnit.DAY))
        assertEquals(90L, valid(" 1.5 ", DurationUnit.HOUR))
        assertEquals(720L, valid("0.5", DurationUnit.DAY))
        assertEquals(60L, valid("1.", DurationUnit.HOUR))
    }

    @Test fun roundsFractionsToNearestMinute() {
        // 0.01 hour = 0.6 minute -> 1 minute; 1.25 minute -> 1 minute; 2.5 minutes -> 3 minutes.
        assertEquals(1L, valid("0.01", DurationUnit.HOUR))
        assertEquals(1L, valid("1.25", DurationUnit.MINUTE))
        assertEquals(3L, valid("2.5", DurationUnit.MINUTE))
    }

    @Test fun blankIsOptionalButZeroAndGarbageAreRejected() {
        assertEquals(DurationParse.Blank, parseDuration("  ", DurationUnit.HOUR))
        assertEquals(DurationParse.Invalid("时长至少 1 分钟。"), parseDuration("0", DurationUnit.DAY))
        assertEquals(DurationParse.Invalid("时长至少 1 分钟。"), parseDuration("0.4", DurationUnit.MINUTE))
        listOf("-5", "abc", "1.234", "1,5", "1e3", ".5", "1 小时").forEach { text ->
            assertEquals(text, DurationParse.Invalid("请填写数字，最多保留两位小数。"), parseDuration(text, DurationUnit.HOUR))
        }
    }

    @Test fun capsAtOneYearWithoutOverflow() {
        assertEquals(MaxInputDurationMinutes, valid("365", DurationUnit.DAY))
        assertEquals(MaxInputDurationMinutes, valid("8760", DurationUnit.HOUR))
        val tooLong = parseDuration("366", DurationUnit.DAY)
        assertTrue(tooLong is DurationParse.Invalid)
        assertEquals("时长不能超过 365 天。", (tooLong as DurationParse.Invalid).message)
        // Nine digits of days would overflow a naive Long multiplication of milliseconds; here it is just rejected.
        assertTrue(parseDuration("999999999.99", DurationUnit.DAY) is DurationParse.Invalid)
    }

    @Test fun storedValuesReopenInLargestExactUnit() {
        assertEquals(DurationText("", DurationUnit.HOUR), splitDuration(null))
        assertEquals(DurationText("", DurationUnit.MINUTE), splitDuration(null, DurationUnit.MINUTE))
        assertEquals(DurationText("90", DurationUnit.MINUTE), splitDuration(90))
        assertEquals(DurationText("2", DurationUnit.HOUR), splitDuration(120))
        assertEquals(DurationText("36", DurationUnit.HOUR), splitDuration(2160))
        assertEquals(DurationText("2", DurationUnit.DAY), splitDuration(2880))
        // Round trip: what the field shows parses back to the same minutes.
        listOf(1L, 59L, 60L, 90L, 1440L, 2160L, 10_080L).forEach { minutes ->
            val shown = splitDuration(minutes)
            assertEquals(minutes, valid(shown.amount, shown.unit))
        }
    }

    @Test fun formatDurationIncludesDays() {
        assertEquals("45 分钟", formatDuration(45))
        assertEquals("1 小时", formatDuration(60))
        assertEquals("1 小时 30 分钟", formatDuration(90))
        assertEquals("1 天", formatDuration(1440))
        assertEquals("1 天 12 小时", formatDuration(2160))
        assertEquals("2 天 1 分钟", formatDuration(2881))
        assertEquals("365 天", formatDuration(MaxInputDurationMinutes))
    }

    @Test fun unknownStoredUnitFallsBackToHours() {
        assertEquals(DurationUnit.DAY, DurationUnit.fromStored("DAY"))
        assertEquals(DurationUnit.HOUR, DurationUnit.fromStored(null))
        assertEquals(DurationUnit.HOUR, DurationUnit.fromStored("WEEK"))
    }
}
