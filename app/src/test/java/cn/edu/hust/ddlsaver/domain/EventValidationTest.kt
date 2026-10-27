package cn.edu.hust.ddlsaver.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class EventValidationTest {
    private fun draft() = EventDraft(
        title = "  课程报告  ",
        deadlineAt = Instant.parse("2030-01-02T10:12:34.567Z").toEpochMilli(),
    )

    @Test
    fun normalizesTitleAndMinutePrecisionWithoutChangingDescription() {
        val result = EventValidation.normalize(draft().copy(description = "  保留格式\n第二行"))
        assertEquals("课程报告", result.title)
        assertEquals(Instant.parse("2030-01-02T10:12:00Z").toEpochMilli(), result.deadlineAt)
        assertEquals("  保留格式\n第二行", result.description)
        assertNull(result.sprintMinutes)
    }

    @Test
    fun blankTitleIsRejected() {
        listOf("", "   ", "\n\t", "\u3000").forEach { title ->
            assertThrows(EventValidationException::class.java) {
                EventValidation.normalize(draft().copy(title = title))
            }
        }
    }

    @Test
    fun sprintAndThresholdRejectNonPositiveOrOverflowingMinutes() {
        listOf(0L, -1L, Long.MAX_VALUE).forEach { minutes ->
            assertThrows(EventValidationException::class.java) {
                EventValidation.normalize(draft().copy(sprintMinutes = minutes))
            }
            assertThrows(EventValidationException::class.java) {
                EventValidation.validateMinutes(minutes)
            }
        }
    }

    @Test
    fun positiveDurationAndAbsentSprintAreAccepted() {
        assertEquals(1L, EventValidation.normalize(draft().copy(sprintMinutes = 1)).sprintMinutes)
        assertNull(EventValidation.normalize(draft()).sprintMinutes)
        EventValidation.validateMinutes(EventValidation.MAX_DURATION_MINUTES)
    }

    @Test
    fun editRequiresAnOriginalRevisionAndNewEventCannotImpersonateEdit() {
        assertThrows(EventValidationException::class.java) {
            EventValidation.normalize(draft().copy(id = "existing"))
        }
        assertThrows(EventValidationException::class.java) {
            EventValidation.normalize(draft().copy(expectedRevision = 1))
        }
        assertEquals(2L, EventValidation.normalize(draft().copy(id = "existing", expectedRevision = 2)).expectedRevision)
    }

    @Test
    fun negativeEpochMinuteNormalizationFloorsInsteadOfTruncatingTowardZero() {
        assertEquals(-60_000L, EventValidation.normalize(draft().copy(deadlineAt = -1)).deadlineAt)
    }

    @Test
    fun deadlineNormalizationRejectsOverflowInsteadOfWrapping() {
        assertThrows(EventValidationException::class.java) {
            EventValidation.normalize(draft().copy(deadlineAt = Long.MIN_VALUE))
        }
    }
}
